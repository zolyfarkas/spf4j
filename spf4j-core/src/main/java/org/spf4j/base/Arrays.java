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

import static java.util.Arrays.deepEquals;
import static java.util.Arrays.equals;

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

    public static double[] toDoubleArray(final long[] larr) {
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
        if (a1 == a2) {
            return true;
        }
        if (a1 == null || a2 == null) {
            return false;
        }
        int length = a1.length;
        if (a2.length != length) {
            return false;
        }

        for (int i = starting; i < length; i++) {
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
}
