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
import gnu.trove.set.hash.THashSet;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Set;
import javax.annotation.Nonnull;
import org.spf4j.io.ObjectAppenderSupplier;

/**
 * A more flexible implementation of the SLF4j message formatter (org.slf4j.helpers.MessageFormatter). the following
 * improvements:
 *
 * 1) Allow to format to a procvided destination (Appendable) allowing you to reduce the amount of garbage generated in
 * a custom formatter... 2) Lets you know which arguments have been used in the message allowing you to implement extra
 * logic to handle the unused ones 3) Lets you plug custom formatters for argument types. (you can get better
 * performance and more flexibility) 4) Processing arguments that are arrays is sligtly faster than the slf4j formatter.
 *
 * @author zoly
 */
public final class Slf4jMessageFormatter {

  private static final char DELIM_START = '{';
  private static final String DELIM_STR = "{}";
  private static final char ESCAPE_CHAR = '\\';


  public interface ErrorHandler {
    void accept(Object obj, Appendable sbuf, Throwable t) throws IOException;
  }



  private Slf4jMessageFormatter() {
  }


  @SuppressWarnings("checkstyle:regexp")
  public static void exHandle(final Object obj, final Appendable sbuf, final Throwable t) throws IOException {
    String className = obj.getClass().getName();
    synchronized (System.err) {
      System.err.print("SPF4J: Failed toString() invocation on an object of type [");
      System.err.print(className);
      System.err.println(']');
    }
    Throwables.writeTo(t, System.err, Throwables.PackageDetail.SHORT);
    sbuf.append("[FAILED toString() for ");
    sbuf.append(className);
    sbuf.append(']');
  }

  public static String toString(@Nonnull final String messagePattern,
          final Object... argArray) {
    StringBuilder sb = new StringBuilder(messagePattern.length() + argArray.length * 8);
    try {
      int nrUsed = format(sb, messagePattern, argArray);
      if (nrUsed != argArray.length) {
        throw new IllegalArgumentException("Invalid format "
                + messagePattern + ", params " + Arrays.toString(argArray));
      }
    } catch (IOException ex) {
     throw new UncheckedIOException(ex);
    }
    return sb.toString();
  }


  /**
   * Slf4j message formatter.
   *
   * @param to Appendable to put formatted message to.
   * @param messagePattern see org.slf4j.helpers.MessageFormatter for format.
   * @param argArray the message arguments.
   * @return the number of arguments used in the message.
   * @throws IOException
   */
  public static int format(@Nonnull final Appendable to, @Nonnull final String messagePattern,
          final Object... argArray) throws IOException {
    return format(to, messagePattern, ObjectAppenderSupplier.TO_STRINGER, argArray);
  }

  /**
   * Slf4j message formatter.
   *
   * @param to Appendable to put formatted message to.
   * @param appSupplier a supplier that will provide the serialization method for a particular argument type.
   * @param messagePattern see org.slf4j.helpers.MessageFormatter for format.
   * @param argArray the message arguments.
   * @return the number of arguments used in the message.
   * @throws IOException
   */
  public static int format(@Nonnull final Appendable to,
          @Nonnull final ObjectAppenderSupplier appSupplier, @Nonnull final String messagePattern,
          final Object... argArray) throws IOException {
    return format(to, messagePattern, appSupplier, argArray);
  }

  /**
   * slf4j message formatter.
   *
   * @param to Appendable to put formatted message to.
   * @param messagePattern see org.slf4j.helpers.MessageFormatter for format.
   * @param appSupplier a supplier that will provide the serialization method for a particular argument type.
   * @param argArray the message arguments.
   * @return the number of arguments used in the message.
   * @throws IOException something wend wrong while writing to the appendable.
   */
  public static int format(@Nonnull final Appendable to, @Nonnull final String messagePattern,
          @Nonnull final ObjectAppenderSupplier appSupplier, final Object... argArray) throws IOException {
    return format(0, to, messagePattern, appSupplier, argArray);
  }

  /**
   * Slf4j message formatter.
   *
   * @param to Appendable to put formatted message to.
   * @param messagePattern see org.slf4j.helpers.MessageFormatter for format.
   * @param appSupplier a supplier that will provide the serialization method for a particular argument type.
   * @param firstArgIdx the index of the first parameter.
   * @param argArray the message arguments.
   * @return the index of the last arguments used in the message + 1.
   * @throws IOException something wend wrong while writing to the appendable.
   */
  public static int format(final int firstArgIdx, @Nonnull final Appendable to, @Nonnull final String messagePattern,
          @Nonnull final ObjectAppenderSupplier appSupplier, final Object... argArray) throws IOException {
    return format(Slf4jMessageFormatter::exHandle, firstArgIdx, to, messagePattern, appSupplier, argArray);
  }

  /**
   * Slf4j message formatter.
   *
   * @param safe - if true recoverable exHandle will be caught when writing arguments, and a error will be appended
 instead.
   * @param to Appendable to put formatted message to.
   * @param messagePattern see org.slf4j.helpers.MessageFormatter for format.
   * @param appSupplier a supplier that will provide the serialization method for a particular argument type.
   * @param firstArgIdx the index of the first parameter.
   * @param argArray the message arguments.
   * @return the index of the last arguments used in the message + 1.
   * @throws IOException something wend wrong while writing to the appendable.
   */
  public static int format(final ErrorHandler exHandler, final int firstArgIdx,
          @Nonnull final Appendable to, @Nonnull final String messagePattern,
          @Nonnull final ObjectAppenderSupplier appSupplier, final Object... argArray)
          throws IOException {
    int i = 0;
    final int len = argArray.length;
    int k = firstArgIdx;
    for (; k < len; k++) {
      int j = messagePattern.indexOf(DELIM_STR, i);
      if (j == -1) {
        // no more variables
        break;
      } else {
        if (isEscapedDelimeter(messagePattern, j)) {
          if (!isDoubleEscaped(messagePattern, j)) {
            k--; // DELIM_START was escaped, thus should not be incremented
            to.append(messagePattern, i, j - 1);
            to.append(DELIM_START);
            i = j + 1;
          } else {
            // The escape character preceding the delimiter start is
            // itself escaped: "abc x:\\{}"
            // we have to consume one backward slash
            to.append(messagePattern, i, j - 1);
            deeplyAppendParameter(exHandler, to, argArray[k], new THashSet<>(), appSupplier);
            i = j + 2;
          }
        } else {
          // normal case
          to.append(messagePattern, i, j);
          deeplyAppendParameter(exHandler, to, argArray[k], new THashSet<>(), appSupplier);
          i = j + 2;
        }
      }
    }
    // append the characters following the last {} pair.
    to.append(messagePattern, i, messagePattern.length());
    return k;
  }

  private static boolean isEscapedDelimeter(final String messagePattern, final int delimeterStartIndex) {
    if (delimeterStartIndex == 0) {
      return false;
    }
    return messagePattern.charAt(delimeterStartIndex - 1) == ESCAPE_CHAR;
  }

  private static boolean isDoubleEscaped(final String messagePattern, final int delimeterStartIndex) {
    return delimeterStartIndex >= 2 && messagePattern.charAt(delimeterStartIndex - 2) == ESCAPE_CHAR;
  }

  // special treatment of array values was suggested by 'lizongbo'
  @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
  private static void deeplyAppendParameter(final ErrorHandler exHandler, final Appendable sbuf, final Object o,
          final Set<Object[]> seen, final ObjectAppenderSupplier appSupplier) throws IOException {
    if (o == null) {
      sbuf.append("null");
      return;
    }
    if (!o.getClass().isArray()) {
      safeObjectAppend(exHandler, sbuf, o, appSupplier);
    } else {
      // check for primitive array types because they
      // unfortunately cannot be cast to Object[]
      if (o instanceof boolean[]) {
        booleanArrayAppend(sbuf, (boolean[]) o);
      } else if (o instanceof byte[]) {
        byteArrayAppend(sbuf, (byte[]) o);
      } else if (o instanceof char[]) {
        charArrayAppend(sbuf, (char[]) o);
      } else if (o instanceof short[]) {
        shortArrayAppend(sbuf, (short[]) o);
      } else if (o instanceof int[]) {
        intArrayAppend(sbuf, (int[]) o);
      } else if (o instanceof long[]) {
        longArrayAppend(sbuf, (long[]) o);
      } else if (o instanceof float[]) {
        floatArrayAppend(sbuf, (float[]) o);
      } else if (o instanceof double[]) {
        doubleArrayAppend(sbuf, (double[]) o);
      } else {
        objectArrayAppend(exHandler, sbuf, (Object[]) o, seen, appSupplier);
      }
    }
  }

  @SuppressWarnings("unchecked")
  public static void safeObjectAppend(final ErrorHandler exHandler, final Appendable sbuf, final Object obj,
          final ObjectAppenderSupplier appSupplier) throws IOException {
    try {
      appSupplier.get((Class) obj.getClass()).append(obj, sbuf);
    } catch (IOException | RuntimeException | StackOverflowError t) {
      exHandler.accept(obj, sbuf, t);
    }

  }

  @SuppressFBWarnings("ABC_ARRAY_BASED_COLLECTIONS")
  private static void objectArrayAppend(final ErrorHandler exHandler, final Appendable sbuf,
          final Object[] a, final Set<Object[]> seen,
          final ObjectAppenderSupplier appSupplier) throws IOException {
    sbuf.append('[');
    if (seen.add(a)) {
      final int len = a.length;
      if (len > 0) {
        deeplyAppendParameter(exHandler, sbuf, a[0], seen, appSupplier);
        for (int i = 1; i < len; i++) {
          sbuf.append(", ");
          deeplyAppendParameter(exHandler, sbuf, a[i], seen, appSupplier);
        }
      }
      // allow repeats in siblings
      seen.remove(a);
    } else {
      sbuf.append("...");
    }
    sbuf.append(']');
  }

  private static void booleanArrayAppend(final Appendable sbuf, final boolean[] a) throws IOException {
    sbuf.append('[');
    final int len = a.length;
    if (len > 0) {
      sbuf.append(Boolean.toString(a[0]));
      for (int i = 1; i < len; i++) {
        sbuf.append(", ");
        sbuf.append(Boolean.toString(a[i]));
      }
    }
    sbuf.append(']');
  }

  private static void byteArrayAppend(final Appendable sbuf, final byte[] a) throws IOException {
    sbuf.append('[');
    final int len = a.length;
    if (len > 0) {
      sbuf.append(Byte.toString(a[0]));
      for (int i = 1; i < len; i++) {
        sbuf.append(", ");
        sbuf.append(Byte.toString(a[i]));
      }
    }
    sbuf.append(']');
  }

  private static void charArrayAppend(final Appendable sbuf, final char[] a) throws IOException {
    sbuf.append('[');
    final int len = a.length;
    if (len > 0) {
      sbuf.append(a[0]);
      for (int i = 1; i < len; i++) {
        sbuf.append(", ");
        sbuf.append(a[i]);
      }
    }
    sbuf.append(']');
  }

  private static void shortArrayAppend(final Appendable sbuf, final short[] a) throws IOException {
    sbuf.append('[');
    final int len = a.length;
    if (len > 0) {
      sbuf.append(Short.toString(a[0]));
      for (int i = 1; i < len; i++) {
        sbuf.append(", ");
        sbuf.append(Short.toString(a[i]));
      }
    }
    sbuf.append(']');
  }

  private static void intArrayAppend(final Appendable sbuf, final int[] a) throws IOException {
    sbuf.append('[');
    final int len = a.length;
    if (len > 0) {
      sbuf.append(Integer.toString(a[0]));
      for (int i = 1; i < len; i++) {
        sbuf.append(", ");
        sbuf.append(Integer.toString(a[i]));
      }
    }
    sbuf.append(']');
  }

  private static void longArrayAppend(final Appendable sbuf, final long[] a) throws IOException {
    sbuf.append('[');
    final int len = a.length;
    if (len > 0) {
      sbuf.append(Long.toString(a[0]));
      for (int i = 1; i < len; i++) {
        sbuf.append(", ");
        sbuf.append(Long.toString(a[i]));
      }
    }
    sbuf.append(']');
  }

  private static void floatArrayAppend(final Appendable sbuf, final float[] a) throws IOException {
    sbuf.append('[');
    final int len = a.length;
    if (len > 0) {
      sbuf.append(Float.toString(a[0]));
      for (int i = 1; i < len; i++) {
        sbuf.append(", ");
        sbuf.append(Float.toString(a[i]));
      }
    }
    sbuf.append(']');
  }

  private static void doubleArrayAppend(final Appendable sbuf, final double[] a) throws IOException {
    sbuf.append('[');
    final int len = a.length;
    if (len > 0) {
      sbuf.append(Double.toString(a[0]));
      for (int i = 1; i < len; i++) {
        sbuf.append(", ");
        sbuf.append(Double.toString(a[i]));
      }
    }
    sbuf.append(']');
  }

}
