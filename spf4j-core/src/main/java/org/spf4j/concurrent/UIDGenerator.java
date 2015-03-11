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
package org.spf4j.concurrent;

import com.google.common.io.BaseEncoding;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Unique ID Generator Based on the assumptions:
 * 1. host MAC address is used. (each network interface has a Unique ID) (encoded with provided encoder)
 * 2. process id is used + current epoch seconds. it is assumed the PID is not recycled within a second.
 * 3. A process sequence is used. UIDs will cycle after Long.MaxValue is reached.
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class UIDGenerator {

    private final Sequence sequence;

    private final StringBuilder base;

    private final int maxSize;

    private final int baseLength;

    public UIDGenerator(final Sequence sequence) {
        this(sequence, 0);
    }

    public UIDGenerator(final Sequence sequence, final String prefix) {
        this(sequence, BaseEncoding.base64Url(), 0, '.', prefix);
    }


    public UIDGenerator(final Sequence sequence, final long customEpoch) {
        this(sequence, BaseEncoding.base64Url(), customEpoch, '.', "");
    }

    /**
     * Construct a UID Generator
     * @param sequence
     * @param baseEncoding - if null MAC address based ID will not be included.
     */
    public UIDGenerator(final Sequence sequence, @Nullable final BaseEncoding baseEncoding,
            final long customEpoch, final char separator, final String prefix) {
        this.sequence = sequence;
        StringBuilder sb = new StringBuilder(16 + prefix.length());
        sb.append(prefix);
        if (baseEncoding != null) {
            byte[] intfMac;
            try {
                Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                if (networkInterfaces != null && networkInterfaces.hasMoreElements()) {
                    do {
                        intfMac = networkInterfaces.nextElement().getHardwareAddress();
                    } while ((intfMac == null || intfMac.length == 0) && networkInterfaces.hasMoreElements());
                    if (intfMac == null) {
                        intfMac = new byte [] {0};
                    }
                } else {
                    intfMac = new byte [] {0};
                }
            } catch (SocketException ex) {
                throw new RuntimeException(ex);
            }
            sb.append(baseEncoding.encode(intfMac)).append(separator);
        }
        appendUnsignedString(sb, org.spf4j.base.Runtime.PID, 5);
        sb.append(separator);
        appendUnsignedString(sb, (System.currentTimeMillis() - customEpoch) / 1000, 5);
        sb.append(separator);
        base = sb;
        baseLength = base.length();
        maxSize = baseLength + 16;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public CharSequence next() {
        base.setLength(baseLength);
        appendUnsignedString(base, sequence.next(), 5);
        return base.toString();
    }

    private static final char[] DIGITS = {
        '0', '1', '2', '3', '4', '5',
        '6', '7', '8', '9', 'a', 'b',
        'c', 'd', 'e', 'f', 'g', 'h',
        'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't',
        'u', 'v', 'w', 'x', 'y', 'z'
    };

    private static void appendUnsignedString(final StringBuilder sb, final long nr, final int shift) {
        long i = nr;
        char[] buf = new char[64];
        int charPos = 64;
        int radix = 1 << shift;
        long mask = radix - 1;
        do {
            buf[--charPos] = DIGITS[(int) (i & mask)];
            i >>>= shift;
        } while (i != 0);
        sb.append(buf, charPos, (64 - charPos));
    }

}
