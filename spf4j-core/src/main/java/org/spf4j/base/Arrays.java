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

/**
 * Array utilities.
 * @author zoly
 */
public final class Arrays {
    private Arrays() { }

    public static double[] getColumnAsDoubles(final long [][] data, final int columnNumber) {
        double [] result = new double [data.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = data [i][columnNumber];
        }
        return result;
    }


    public static double[] getColumn(final double [][] data, final int columnNumber) {
        double [] result = new double [data.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = data[i][columnNumber];
        }
        return result;
    }

    public static double[] toDoubleArray(final long [] larr) {
        double [] result = new double[larr.length];
        for (int i = 0; i < larr.length; i++) {
            result[i] = larr[i];
        }
        return result;
    }

    public static double [] divide(final double [] arr1, final double [] arr2) {
        double [] result = new double [arr1.length];
        for (int i = 0; i < result.length; i++) {
            result [i] = arr1[i] / arr2[i];
        }
        return result;
    }

}
