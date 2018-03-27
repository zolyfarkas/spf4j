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
package org.spf4j.concurrent;

import com.google.common.io.BaseEncoding;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.function.Supplier;
import javax.annotation.ParametersAreNonnullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.AppendableUtils;

/**
 * Unique ID Generator Based on the assumptions: 1. host MAC address is used. (each network interface has a Unique ID)
 * (encoded with provided encoder) 2. process id is used + current epoch seconds. it is assumed the PID is not recycled
 * within a second. 3. A process sequence is used. UIDs will cycle after Long.MaxValue is reached.
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class UIDGenerator implements Supplier<CharSequence> {

  private static final Logger LOG = LoggerFactory.getLogger(UIDGenerator.class);

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

  public UIDGenerator(final Sequence sequence, final String prefix, final long customEpoch) {
    this(sequence, BaseEncoding.base64Url(), customEpoch, '.', prefix);
  }

  /**
   * Construct a UID Generator
   *
   * @param sequence
   * @param baseEncoding - if null MAC address based ID will not be included.
   */
  @SuppressFBWarnings("STT_TOSTRING_STORED_IN_FIELD")
  public UIDGenerator(final Sequence sequence, final BaseEncoding baseEncoding,
          final long customEpoch, final char separator, final String prefix) {
    this.sequence = sequence;
    StringBuilder sb = generateIdBase(prefix, baseEncoding, separator, customEpoch);
    base = sb;
    maxSize = base.length() + 16;
  }

  public static StringBuilder generateIdBase(final String prefix,
          final char separator) {
    return generateIdBase(prefix, BaseEncoding.base64Url(), separator, 1509741164184L);
  }

  public static StringBuilder generateIdBase(final String prefix,
          final char separator,
          final long customEpoch) {
    return generateIdBase(prefix, BaseEncoding.base64Url(), separator, customEpoch);
  }

  public static StringBuilder generateIdBase(final String prefix,
          final BaseEncoding baseEncoding, final char separator,
          final long customEpoch) {
    StringBuilder sb = new StringBuilder(16 + prefix.length());
    sb.append(prefix);

    byte[] intfMac;
    try {
      Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
      if (networkInterfaces != null && networkInterfaces.hasMoreElements()) {
        do {
          intfMac = networkInterfaces.nextElement().getHardwareAddress();
        } while ((intfMac == null || intfMac.length == 0) && networkInterfaces.hasMoreElements());
        if (intfMac == null) {
          LOG.info("Unable to get interface MAC address for ID generation");
          intfMac = new byte[]{0};
        }
      } else {
        LOG.info("Unable to get interface MAC address for ID generation");
        intfMac = new byte[]{0};
      }
    } catch (SocketException ex) {
      LOG.info("Unable to get interface MAC address for ID generation", ex);
      intfMac = new byte[]{0};
    }
    sb.append(baseEncoding.encode(intfMac)).append(separator);

    AppendableUtils.appendUnsignedString(sb, org.spf4j.base.Runtime.PID, 5);
    sb.append(separator);
    AppendableUtils.appendUnsignedString(sb, (System.currentTimeMillis() - customEpoch) / 1000, 5);
    sb.append(separator);
    return sb;
  }

  public int getMaxSize() {
    return maxSize;
  }

  public CharSequence next() {
    StringBuilder result = new StringBuilder(maxSize);
    result.append(base);
    AppendableUtils.appendUnsignedString(result, sequence.next(), 5);
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
