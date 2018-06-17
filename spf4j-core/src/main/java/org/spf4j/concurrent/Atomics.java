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
package org.spf4j.concurrent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.UnaryOperator;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
@SuppressFBWarnings("PREDICTABLE_RANDOM")
public final class Atomics {

  public static final int MAX_BACKOFF_NANOS = Integer.getInteger("spf4j.atomics.maxBackoffNanos", 3);

  private Atomics() {
  }

  public static <T> UpdateResult<T> update(final AtomicReference<T> ar, final UnaryOperator<T> function) {
    T initial;
    T newObj;
    do {
      initial = ar.get();
      newObj = function.apply(initial);
      if (Objects.equals(initial, newObj)) {
        return UpdateResult.same(initial);
      }
    } while (!ar.compareAndSet(initial, newObj));
    return UpdateResult.updated(newObj);
  }

  public static boolean maybeAccumulate(final AtomicLong dval, final double x,
          final DoubleBinaryOperator accumulatorFunction, final int maxBackoffNanos) {
    long prev, next;
    do {
      prev = dval.get();
      next = Double.doubleToRawLongBits(accumulatorFunction.applyAsDouble(Double.longBitsToDouble(prev), x));
      if (prev == next) {
        return false;
      }
      if (dval.compareAndSet(prev, next)) {
        return true;
      }
      LockSupport.parkNanos(getBackoffNanos(maxBackoffNanos)); // backoff
    } while (true);
  }

  public static void accumulate(final AtomicLong dval, final double x,
          final DoubleBinaryOperator accumulatorFunction, final int maxBackoffNanos) {
    long prev, next;
    do {
      prev = dval.get();
      next = Double.doubleToRawLongBits(accumulatorFunction.applyAsDouble(Double.longBitsToDouble(prev), x));
      if (next == prev) {
        return;
      }
      if (dval.compareAndSet(prev, next)) {
        return;
      }
      LockSupport.parkNanos(getBackoffNanos(maxBackoffNanos)); // backoff
    } while (true);
  }

  private  static long getBackoffNanos(final int maxBackoffNanos) {
    return maxBackoffNanos > 0 ? Thread.currentThread().getId() % maxBackoffNanos : 0;
  }

  public static boolean maybeAccumulate(final AtomicLong dval,
          final DoubleUnaryOperator accumulatorFunction, final int maxBackoffNanos) {
    long prev, next;
    do {
      prev = dval.get();
      next = Double.doubleToRawLongBits(accumulatorFunction.applyAsDouble(Double.longBitsToDouble(prev)));
      if (prev == next) {
        return false;
      }
      if (dval.compareAndSet(prev, next)) {
        return true;
      }
      LockSupport.parkNanos(getBackoffNanos(maxBackoffNanos)); // backoff
    } while (true);
  }



}
