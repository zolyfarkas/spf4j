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

import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @author zoly
 */
public final class IntMath {

    private IntMath() { }

    public static int closestPowerOf2(final int number) {
        return number == 0 ? 0 : 32 - Integer.numberOfLeadingZeros(number - 1);
    }

    public static int closestPowerOf2Number(final int number) {
       return number == 0 ? 0 : 1 << (32 - Integer.numberOfLeadingZeros(number - 1));
    }

    /**
     * A very fast Pseudo random generator.
     * use of this random is appropriate when you need the fastest random that you plan to use in a single
     * thread.
     * If you need a thread-safe random, please use JDK ThreadLocalRandom, which will be your best option.
     */
    @NotThreadSafe
    public static final class XorShift32 implements IntSequence {
        // XorShift128 PRNG with a 2^32-1 period.
        private int x;

        public XorShift32() {
          x = (int) System.currentTimeMillis();
          while (x == 0) {
            x = (int) System.currentTimeMillis();
          }
        }

        @Override
        public int nextInt() {
            x ^= (x << 6);
            x ^= (x >>> 21);
            return x ^ (x << 7);
        }
    }

    /**
     * @deprecated please use JDK java.util.concurrent.ThreadLocalRandom instead.
     * The JDK implementation uses local fields in the Thread class instead of a classic ThreadLocal,
     * which makes it faster...
     */
    @ThreadSafe
    @Deprecated
    public static final class XorShift32ThreadSafe implements IntSequence {

      private final ThreadLocal<XorShift32> rnd = new ThreadLocalRandom();

      public static final class Singleton {

        public static final XorShift32ThreadSafe INSTANCE = new XorShift32ThreadSafe();

      }

      @Override
      public int nextInt() {
        return rnd.get().nextInt();
      }

      private static class ThreadLocalRandom extends ThreadLocal<XorShift32> {

        @Override
        protected XorShift32 initialValue() {
          return new XorShift32();
        }
      }
    }


    @NotThreadSafe
    public static final class XorShift128 implements IntSequence {
        // XorShift128 PRNG with a 2^128-1 period.
        private int x = System.identityHashCode(this);
        private int y = -938745813;
        private int z = 452465366;
        private int w = 1343246171;

        @Override
        public int nextInt() {
            int t = x ^ (x << 15);
            x = y; y = z; z = w;
            w = (w ^ (w >>> 21)) ^ (t ^ (t >>> 4));
            return w;
        }
    }


}
