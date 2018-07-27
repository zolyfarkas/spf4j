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

import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.TimeSource;
import org.spf4j.failsafe.RetryPolicy;

/**
 * Simple NTP client. Inspired by Android Sntp client. For how to use, see SntpClientTest.java.
 * https://tools.ietf.org/html/rfc1361
 *
 * @author zoly
 */
@SuppressFBWarnings("PREDICTABLE_RANDOM")
public final class SntpClient {

  static final int ORIGINATE_TIME_OFFSET = 24;
  static final int RECEIVE_TIME_OFFSET = 32;
  static final int TRANSMIT_TIME_OFFSET = 40;
  private static final int NTP_PACKET_SIZE = 48;

  private static final int NTP_PORT = 123;
  private static final int NTP_MODE_CLIENT = 3;
  private static final int NTP_MODE_SERVER = 4;
  private static final int NTP_MODE_BROADCAST = 5;
  private static final int NTP_VERSION = 3;

  private static final int NTP_LEAP_NOSYNC = 3;
  private static final int NTP_STRATUM_DEATH = 0;
  private static final int NTP_STRATUM_MAX = 15;

  // Number of seconds between Jan 1, 1900 and Jan 1, 1970
  // 70 years plus 17 leap days
  private static final long OFFSET_1900_TO_1970 = ((365L * 70L) + 17L) * 24L * 60L * 60L;

  private static final int DEFAULT_SOCKET_TIMEOUT_MILLIS =
          Integer.getInteger("spf4j.sntpClient.defaultSocketTimeoutMillis", 5000); // 5 seconds


  private SntpClient() {
  }

  public static Timing requestTimeHA(final int timeoutMillis, final String... hosts)
          throws IOException, InterruptedException, TimeoutException {
    return requestTimeHA(timeoutMillis, DEFAULT_SOCKET_TIMEOUT_MILLIS, hosts);
  }

  /**
   * Request NTP time with retries.
   *
   * @param hosts NTP server hosts.
   * @param timeoutMillis Max time to attempt to get NTP time
   * @param ntpResponseTimeoutMillis the time after which if we do not receive a response from the NTP server,
   * we consider the call failed (and will retry until timeoutMillis.
   * @return Ntp timing info.
   * @throws IOException - thrown in case of time server connectivity issues.
   * @throws InterruptedException - thrown if exec interrupted.
   */
  public static Timing requestTimeHA(final int timeoutMillis,
          final int ntpResponseTimeoutMillis, final String... hosts)
          throws IOException, InterruptedException, TimeoutException {
    return requestTimeHA(timeoutMillis, ntpResponseTimeoutMillis, NTP_PORT, hosts);
  }

  public static Timing requestTimeHA(final int timeoutMillis,
          final int ntpResponseTimeoutMillis, final int port, final String... hosts)
          throws IOException, InterruptedException, TimeoutException {
    List<InetAddress> addrs = new ArrayList<>(hosts.length * 2);
    for (String host : hosts) {
      for (InetAddress addr : InetAddress.getAllByName(host)) {
        addrs.add(addr);
      }
    }
    return requestTimeHA(timeoutMillis,
            ntpResponseTimeoutMillis, port, addrs.toArray(new InetAddress[addrs.size()]));
  }

  public static Timing requestTimeHA(final int timeoutMillis,
          final int ntpResponseTimeoutMillis, final int port, final InetAddress... hosts)
          throws IOException, InterruptedException, TimeoutException {
    final long ntpResponseTimeoutNanos = TimeUnit.MILLISECONDS.toNanos(ntpResponseTimeoutMillis);
    if (hosts.length <= 0) {
      throw new IllegalArgumentException("Must specify at least one host " + java.util.Arrays.toString(hosts));
    }
    try (ExecutionContext ctx = ExecutionContexts.start("requestTimeHA", timeoutMillis, TimeUnit.MILLISECONDS)) {
      return  RetryPolicy.defaultPolicy().call(new Callable<Timing>() {

        private int i = ThreadLocalRandom.current().nextInt(hosts.length);

        @Override
        public Timing call() throws IOException {
          int hostIdx = Math.abs(i++) % hosts.length;
          long contextDe = ctx.getDeadlineNanos();
          long currTime = TimeSource.nanoTime();
          long deadline = (ntpResponseTimeoutNanos > contextDe - currTime)
                  ? contextDe : currTime + ntpResponseTimeoutNanos;
          return requestTime(hosts[hostIdx], port, deadline);
        }
      }, IOException.class, ctx.getDeadlineNanos());
    }
  }

  public static synchronized Timing requestTime(final String host, final int timeoutMillis)
          throws IOException {
    return requestTime(host, NTP_PORT, timeoutMillis);
  }

  @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE") // false positive
  public static Timing requestTime(final String host, final int port, final int timeoutMillis)
          throws IOException {
    return requestTime(host, port, TimeSource.getDeadlineNanos(timeoutMillis, TimeUnit.MILLISECONDS));
  }

  /**
   * Get NTP time.
   *
   * @param host - NTP server host name.
   * @param timeoutMillis - the socket timeout.
   * @return - NTP server timing info.
   * @throws IOException - thrown in case of time server connectivity issues.
   */
  @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE") // false positive
  public static Timing requestTime(final String host, final int port, final long deadlineNanos)
          throws IOException {
    return requestTime(InetAddress.getByName(host), port, deadlineNanos);
  }


  /**
   * Get NTP time.
   *
   * @param address - NTP server addr.
   * @param timeoutMillis - the socket timeout.
   * @return - NTP server timing info.
   * @throws IOException - thrown in case of time server connectivity issues.
   */
  @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE") // false positive
  public static synchronized Timing requestTime(final InetAddress address, final int port, final long deadlineNanos)
          throws IOException {
    try (DatagramSocket socket = new DatagramSocket()) {
      byte[] buffer = new byte[NTP_PACKET_SIZE];
      byte[] clientTime = new byte[8];
      DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, port);

      // set mode = 3 (client) and version = 3
      // mode is in low 3 bits of first byte
      // version is in bits 3-5 of first byte
      buffer[0] = NTP_MODE_CLIENT | (NTP_VERSION << 3);
      // Allocate response.
      DatagramPacket response = new DatagramPacket(buffer, buffer.length);
      long nanoTime = TimeSource.nanoTime();
      // get current nanotime.
      long requestTicks = nanoTime / 1000000L;
      // get current time and write it to the request packet
      long requestTime = System.currentTimeMillis();
      writeTimeStamp(clientTime, 0, requestTime);
      System.arraycopy(clientTime, 0, buffer, TRANSMIT_TIME_OFFSET, 8);
      socket.send(request);
      // read the response
      long msToDeadline = deadlineNanos / 1000000L - requestTicks;
      if (msToDeadline <= 0) {
        throw new SocketTimeoutException("deadline exceded by " + (-msToDeadline) + " ms");
      }
      socket.setSoTimeout((int) msToDeadline);
      socket.receive(response);
      if (!org.spf4j.base.Arrays.equals(clientTime, buffer, 0, ORIGINATE_TIME_OFFSET, 0)) {
        throw new IllegalStateException("Packet with incorrect OriginateTime received " + Arrays.toString(buffer));
      }

      long responseTicks = TimeSource.nanoTime() / 1000000L;
      long roundTripMs = responseTicks - requestTicks;
      long responseTime = requestTime + roundTripMs;

      // extract the results
      // the server copies originateTime from request.
      long originateTime = requestTime; //readTimeStamp(buffer, ORIGINATE_TIME_OFFSET);
      long receiveTime = readTimeStamp(buffer, RECEIVE_TIME_OFFSET);
      long transmitTime = readTimeStamp(buffer, TRANSMIT_TIME_OFFSET);
      final byte leap = (byte) ((buffer[0] >> 6) & 0x3);
      final byte mode = (byte) (buffer[0] & 0x7);
      final int stratum = buffer[1] & 0xff;
      checkValidServerReply(leap, mode, stratum, transmitTime);
      long roundTripTime = roundTripMs - (transmitTime - receiveTime);
      // receiveTime = originateTime + transit + skew
      // responseTime = transmitTime + transit - skew
      // clockOffset = ((receiveTime - originateTime) + (transmitTime - responseTime))/2
      //             = ((originateTime + transit + skew - originateTime) +
      //                (transmitTime - (transmitTime + transit - skew)))/2
      //             = ((transit + skew) + (transmitTime - transmitTime - transit + skew))/2
      //             = (transit + skew - transit + skew)/2
      //             = (2 * skew)/2 = skew
      long clockOffset = ((receiveTime - originateTime) + (transmitTime - responseTime)) / 2;
      return new Timing(responseTime + clockOffset, responseTicks, roundTripTime);
    }
  }

  private static void checkValidServerReply(
          final byte leap, final byte mode, final int stratum, final long transmitTime) {
    if (leap == NTP_LEAP_NOSYNC) {
      throw new IllegalStateException("Unsynchronized server: " + leap);
    } else if ((mode != NTP_MODE_SERVER) && (mode != NTP_MODE_BROADCAST)) {
      throw new IllegalStateException("Untrusted mode: " + mode);
    } else if ((stratum == NTP_STRATUM_DEATH) || (stratum > NTP_STRATUM_MAX)) {
      throw new IllegalStateException("Untrusted stratum: " + stratum);
    } else if (transmitTime == 0) {
      throw new IllegalStateException("Zero transmitTime: " + transmitTime);
    }
  }

  /**
   * Reads an unsigned 32 bit big endian number from the given offset in the buffer.
   */
  private static long read32(final byte[] buffer, final int offset) {
    byte b0 = buffer[offset];
    byte b1 = buffer[offset + 1];
    byte b2 = buffer[offset + 2];
    byte b3 = buffer[offset + 3];

    // convert signed bytes to unsigned values
    int i0 = ((b0 & 0x80) == 0x80 ? (b0 & 0x7F) + 0x80 : b0);
    int i1 = ((b1 & 0x80) == 0x80 ? (b1 & 0x7F) + 0x80 : b1);
    int i2 = ((b2 & 0x80) == 0x80 ? (b2 & 0x7F) + 0x80 : b2);
    int i3 = ((b3 & 0x80) == 0x80 ? (b3 & 0x7F) + 0x80 : b3);

    return ((long) i0 << 24) + ((long) i1 << 16) + ((long) i2 << 8) + (long) i3;
  }

  /**
   * Reads the NTP time stamp at the given offset in the buffer and returns it as a system time (milliseconds since
   * January 1, 1970).
   */
  @VisibleForTesting
  static long readTimeStamp(final byte[] buffer, final int offset) {
    long seconds = read32(buffer, offset);
    long fraction = read32(buffer, offset + 4);
    return ((seconds - OFFSET_1900_TO_1970) * 1000) + ((fraction * 1000L) / 0x100000000L);
  }

  /**
   * Writes system time (milliseconds since January 1, 1970) as an NTP time stamp at the given offset in the buffer.
   */
  @SuppressFBWarnings("PREDICTABLE_RANDOM")
  @VisibleForTesting
  static void writeTimeStamp(final byte[] buffer, final int poffset, final long time) {
    int offset = poffset;
    long seconds = time / 1000L;
    long milliseconds = time % 1000L;
    seconds += OFFSET_1900_TO_1970;

    // write seconds in big endian format
    buffer[offset++] = (byte) (seconds >> 24);
    buffer[offset++] = (byte) (seconds >> 16);
    buffer[offset++] = (byte) (seconds >> 8);
    buffer[offset++] = (byte) (seconds);

    long fraction = milliseconds * 0x100000000L / 1000L;
    // write fraction in big endian format
    buffer[offset++] = (byte) (fraction >> 24);
    buffer[offset++] = (byte) (fraction >> 16);
    buffer[offset++] = (byte) (fraction >> 8);
    // low order bits should be random data
    buffer[offset] = (byte) (ThreadLocalRandom.current().nextInt(256));
  }
}
