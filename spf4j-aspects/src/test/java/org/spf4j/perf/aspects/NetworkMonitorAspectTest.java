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
package org.spf4j.perf.aspects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.base.Throwables;
import org.spf4j.concurrent.DefaultExecutor;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings({"MDM_THREAD_YIELD", "DM_DEFAULT_ENCODING", "UNENCRYPTED_SERVER_SOCKET", "UNENCRYPTED_SOCKET"})
public final class NetworkMonitorAspectTest {

  private static final Logger LOG = LoggerFactory.getLogger(NetworkMonitorAspectTest.class);

  private volatile boolean terminated = false;

  private final CountDownLatch latch = new CountDownLatch(1);

  public void runServer() throws IOException {
    try (ServerSocket server = new ServerSocket(4321, 0, InetAddress.getLoopbackAddress())) {
      latch.countDown();
      while (!terminated) {
        Socket client = server.accept();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                client.getInputStream(), Charset.defaultCharset()));
                PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {
          try {
            while (true) {
              String line = in.readLine();
              if (line == null) {
                break;
              }
              out.println(line);
            }
          } catch (IOException e) {
            Throwables.writeTo(e, System.err, Throwables.PackageDetail.SHORT);
          }
        }
      }
    } finally {
      latch.countDown();
    }

  }

  /**
   * Test of nioReadLong method, of class NetworkMonitorAspect.
   */
  @Test
  public void testNetworkUsageRecording()
          throws InterruptedException, IOException, ExecutionException, TimeoutException {
    System.setProperty("spf4j.perf.network.sampleTimeMillis", "1000");
    Future<String> serverFuture = DefaultExecutor.INSTANCE.submit(new AbstractRunnable() {

      @Override
      public void doRun() throws IOException {
        runServer();
      }
    }, "server");
    if (!latch.await(10, TimeUnit.SECONDS)) {
      serverFuture.get(1, TimeUnit.SECONDS);
      throw new IOException("Unable to start server in " + 10 + "seconds" + serverFuture);
    }
    for (int i = 0; i < 100; i++) {
      clientTest();
      Thread.sleep(100);
    }
    terminated = true;
    clientTest();
    serverFuture.get();
  }

  private void clientTest() throws IOException {
    try (Socket socket = new Socket("localhost", 4321);
            PrintWriter out = new PrintWriter(socket.getOutputStream(),
                    true); BufferedReader in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), Charset.defaultCharset()))) {
      out.println("hello world");
      LOG.debug("Received: {}", in.readLine());
    }
  }
}
