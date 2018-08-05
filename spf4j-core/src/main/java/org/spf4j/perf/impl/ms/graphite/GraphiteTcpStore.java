/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.perf.impl.ms.graphite;

import com.google.common.base.Throwables;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nullable;
import javax.net.SocketFactory;
import org.spf4j.base.HandlerNano;
import org.spf4j.base.UncheckedExecutionException;
import org.spf4j.base.UncheckedTimeoutException;
import org.spf4j.failsafe.RetryPolicy;
import org.spf4j.perf.MeasurementsInfo;
import org.spf4j.perf.MeasurementStore;
import org.spf4j.perf.impl.ms.Id2Info;
import static org.spf4j.perf.impl.ms.graphite.GraphiteUdpStore.writeMetric;
import org.spf4j.recyclable.ObjectCreationException;
import org.spf4j.recyclable.ObjectDisposeException;
import org.spf4j.recyclable.RecyclingSupplier;
import org.spf4j.recyclable.Template;
import org.spf4j.recyclable.impl.RecyclingSupplierBuilder;

/**
 *
 * @author zoly
 */
public final class GraphiteTcpStore implements MeasurementStore {

  private final RecyclingSupplier<Writer> socketWriterSupplier;

  private final InetSocketAddress address;

  private static class WriterSupplierFactory implements RecyclingSupplier.Factory<Writer> {

    private final String hostName;
    private final int port;
    private final SocketFactory socketFactory;

    WriterSupplierFactory(final SocketFactory socketFactory, final String hostName, final int port) {
      this.hostName = hostName;
      this.port = port;
      this.socketFactory = socketFactory;
    }

    @Override
    public Writer create() throws ObjectCreationException {
      Socket socket;
      try {
        socket = socketFactory.createSocket(hostName, port);
      } catch (IOException ex) {
        throw new ObjectCreationException(ex);
      }
      try {
        return new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
      } catch (IOException ex) {
        try {
          socket.close();
        } catch (IOException ex1) {
          ex1.addSuppressed(ex);
          throw new ObjectCreationException(ex1);
        }
        throw new ObjectCreationException(ex);
      }
    }

    @Override
    public void dispose(final Writer object) throws ObjectDisposeException {
      try {
        object.close();
      } catch (IOException ex) {
        throw new ObjectDisposeException(ex);
      }
    }

    @Override
    public boolean validate(final Writer object, final Exception e) {
      return e == null || !(Throwables.getRootCause(e) instanceof IOException);
    }

  }

  public GraphiteTcpStore(final String hostPort) throws ObjectCreationException, URISyntaxException {
    this(new URI("graphiteTcp://" + hostPort));
  }

  public GraphiteTcpStore(final URI uri) throws ObjectCreationException {
    this(uri.getHost(), uri.getPort());
  }

  public GraphiteTcpStore(final String hostName, final int port) throws ObjectCreationException {
    this(hostName, port, SocketFactory.getDefault());
  }

  public GraphiteTcpStore(final String hostName, final int port, final SocketFactory socketFactory)
          throws ObjectCreationException {
    address = new InetSocketAddress(hostName, port);
    socketWriterSupplier = new RecyclingSupplierBuilder<>(1,
            new WriterSupplierFactory(socketFactory, hostName, port)).build();
  }

  @Override
  public long alocateMeasurements(final MeasurementsInfo measurement, final int sampleTimeMillis) {
    return Id2Info.getId(measurement);
  }

  @Override
  @SuppressFBWarnings("BED_BOGUS_EXCEPTION_DECLARATION") // fb nonsense
  public void saveMeasurements(final long tableId,
          final long timeStampMillis, final long... measurements) throws IOException {
    try {
      Template.doOnSupplied(new HandlerImpl(measurements, Id2Info.getInfo(tableId), timeStampMillis),
              1, TimeUnit.MINUTES,
              socketWriterSupplier, RetryPolicy.defaultPolicy(), IOException.class);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return;
    } catch (TimeoutException ex) {
      throw new UncheckedTimeoutException(ex);
    }
  }

  @Override
  public String toString() {
    return "GraphiteTcpStore{address=" + address + '}';
  }

  @Override
  public void close() {
    try {
      socketWriterSupplier.dispose();
    } catch (ObjectDisposeException | InterruptedException ex) {
      throw new UncheckedExecutionException(ex);
    }
  }

  private static class HandlerImpl implements HandlerNano<Writer, Void, IOException> {

    private final long[] measurements;
    private final MeasurementsInfo measurementInfo;
    private final long timeStampMillis;

    HandlerImpl(final long[] measurements, final MeasurementsInfo measurementInfo,
            final long timeStampMillis) {
      this.measurements = measurements;
      this.measurementInfo = measurementInfo;
      this.timeStampMillis = timeStampMillis;
    }

    @Override
    @Nullable
    public Void handle(final Writer socketWriter, final long deadline) throws IOException {
      for (int i = 0; i < measurements.length; i++) {
        writeMetric(measurementInfo, measurementInfo.getMeasurementName(i),
                measurements[i], timeStampMillis, socketWriter);
      }
      socketWriter.flush();
      return null;
    }
  }

  @Override
  public void flush() {
    // No buffering yet
  }

}
