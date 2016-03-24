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

import javax.annotation.Nonnull;

/**
 *
 * @author zoly
 */
public final class Comparables {

    private Comparables() {
    }

    public static <T> Comparable<T> min(@Nonnull final Object ... nrs) {
        if (nrs.length == 0) {
            throw new IllegalArgumentException("cannot calc min of empty array" + java.util.Arrays.toString(nrs));
        }
        Comparable min = (Comparable) nrs[0];
        for (int i = 1; i < nrs.length; i++) {
            Comparable c = (Comparable) nrs[i];
            if (c.compareTo((T) min) < 0) {
                min  = c;
            }
        }
        return min;
    }
    
    public static <T> Comparable<T> max(@Nonnull final Object ... nrs) {
        if (nrs.length == 0) {
            throw new IllegalArgumentException("cannot calc max of empty array: " + java.util.Arrays.toString(nrs));
        }
        Comparable max = (Comparable) nrs[0];
        for (int i = 1; i < nrs.length; i++) {
            Comparable c = (Comparable) nrs[i];
            if (c.compareTo((T) max) > 0) {
                max  = c;
            }
        }
        return max;
    }
    
    public static <T extends Comparable<T>> int compare(final T first, final T second) {
        if (first == null) {
            return (second == null) ? 0 : 1;
        } else {
            return (second == null) ? -1 : first.compareTo(second);
        }
    }
    
    
    public static <T extends Comparable<T>> int compareArrays(final T[] first, final T[] second) {
        int result = 0;
        if (first == second) {
            return result;
        }
        int i = 0;
        int l1 = first.length;
        int l2 = second.length;
        int n = Math.min(l1, l2);
        do {
            result = Comparables.compare(first[i], second[i]);
            i++;
        } while (result == 0 && i < n);
        if (result == 0 && l1 != l2) {
            return (l1 < l2) ? -1 : 1;
        }
        return result;
    }
    
    
}
