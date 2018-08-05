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
package org.spf4j.perf.impl;

import org.spf4j.perf.impl.ms.graphite.GraphiteUdpStore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.perf.CloseableMeasurementRecorder;
import org.spf4j.recyclable.ObjectCreationException;

/**
 * @author zoly
 */
public final class GraphiteUdpStoreTest {

  private static final Logger LOG = LoggerFactory.getLogger(GraphiteUdpStoreTest.class);

  private static volatile boolean terminated = false;
  private static volatile Future<?> server;
  private static final BlockingQueue<String> QUEUE = new LinkedBlockingQueue<>();

  private static final File TSDB_TXT;

  static {
    File tsdb;
    try {
      tsdb = File.createTempFile("ttt", "tsdb");
      TSDB_TXT = File.createTempFile("ttt", "tsdbtxt");
    } catch (IOException ex) {
      throw new ExceptionInInitializerError(ex);
    }
    System.setProperty("spf4j.perf.ms.config",
            "TSDB@" + tsdb.getAbsolutePath() + "," + "TSDB_TXT@" + TSDB_TXT.getAbsolutePath()
            + ",GRAPHITE_UDP@127.0.0.1:1976");
  }


  @Test
  public void testGraphiteUdpStore() throws IOException, ObjectCreationException, InterruptedException {

    final GraphiteUdpStore store = new GraphiteUdpStore("127.0.0.1", 1976);
    long id = store.alocateMeasurements(new MeasurementsInfoImpl("bla", "ms",
            new String[]{"val1", "val2", "val3"},
            new String[]{"ms", "ms", "ms"}), 0);
    store.saveMeasurements(id, 1L, 2L, 3L, 5L);
    LOG.debug("measurements sent: {} {} {} {}", 1L, 2L, 3L, 5L);
    String line = QUEUE.poll(5, TimeUnit.SECONDS);
    LOG.debug("measurements received: {} ", line);
    Assert.assertEquals("bla/val1 2 1", line);
    line = QUEUE.poll(5, TimeUnit.SECONDS);
    LOG.debug("measurements received: {} ", line);
    Assert.assertEquals("bla/val2 3 1", line);
    line = QUEUE.poll(5, TimeUnit.SECONDS);
    LOG.debug("measurements received: {} ", line);
    Assert.assertEquals("bla/val3 5 1", line);
  }

  @Before
  public void beforeTest() {
    QUEUE.drainTo(new ArrayList<String>());
  }


  @Test
  @SuppressFBWarnings("MDM_THREAD_YIELD")
  public void testStore() throws InterruptedException, IOException {

    CloseableMeasurementRecorder recorder = RecorderFactory.createScalableQuantizedRecorder2("test measurement",
            "ms", 1000, 10, 0, 6, 10);

    for (int i = 0; i < 100; i++) {
      recorder.record(i);
      Thread.sleep(100);
    }
    recorder.close();
    RecorderFactory.MEASUREMENT_STORE.flush();
//    RecorderFactory.MEASUREMENT_STORE.close();
    List<String> lines = Files.readAllLines(TSDB_TXT.toPath(), StandardCharsets.UTF_8);
    LOG.debug("measurements = {}", lines);
    Assert.assertThat(lines, Matchers.hasItem(Matchers.allOf(
            Matchers.containsString("Q6_7"), Matchers.containsString("test measurement"))));
    String line = QUEUE.poll(5, TimeUnit.SECONDS);
    Assert.assertThat(line, Matchers.containsString("test-measurement"));

  }

  @BeforeClass
  public static void runUdpServer() {

    server = org.spf4j.concurrent.DefaultExecutor.INSTANCE.submit(new AbstractRunnable(true) {

      @Override
      public void doRun() throws IOException, InterruptedException {
        DatagramChannel channel = DatagramChannel.open();
        final InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 1976);
        channel.socket().bind(inetSocketAddress);
        ByteBuffer bb = ByteBuffer.allocate(512);
        while (!terminated) {
          bb.rewind();
          try {
            channel.receive(bb);
          } catch (ClosedByInterruptException ex) {
            // this we receive when we interrupt this exec with server.cancel.
            break;
          }
          byte[] rba = new byte[bb.position()];
          bb.rewind();
          bb.get(rba);
          String receivedString = new String(rba, StandardCharsets.UTF_8);
          String[] lines = receivedString.split("\n");
          LOG.debug("Received = {}", lines);
          for (String line : lines) {
            QUEUE.put(line);
          }
        }
      }
    });

  }

  @AfterClass
  public static void stopUdpServer() {
    terminated = true;
    server.cancel(true);
  }

}
