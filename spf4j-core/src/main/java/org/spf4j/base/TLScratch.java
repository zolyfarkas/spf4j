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
package org.spf4j.base;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

/**
 *
 * @author zoly
 */
public final class TLScratch {

  private static final int MAX_LOCAL_ARRAY_SIZE = Integer.getInteger("spf4j.TLScratch.maxLocalArraySize",  256 * 1024);

  private static final ThreadLocal<Reference<byte[]>> BYTES_TMP = new ThreadLocal<>();

  private static final ThreadLocal<Reference<char[]>> CHARS_TMP = new ThreadLocal<>();

  private TLScratch() { }

  /**
   * returns a thread local byte array of at least the size requested. use only for temporary purpose. This method needs
   * to be carefully used!
   *
   * @param size - the minimum size of the temporary buffer requested.
   * @return - the temporary buffer.
   */
  public static byte[] getBytesTmp(final int size) {
    if (size > MAX_LOCAL_ARRAY_SIZE) {
      return new byte[size];
    }
    Reference<byte[]> sr = BYTES_TMP.get();
    byte[] result;
    if (sr == null) {
      result = new byte[size];
      BYTES_TMP.set(new SoftReference<>(result));
    } else {
      result = sr.get();
      if (result == null || result.length < size) {
        result = new byte[size];
        BYTES_TMP.set(new SoftReference<>(result));
      }
    }
    return result;
  }

  /**
   * returns a thread local char array of at least the requested size. Use only for temporary purpose.
   *
   * @param size - the minimum size of the temporary buffer requested.
   * @return - the temporary buffer.
   */
  @SuppressFBWarnings("SUA_SUSPICIOUS_UNINITIALIZED_ARRAY")
  public static char[] getCharsTmp(final int size) {
    if (size > MAX_LOCAL_ARRAY_SIZE) {
      return new char[size];
    }
    Reference<char[]> sr = CHARS_TMP.get();
    char[] result;
    if (sr == null) {
      result = new char[size];
      CHARS_TMP.set(new SoftReference<>(result));
    } else {
      result = sr.get();
      if (result == null || result.length < size) {
        result = new char[size];
        CHARS_TMP.set(new SoftReference<>(result));
      }
    }
    return result;
  }

}
