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

package org.spf4j.net;



import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import org.spf4j.base.Callables;
import org.spf4j.base.IntMath;

/**
 * Simple NTP client. Inspired by Android Sntp client.
 * For how to use, see SntpClientTest.java.
 * @author zoly
 */

public final class SntpClient {

    private SntpClient() { }

    private static final int ORIGINATE_TIME_OFFSET = 24;
    private static final int RECEIVE_TIME_OFFSET = 32;
    private static final int TRANSMIT_TIME_OFFSET = 40;
    private static final int NTP_PACKET_SIZE = 48;

    private static final int NTP_PORT = 123;
    private static final int NTP_MODE_CLIENT = 3;
    private static final int NTP_VERSION = 3;

    // Number of seconds between Jan 1, 1900 and Jan 1, 1970
    // 70 years plus 17 leap days
    private static final long OFFSET_1900_TO_1970 = ((365L * 70L) + 17L) * 24L * 60L * 60L;

    private static final int MAX_SOCKET_TIMEOUT = 1000; // 5 seconds
    public static Timing requestTimeHA(final int timeoutMillis, final String ... hosts)
            throws IOException, InterruptedException {
        return requestTimeHA(timeoutMillis, MAX_SOCKET_TIMEOUT, hosts);
    }

    /**
     * Request NTP time with retries.
     *
     * @param hosts - NTP server hosts.
     * @param timeoutMillis - Max time to attempt to get  NTP time
     * @param ntpResponseTimeout - the time after which if we do not receive a response from the NTP server,
     *                              we consider the call failed (and will retry until timeoutMillis.
     * @return Ntp timing info.
     * @throws IOException - thrown in case of time server connectivity issues.
     * @throws InterruptedException - thrown if exec interrupted.
     */
    @SuppressFBWarnings("BED_BOGUS_EXCEPTION_DECLARATION") //findbugs nonsense
    public static Timing requestTimeHA(final int timeoutMillis, final int ntpResponseTimeout, final String ... hosts)
            throws IOException, InterruptedException {
        return Callables.executeWithRetry(new Callables.TimeoutCallable<Timing, IOException>(timeoutMillis) {

            private int i = 0;

            @Override
            @SuppressFBWarnings("BED_BOGUS_EXCEPTION_DECLARATION") //findbugs nonsense
            public Timing call(final long deadline) throws IOException {
                int hostIdx = Math.abs(i++) % hosts.length;
                return requestTime(hosts[hostIdx],
                        Math.min((int) (deadline - System.currentTimeMillis()), ntpResponseTimeout));
            }
        }, 3, 1000);
    }

    /**
     * Get NTP time.
     * @param host - NTP server host name.
     * @param timeoutMillis - the socket timeout.
     * @return - NTP server timing info.
     * @throws IOException - thrown in case of time server connectivity issues.
     */
    @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE") // false positive
    public static Timing requestTime(final String host, final int timeoutMillis) throws IOException {

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(timeoutMillis);
            InetAddress address = InetAddress.getByName(host);
            byte[] buffer = new byte[NTP_PACKET_SIZE];
            DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, NTP_PORT);

            // set mode = 3 (client) and version = 3
            // mode is in low 3 bits of first byte
            // version is in bits 3-5 of first byte
            buffer[0] = NTP_MODE_CLIENT | (NTP_VERSION << 3);
            // Allocate response.
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            // get current nanotime.
            long requestTicks = System.nanoTime() / 1000000L;
            // get current time and write it to the request packet
            long requestTime = System.currentTimeMillis();
            writeTimeStamp(buffer, TRANSMIT_TIME_OFFSET, requestTime);
            socket.send(request);
            // read the response
            socket.receive(response);
            long responseTicks = System.nanoTime() / 1000000L;
            long responseTime = requestTime + (responseTicks - requestTicks);

            // extract the results
            long originateTime = readTimeStamp(buffer, ORIGINATE_TIME_OFFSET);
            long receiveTime = readTimeStamp(buffer, RECEIVE_TIME_OFFSET);
            long transmitTime = readTimeStamp(buffer, TRANSMIT_TIME_OFFSET);
            long roundTripTime = responseTicks - requestTicks - (transmitTime - receiveTime);
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
     * Reads the NTP time stamp at the given offset in the buffer and returns
     * it as a system time (milliseconds since January 1, 1970).
     */
    private static long readTimeStamp(final byte[] buffer, final int offset) {
        long seconds = read32(buffer, offset);
        long fraction = read32(buffer, offset + 4);
        return ((seconds - OFFSET_1900_TO_1970) * 1000) + ((fraction * 1000L) / 0x100000000L);
    }

    private static final IntMath.XorShift32 RANDOM = new IntMath.XorShift32();

    /**
     * Writes system time (milliseconds since January 1, 1970) as an NTP time stamp
     * at the given offset in the buffer.
     */
    private static void writeTimeStamp(final byte[] buffer, final int poffset, final long time) {
        int offset = poffset;
        long seconds = time / 1000L;
        long milliseconds = time - seconds * 1000L;
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
        buffer[offset] = (byte) (RANDOM.nextInt() % 256);
    }
}