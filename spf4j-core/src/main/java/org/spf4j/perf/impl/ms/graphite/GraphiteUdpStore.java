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
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nullable;
import org.spf4j.base.HandlerNano;
import org.spf4j.base.Strings;
import org.spf4j.base.UncheckedExecutionException;
import org.spf4j.base.UncheckedTimeoutException;
import org.spf4j.failsafe.RetryPolicy;
import org.spf4j.io.ByteArrayBuilder;
import org.spf4j.perf.MeasurementsInfo;
import org.spf4j.perf.MeasurementStore;
import org.spf4j.perf.impl.ms.Id2Info;
import org.spf4j.recyclable.ObjectCreationException;
import org.spf4j.recyclable.ObjectDisposeException;
import org.spf4j.recyclable.RecyclingSupplier;
import org.spf4j.recyclable.Template;
import org.spf4j.recyclable.impl.RecyclingSupplierBuilder;

/**
 *
 * @author zoly
 */
public final class GraphiteUdpStore implements MeasurementStore {

  public static final int MAX_UDP_MSG_SIZE = 512;

  private final RecyclingSupplier<DatagramChannel> datagramChannelSupplier;

  private final InetSocketAddress address;

  private static class DatagramChannelSupplierFactory implements RecyclingSupplier.Factory<DatagramChannel> {

    private final InetSocketAddress address;

    DatagramChannelSupplierFactory(final InetSocketAddress address) {
      this.address = address;
    }

    @Override
    public DatagramChannel create() throws ObjectCreationException {
      DatagramChannel datagramChannel;
      try {
        datagramChannel = DatagramChannel.open();
      } catch (IOException ex) {
        throw new ObjectCreationException(ex);
      }
      try {
        datagramChannel.connect(address);
        return datagramChannel;
      } catch (IOException ex) {
        try {
          datagramChannel.close();
        } catch (IOException ex1) {
          ex1.addSuppressed(ex);
          throw new ObjectCreationException(ex1);
        }
        throw new ObjectCreationException(ex);
      }
    }

    @Override
    public void dispose(final DatagramChannel object) throws ObjectDisposeException {
      try {
        object.close();
      } catch (IOException ex) {
        throw new ObjectDisposeException(ex);
      }
    }

    @Override
    public boolean validate(final DatagramChannel object, final Exception e) throws Exception {
      return e == null || !(Throwables.getRootCause(e) instanceof IOException);
    }

  }

  public GraphiteUdpStore(final String hostPort) throws ObjectCreationException, URISyntaxException {
    this(new URI("graphiteUdp://" + hostPort));
  }

  public GraphiteUdpStore(final URI uri) throws ObjectCreationException {
    this(uri.getHost(), uri.getPort());
  }

  public GraphiteUdpStore(final String hostName, final int port) throws ObjectCreationException {
    address = new InetSocketAddress(hostName, port);
    datagramChannelSupplier = new RecyclingSupplierBuilder<>(1,
            new DatagramChannelSupplierFactory(address)).build();
  }

  @Override
  public void flush() {
    // No buffering yet
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
              datagramChannelSupplier, RetryPolicy.defaultPolicy(), IOException.class);
    } catch (TimeoutException ex) {
      throw new UncheckedTimeoutException(ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }

  }

  /**
   * Write with the plaintext protocol: https://graphite.readthedocs.io/en/0.9.10/feeding-carbon.html
   *
   * @param measurementInfo measuremrnt info
   * @param measurementName measurement name
   * @param measurement measurement value
   * @param timeStampMillis timestamp millis since epoch.
   * @param os the output writer to write to.
   * @throws IOException
   */
  public static void writeMetric(final MeasurementsInfo measurementInfo, final String measurementName,
          final long measurement, final long timeStampMillis, final Writer os)
          throws IOException {
    Strings.writeReplaceWhitespaces(measurementInfo.getMeasuredEntity().toString(), '-', os);
    os.append('/');
    Strings.writeReplaceWhitespaces(measurementName, '-', os);
    os.append(' ');
    os.append(Long.toString(measurement));
    os.append(' ');
    os.append(Long.toString(timeStampMillis));
    os.append('\n');
  }

  @Override
  public String toString() {
    return "GraphiteUdpStore{address=" + address + '}';
  }

  @Override
  public void close() {
    try {
      datagramChannelSupplier.dispose();
    } catch (ObjectDisposeException | InterruptedException ex) {
      throw new UncheckedExecutionException(ex);
    }
  }

  private static class HandlerImpl implements HandlerNano<DatagramChannel, Void, IOException> {

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
    public Void handle(final DatagramChannel datagramChannel, final long deadline) throws IOException {
      try (ByteArrayBuilder bos = new ByteArrayBuilder();
              OutputStreamWriter os = new OutputStreamWriter(bos, StandardCharsets.UTF_8)) {

        int msgStart = 0;
        int msgEnd = 0;
        int prevEnd = 0;

        for (int i = 0; i < measurements.length; i++) {
          writeMetric(measurementInfo, measurementInfo.getMeasurementName(i),
                  measurements[i], timeStampMillis, os);
          os.flush();
          msgEnd = bos.size();
          int length = msgEnd - msgStart;
          if (length > MAX_UDP_MSG_SIZE) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(bos.getBuffer(), msgStart, prevEnd - msgStart);
            datagramChannel.write(byteBuffer);
            msgStart = prevEnd;
          }
          prevEnd = msgEnd;
        }
        if (msgEnd > msgStart) {
          ByteBuffer byteBuffer = ByteBuffer.wrap(bos.getBuffer(), msgStart, msgEnd - msgStart);
          datagramChannel.write(byteBuffer);
        }
      }
      return null;
    }
  }

}
