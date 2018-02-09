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
import java.io.IOException;
import java.nio.CharBuffer;
import javax.annotation.Nonnull;

/**
 *
 * @author Zoltan Farkas
 */
public final class AppendableUtils {

  private static final ThreadLocal<char[]> BUFF = new ThreadLocal<char[]>() {

    @Override
    @SuppressFBWarnings("SUA_SUSPICIOUS_UNINITIALIZED_ARRAY")
    protected char[] initialValue() {
      return new char[64];
    }

  };

  private static final char[] DIGITS = {
    '0', '1', '2', '3', '4', '5',
    '6', '7', '8', '9', 'a', 'b',
    'c', 'd', 'e', 'f', 'g', 'h',
    'i', 'j', 'k', 'l', 'm', 'n',
    'o', 'p', 'q', 'r', 's', 't',
    'u', 'v', 'w', 'x', 'y', 'z'
  };


  private AppendableUtils() { }

    /**
   * Utility method to escape java strings to json strings.
   *
   * @param toEscape - the java string to escape.
   * @param jsonString - the destination json String builder.
   */
  public static void escapeJsonString(@Nonnull final CharSequence toEscape, final StringBuilder jsonString) {

    int len = toEscape.length();
    for (int i = 0; i < len; i++) {
      char c = toEscape.charAt(i);
      appendJsonStringEscapedChar(c, jsonString);
    }

  }

  public static void escapeJsonString(@Nonnull final CharSequence toEscape, final Appendable jsonString)
          throws IOException {
    int len = toEscape.length();
    for (int i = 0; i < len; i++) {
      char c = toEscape.charAt(i);
      appendJsonStringEscapedChar(c, jsonString);
    }
  }

  public static void appendJsonStringEscapedChar(final char c, final StringBuilder jsonString) {
    switch (c) {
      case '\\':
      case '"':
        jsonString.append('\\');
        jsonString.append(c);
        break;
      case '\b':
        jsonString.append("\\b");
        break;
      case '\t':
        jsonString.append("\\t");
        break;
      case '\n':
        jsonString.append("\\n");
        break;
      case '\f':
        jsonString.append("\\f");
        break;
      case '\r':
        jsonString.append("\\r");
        break;
      default:
        if (c < ' ') {
          jsonString.append("\\u");
          appendUnsignedStringPadded(jsonString, (int) c, 4, 4);
        } else {
          jsonString.append(c);
        }
    }
  }

  public static void appendJsonStringEscapedChar(final char c, final Appendable jsonString) throws IOException {
    switch (c) {
      case '\\':
      case '"':
        jsonString.append('\\');
        jsonString.append(c);
        break;
      case '\b':
        jsonString.append("\\b");
        break;
      case '\t':
        jsonString.append("\\t");
        break;
      case '\n':
        jsonString.append("\\n");
        break;
      case '\f':
        jsonString.append("\\f");
        break;
      case '\r':
        jsonString.append("\\r");
        break;
      default:
        if (c < ' ') {
          jsonString.append("\\u");
          appendUnsignedStringPadded(jsonString, (int) c, 4, 4);
        } else {
          jsonString.append(c);
        }
    }
  }

  public static void appendUnsignedStringPadded(final StringBuilder sb, final int nr, final int shift,
          final int padTo) {
    long i = nr;
    char[] buf = BUFF.get();
    int charPos = 32;
    int radix = 1 << shift;
    long mask = radix - 1;
    do {
      buf[--charPos] = DIGITS[(int) (i & mask)];
      i >>>= shift;
    } while (i != 0);
    final int nrChars = 32 - charPos;
    if (nrChars > padTo) {
      throw new IllegalArgumentException("Your pad to value " + padTo
              + " is to small, must be at least " + nrChars);
    } else {
      for (int j = 0, n = padTo - nrChars; j < n; j++) {
        sb.append('0');
      }
    }
    sb.append(buf, charPos, nrChars);
  }


  public static void appendUnsignedStringPadded(final Appendable sb, final int nr, final int shift,
          final int padTo) throws IOException {
    long i = nr;
    char[] buf = BUFF.get();
    int charPos = 32;
    int radix = 1 << shift;
    long mask = radix - 1;
    do {
      buf[--charPos] = DIGITS[(int) (i & mask)];
      i >>>= shift;
    } while (i != 0);
    final int nrChars = 32 - charPos;
    if (nrChars > padTo) {
      throw new IllegalArgumentException("Your pad to value " + padTo
              + " is to small, must be at least " + nrChars);
    } else {
      for (int j = 0, n = padTo - nrChars; j < n; j++) {
        sb.append('0');
      }
    }
    sb.append(CharBuffer.wrap(buf), charPos, charPos + nrChars);
  }

  public static void appendUnsignedString(final StringBuilder sb, final long nr, final int shift) {
    long i = nr;
    char[] buf = BUFF.get();
    int charPos = 64;
    int radix = 1 << shift;
    long mask = radix - 1;
    do {
      buf[--charPos] = DIGITS[(int) (i & mask)];
      i >>>= shift;
    } while (i != 0);
    sb.append(buf, charPos, 64 - charPos);
  }

  public static void appendUnsignedString(final StringBuilder sb, final int nr, final int shift) {
    long i = nr;
    char[] buf = BUFF.get();
    int charPos = 32;
    int radix = 1 << shift;
    long mask = radix - 1;
    do {
      buf[--charPos] = DIGITS[(int) (i & mask)];
      i >>>= shift;
    } while (i != 0);
    sb.append(buf, charPos, 32 - charPos);
  }

  public static void appendSpaces(final Appendable to, final int nrSpaces) throws IOException {
    for (int i = 0; i < nrSpaces; i++) {
      to.append(' ');
    }
  }

  public static void appendSpaces(final StringBuilder to, final int nrSpaces) {
    for (int i = 0; i < nrSpaces; i++) {
      to.append(' ');
    }
  }


}
