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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.base.Strings;
import static org.spf4j.base.Strings.appendUnsignedString;

/**
 * Unique ID Generator Based on the assumptions:
 * 1. host MAC address is used. (each network interface has a Unique ID) (encoded with provided encoder)
 * 2. process id is used + current epoch seconds. it is assumed the PID is not recycled within a second.
 * 3. A process sequence is used. UIDs will cycle after Long.MaxValue is reached.
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class UIDGenerator implements Supplier<CharSequence> {

    private final Sequence sequence;

    private final StringBuilder base;

    private final int maxSize;

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
    @SuppressFBWarnings("STT_TOSTRING_STORED_IN_FIELD")
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
                        intfMac = new byte[] {0};
                    }
                } else {
                    intfMac = new byte[] {0};
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
        maxSize = base.length() + 16;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public CharSequence next() {
        StringBuilder result = new StringBuilder(maxSize);
        result.append(base);
        Strings.appendUnsignedString(result, sequence.next(), 5);
        return result;
    }



    @Override
    public String toString() {
        return "UIDGenerator{" + "sequence=" + sequence + ", base=" + base + ", maxSize=" + maxSize + '}';
    }

    @Override
    public CharSequence get() {
      return next();
    }



}
