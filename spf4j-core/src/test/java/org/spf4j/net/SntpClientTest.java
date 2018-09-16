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
package org.spf4j.net;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import javax.annotation.concurrent.NotThreadSafe;
import org.junit.Assert;
//import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.AbstractRunnable;

/**
 *
 * @author zoly
 */
//@Ignore
@NotThreadSafe
public final class SntpClientTest {

  private static final Logger LOG = LoggerFactory.getLogger(SntpClientTest.class);

  @Test
  public void test() throws IOException, InterruptedException, TimeoutException {
    Timing requestTime = SntpClient.requestTimeHA(60000, "time.apple.com");
    long currentTimeMachine = System.currentTimeMillis();
    long currentTimeNtp = requestTime.getTime();
    LOG.debug("Current time machine = {} ", Instant.ofEpochMilli(currentTimeMachine));
    LOG.debug("Current time ntp = {} ", Instant.ofEpochMilli(currentTimeNtp));
    Assert.assertTrue(Math.abs(currentTimeNtp - currentTimeMachine) < 10000);
  }

  @Test
  public void test2() throws IOException {
    try (Closeable runUdpServer = runUdpServer(false)) {
      Timing timing = SntpClient.requestTime("localhost", 50123, 20);
      long currentTimeMachine = System.currentTimeMillis();
      long currentTimeNtp = timing.getTime();
      LOG.debug("Current Timing = {} ", timing);
      LOG.debug("Current time machine = {} ", Instant.ofEpochMilli(currentTimeMachine));
      LOG.debug("Current time ntp = {} ", Instant.ofEpochMilli(currentTimeNtp));
      Assert.assertTrue(Math.abs(currentTimeNtp - currentTimeMachine) < 10000);
    }
  }

  @Test
  public void test2h() throws IOException, InterruptedException, TimeoutException {
    try (Closeable runUdpServer = runUdpServer(true)) {
      Timing timing = SntpClient.requestTimeHA(60000, 5, 50123, "localhost");
      long currentTimeMachine = System.currentTimeMillis();
      long currentTimeNtp = timing.getTime();
      LOG.debug("Current Timing = {} ", timing);
      LOG.debug("Current time machine = {} ", Instant.ofEpochMilli(currentTimeMachine));
      LOG.debug("Current time ntp = {} ", Instant.ofEpochMilli(currentTimeNtp));
      Assert.assertTrue(Math.abs(currentTimeNtp - currentTimeMachine) < 10000);
    }
  }

  @Test
  public void test3() {
    byte[] buffer = new byte[8];
    long currentTimeMillis = 1530813879198L;
    SntpClient.writeTimeStamp(buffer, 0, currentTimeMillis);
    long readTimeStamp = SntpClient.readTimeStamp(buffer, 0);
    Assert.assertTrue(Math.abs(currentTimeMillis - readTimeStamp) < 2);
  }

  public static Closeable runUdpServer(final boolean hickup) {

    return new Closeable() {
      private volatile boolean terminated = false;
      private Future<?> server = org.spf4j.concurrent.DefaultExecutor.INSTANCE.submit(new AbstractRunnable(true) {

        @Override
        @SuppressFBWarnings("MDM_THREAD_YIELD") // not ideal
        public void doRun() throws IOException, InterruptedException {
          boolean first = true;
          try (DatagramSocket socket = new DatagramSocket(50123)) {
            socket.setSoTimeout(1000);
            byte[] buffer = new byte[48];
            DatagramPacket request = new DatagramPacket(buffer, buffer.length);
            while (!terminated) {
              try {
                socket.receive(request);
              } catch (SocketTimeoutException sex) {
                continue;
              }
              long currentTimeMillis = System.currentTimeMillis();
              LOG.debug("Server received {}", buffer);
              if (first && hickup) {
                Thread.sleep(1000);
                first = false;
              }
              DatagramPacket response
                      = new DatagramPacket(buffer, buffer.length, request.getAddress(), request.getPort());
              buffer[0] = 0b00011100;
              buffer[1] = 2;
              SntpClient.writeTimeStamp(buffer, SntpClient.RECEIVE_TIME_OFFSET, currentTimeMillis);
              System.arraycopy(buffer, SntpClient.TRANSMIT_TIME_OFFSET, buffer, SntpClient.ORIGINATE_TIME_OFFSET, 8);
              SntpClient.writeTimeStamp(buffer, SntpClient.TRANSMIT_TIME_OFFSET, System.currentTimeMillis());
              socket.send(response);
              LOG.debug("Server reply to {} with {}", request.getPort(), buffer);
            }
          }
        }
      });

      @Override
      public void close() {
        terminated = true;
        server.cancel(true);
      }
    };

  }

}
