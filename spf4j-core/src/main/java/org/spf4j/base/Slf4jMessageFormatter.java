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

package org.spf4j.base;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gnu.trove.set.hash.THashSet;
import java.io.IOException;
import java.util.Set;
import org.slf4j.helpers.Util;
import org.spf4j.io.ObjectAppenderSupplier;

/**
 *
 * @author zoly
 */
public final  class Slf4jMessageFormatter {
    
    private Slf4jMessageFormatter() { }
    

    static final char DELIM_START = '{';
    static final char DELIM_STOP = '}';
    static final String DELIM_STR = "{}";
    private static final char ESCAPE_CHAR = '\\';
    
    
    public static int format(final Appendable sbuf, final String messagePattern,
            final Object... argArray) throws IOException {
        return format(sbuf, messagePattern, ObjectAppenderSupplier.TO_STRINGER, argArray);
    }
    
    public static int format(final Appendable sbuf, final String messagePattern,
            final ObjectAppenderSupplier appSupplier, final Object... argArray) throws IOException {
        int i = 0;
        final int len = argArray.length;
        for (int k = 0; k < len; k++) {

            int j = messagePattern.indexOf(DELIM_STR, i);

            if (j == -1) {
                // no more variables
                if (i == 0) { // this is a simple string
                    return k;
                } else { // add the tail string which contains no variables and return
                    // the result.
                    sbuf.append(messagePattern, i, messagePattern.length());
                    return k;
                }
            } else {
                if (isEscapedDelimeter(messagePattern, j)) {
                    if (!isDoubleEscaped(messagePattern, j)) {
                        k--; // DELIM_START was escaped, thus should not be incremented
                        sbuf.append(messagePattern, i, j - 1);
                        sbuf.append(DELIM_START);
                        i = j + 1;
                    } else {
                        // The escape character preceding the delimiter start is
                        // itself escaped: "abc x:\\{}"
                        // we have to consume one backward slash
                        sbuf.append(messagePattern, i, j - 1);
                        deeplyAppendParameter(sbuf, argArray[k], new THashSet<Object[]>(), appSupplier);
                        i = j + 2;
                    }
                } else {
                    // normal case
                    sbuf.append(messagePattern, i, j);
                    deeplyAppendParameter(sbuf, argArray[k], new THashSet<Object[]>(), appSupplier);
                    i = j + 2;
                }
            }
        }
        // append the characters following the last {} pair.
        sbuf.append(messagePattern, i, messagePattern.length());
        return len;
    }
    
    static boolean isEscapedDelimeter(final String messagePattern, final int delimeterStartIndex) {
        if (delimeterStartIndex == 0) {
            return false;
        }
        return messagePattern.charAt(delimeterStartIndex - 1) == ESCAPE_CHAR;
    }
    
    static boolean isDoubleEscaped(final String messagePattern, final int delimeterStartIndex) {
        return delimeterStartIndex >= 2 && messagePattern.charAt(delimeterStartIndex - 2) == ESCAPE_CHAR;
    }

    // special treatment of array values was suggested by 'lizongbo'
    @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
    private static void deeplyAppendParameter(final Appendable sbuf, final Object o,
            final Set<Object[]> seen, final ObjectAppenderSupplier appSupplier) throws IOException {
        if (o == null) {
            sbuf.append("null");
            return;
        }
        if (!o.getClass().isArray()) {
            safeObjectAppend(sbuf, o, appSupplier);
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
                objectArrayAppend(sbuf, (Object[]) o, seen, appSupplier);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void safeObjectAppend(final Appendable sbuf, final Object obj,
            final ObjectAppenderSupplier appSupplier) throws IOException {
        try {
            appSupplier.get((Class) obj.getClass()).append(obj, sbuf);
        } catch (Throwable t) {
            Util.report("SLF4J: Failed toString() invocation on an object of type ["
                    + obj.getClass().getName() + "]", t);
            sbuf.append("[FAILED toString()]");
        }

    }

    @SuppressFBWarnings("ABC_ARRAY_BASED_COLLECTIONS")
    private static void objectArrayAppend(final Appendable sbuf, final Object[] a, final Set<Object[]> seen,
            final ObjectAppenderSupplier appSupplier) throws IOException {
        sbuf.append('[');
        if (!seen.contains(a)) {
            seen.add(a);
            final int len = a.length;
            if (len > 0) {
                deeplyAppendParameter(sbuf, a[0], seen, appSupplier);
                for (int i = 1; i < len; i++) {
                    sbuf.append(", ");
                    deeplyAppendParameter(sbuf, a[i], seen, appSupplier);
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
