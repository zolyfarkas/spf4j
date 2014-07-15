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
 *
 * @author zoly
 */
public final class IntMath {

    private IntMath() { }
    
    public static int closestPowerOf2(final int number) {
        return number == 0 ? 0 : 32 - Integer.numberOfLeadingZeros(number - 1);
    }
    
    public static final class XorShift32 {
        // XorShift128 PRNG with a 2^32-1 period.
        private int x = System.identityHashCode(this);

        public int nextInt() {
            x ^= (x << 6);
            x ^= (x >>> 21);
            return x ^ (x << 7);
        }
    }

    public static final class XorShift128 {
        // XorShift128 PRNG with a 2^128-1 period.
        private int x = System.identityHashCode(this);
        private int y = -938745813;
        private int z = 452465366;
        private int w = 1343246171;

        public int nextInt() {
            int t = x ^ (x << 15);
            x = y; y = z; z = w;
            w = (w ^ (w >>> 21)) ^ (t ^ (t >>> 4));
            return w;
        }
    }
    
    
}
