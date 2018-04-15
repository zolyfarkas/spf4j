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
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import org.spf4j.base.TimeSource;

/**
 * Special purpose queue for a single value Custom designed for the LifoThreadPool
 *
 * @author zoly
 */
final class UnitQueuePU<T> {


  private final AtomicReference<T> value = new AtomicReference<>();

  private final Thread readerThread;

  UnitQueuePU(final Thread readerThread) {
    this.readerThread = readerThread;
  }

  @CheckReturnValue
  @Nullable
  public T poll() {
    return value.getAndSet(null);
  }

  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  @CheckReturnValue
  @Nullable
  @SuppressWarnings("checkstyle:InnerAssignment")
  public T poll(final long timeoutNanos, final long spinCount, final long pcurrTime) throws InterruptedException {
    T result = value.getAndSet(null);
    if (result != null) {
      return result;
    }
    if (spinCount > 0 && org.spf4j.base.Runtime.NR_PROCESSORS > 1) {
      if (SpinLimiter.SPIN_LIMIT.tryAcquire()) {
        try {
          int i = 0;
          while (i < spinCount) {
            result = value.getAndSet(null);
            if (result != null) {
              return result;
            }
            if (i % 100 == 0 && Thread.interrupted()) {
              throw new InterruptedException();
            }
            i++;
          }
        } finally {
          SpinLimiter.SPIN_LIMIT.release();
        }
      }
    }
    if ((result = value.getAndSet(null)) == null) {
      long currTime = spinCount <= 0 ? pcurrTime : TimeSource.nanoTime();
      long deadlineNanos = currTime + timeoutNanos;
      do {
        final long to = deadlineNanos - currTime;
        if (to <= 0) {
          return null;
        }
        LockSupport.parkNanos(to);
        if ((result = value.getAndSet(null)) != null) {
         return result;
        }
        if (Thread.interrupted()) {
          throw new InterruptedException();
        }
        currTime = TimeSource.nanoTime();
      } while (true);
    }
    return result;
  }

  @CheckReturnValue
  public boolean offer(final T offer) {
    boolean result = value.compareAndSet(null, offer);
    if (result) {
      LockSupport.unpark(readerThread);
    }
    return result;
  }

  @Override
  public String toString() {
    return "UnitQueuePU{" + "value=" + value + '}';
  }

}
