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

import java.lang.ref.SoftReference;
import java.lang.reflect.Array;

/**
 * Array utilities.
 *
 * @author zoly
 */
public final class Arrays {

    private Arrays() {
    }

    public static double[] getColumnAsDoubles(final long[][] data, final int columnNumber) {
        double[] result = new double[data.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = data[i][columnNumber];
        }
        return result;
    }

    public static double[] getColumn(final double[][] data, final int columnNumber) {
        double[] result = new double[data.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = data[i][columnNumber];
        }
        return result;
    }

    public static double[] toDoubleArray(final long ... larr) {
        double[] result = new double[larr.length];
        for (int i = 0; i < larr.length; i++) {
            result[i] = larr[i];
        }
        return result;
    }

    public static double[] divide(final double[] arr1, final double[] arr2) {
        double[] result = new double[arr1.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = arr1[i] / arr2[i];
        }
        return result;
    }

    public static boolean deepEquals(final Object[] a1, final Object[] a2, final int starting) {
        return deepEquals(a1, a2, starting, a1.length);
    }

    public static boolean deepEquals(final Object[] a1, final Object[] a2, final int starting, final int ending) {
        if (a1 == a2) {
            return true;
        }
        if (a1 == null || a2 == null) {
            return false;
        }

        for (int i = starting; i < ending; i++) {
            Object e1 = a1[i];
            Object e2 = a2[i];

            if (e1 == e2) {
                continue;
            }
            if (e1 == null) {
                return false;
            }

            // Figure out whether the two elements are equal
            boolean eq;
            if (e1 instanceof Object[] && e2 instanceof Object[]) {
                eq = java.util.Arrays.deepEquals((Object[]) e1, (Object[]) e2);
            } else if (e1 instanceof byte[] && e2 instanceof byte[]) {
                eq = java.util.Arrays.equals((byte[]) e1, (byte[]) e2);
            } else if (e1 instanceof short[] && e2 instanceof short[]) {
                eq = java.util.Arrays.equals((short[]) e1, (short[]) e2);
            } else if (e1 instanceof int[] && e2 instanceof int[]) {
                eq = java.util.Arrays.equals((int[]) e1, (int[]) e2);
            } else if (e1 instanceof long[] && e2 instanceof long[]) {
                eq = java.util.Arrays.equals((long[]) e1, (long[]) e2);
            } else if (e1 instanceof char[] && e2 instanceof char[]) {
                eq = java.util.Arrays.equals((char[]) e1, (char[]) e2);
            } else if (e1 instanceof float[] && e2 instanceof float[]) {
                eq = java.util.Arrays.equals((float[]) e1, (float[]) e2);
            } else if (e1 instanceof double[] && e2 instanceof double[]) {
                eq = java.util.Arrays.equals((double[]) e1, (double[]) e2);
            } else if (e1 instanceof boolean[] && e2 instanceof boolean[]) {
                eq = java.util.Arrays.equals((boolean[]) e1, (boolean[]) e2);
            } else {
                eq = e1.equals(e2);
            }

            if (!eq) {
                return false;
            }
        }
        return true;
    }

    public static int search(final char [] array, final char c) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == c) {
                return i;
            }
        }
        return -1;
    }

    public static <T> T[] moveOfRange(final T[] original, final int from, final int to) {
        int newLength = to - from;
        if (newLength < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        Class<?> newType = original.getClass();
        T[] copy = ((Object) newType == (Object) Object[].class)
            ? (T[]) new Object[newLength]
            : (T[]) Array.newInstance(newType.getComponentType(), newLength);
        for (int i = from, j = 0; i < to; i++, j++) {
            copy[j] = original[i];
            original[i] = null;
        }
        return copy;
    }

    public static <T> T[] concat(final T[] ... arrays) {
        if (arrays.length < 2) {
            throw new IllegalArgumentException("You should concatenate at least 2 arrays: "
                    + java.util.Arrays.deepToString(arrays));
        }
        int newLength = 0;
        for (T[] arr : arrays) {
            newLength += arr.length;
        }
        Class<?> newType = arrays[0].getClass();
        T[] result = ((Object) newType == (Object) Object[].class)
            ? (T[]) new Object[newLength]
            : (T[]) Array.newInstance(newType.getComponentType(), newLength);
        int destIdx = 0;
        for (T[] arr : arrays) {
            System.arraycopy(arr, 0, result, destIdx, arr.length);
            destIdx += arr.length;
        }
        return result;
    }

    public static final Object [] EMPTY_OBJ_ARRAY = new Object [] {};

    public static final byte [] EMPTY_BYTE_ARRAY = new byte [] {};

    public static final long [] EMPTY_LONG_ARRAY = new long [] {};

    private static final ThreadLocal<SoftReference<byte[]>> BYTES_TMP = new ThreadLocal<>();

    private static final ThreadLocal<SoftReference<char[]>> CHARS_TMP = new ThreadLocal<>();

    /**
     * returns a thread local byte array of at least the size requested.
     * use only for temporary purpose.
     * This method needs to be carefully used!
     * @param size
     * @return
     */
    public static byte [] getBytesTmp(final int size) {
        SoftReference<byte[]> sr = BYTES_TMP.get();
        byte [] result;
        if (sr == null) {
            result = new byte [size];
            BYTES_TMP.set(new SoftReference<>(result));
        } else {
            result = sr.get();
            if (result == null || result.length < size) {
                result = new byte [size];
                BYTES_TMP.set(new SoftReference<>(result));
            }
        }
        return result;
    }

    /**
     * returns a thread local char array of at least the requested size.
     * Use only for temporary purpose.
     * @param size
     * @return
     */

    public static char [] getCharsTmp(final int size) {
        SoftReference<char[]> sr = CHARS_TMP.get();
        char [] result;
        if (sr == null) {
            result = new char [size];
            CHARS_TMP.set(new SoftReference<>(result));
        } else {
            result = sr.get();
            if (result == null || result.length < size) {
                result = new char [size];
                CHARS_TMP.set(new SoftReference<>(result));
            }
        }
        return result;
    }



}
