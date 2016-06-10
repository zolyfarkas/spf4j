/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
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
 */
package org.spf4j.perf.impl;

import org.spf4j.perf.impl.ms.graphite.GraphiteUdpStore;
import com.google.common.base.Charsets;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.perf.MeasurementRecorder;
import org.spf4j.recyclable.ObjectCreationException;

/**
 *
 * @author zoly
 */
public final class GraphiteUdpStoreTest {


  private static final File tsdbtxt;
  static {
    File tsdb;
    try {
      tsdb = File.createTempFile("ttt", "tsdb");
      tsdbtxt = File.createTempFile("ttt", "tsdbtxt");
    } catch (IOException ex) {
      throw new ExceptionInInitializerError(ex);
    }
    System.setProperty("spf4j.perf.ms.config",
            "TSDB@" + tsdb.getAbsolutePath() + "," + "TSDB_TXT@" + tsdbtxt.getAbsolutePath()
            + ",GRAPHITE_UDP@127.0.0.1:1976");
  }

  @Test
  public void testGraphiteUdpStore() throws IOException, ObjectCreationException, InterruptedException {

    final GraphiteUdpStore store = new GraphiteUdpStore("127.0.0.1", 1976);
    long id = store.alocateMeasurements(new MeasurementsInfoImpl("bla", "ms",
            new String[]{"val1", "val2", "val3"},
            new String[]{"ms", "ms", "ms"}), 0);
    store.saveMeasurements(id, 1L, 2L, 3L, 5L);
    System.out.println("measurements sent");
    String line = queue.poll(5, TimeUnit.SECONDS);
    System.out.println("measurements received " + line);
    Assert.assertEquals("bla/val1 2 1", line);
    line = queue.poll(5, TimeUnit.SECONDS);
    System.out.println("measurements received " + line);
    Assert.assertEquals("bla/val2 3 1", line);
    line = queue.poll(5, TimeUnit.SECONDS);
    System.out.println("measurements received " + line);
    Assert.assertEquals("bla/val3 5 1", line);
  }

  @Test
  @SuppressFBWarnings("MDM_THREAD_YIELD")
  public void testStore() throws InterruptedException, IOException {

    MeasurementRecorder recorder = RecorderFactory.createScalableQuantizedRecorder("test measurement",
            "ms", 1000, 10, 0, 6, 10);

    for (int i = 0; i < 100; i++) {
      recorder.record(i);
      Thread.sleep(100);
    }
    recorder.close();
    RecorderFactory.MEASUREMENT_STORE.flush();
//    RecorderFactory.MEASUREMENT_STORE.close();
    List<String> lines = Files.readAllLines(tsdbtxt.toPath(), StandardCharsets.UTF_8);
    System.out.println("measurements = " + lines);
    Assert.assertThat(lines, Matchers.hasItem(Matchers.allOf(
            Matchers.containsString("Q6_7"), Matchers.containsString("test measurement"))));
    String line = queue.poll(5, TimeUnit.SECONDS);
    Assert.assertThat(line, Matchers.containsString("test-measurement"));

  }

  private static volatile boolean terminated = false;
  private static volatile Future<?> server;
  private static final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

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
          channel.receive(bb);
          byte[] rba = new byte[bb.position()];
          bb.rewind();
          bb.get(rba);
          String receivedString = new String(rba, Charsets.UTF_8);
          String[] lines = receivedString.split("\n");
          System.out.println("Received = " + Arrays.toString(lines));
          for (String line : lines) {
            queue.put(line);
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
