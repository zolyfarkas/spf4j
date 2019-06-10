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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.ThreadSafe;
import org.spf4j.base.ExecutionContexts;

/**
 *
 * @author Zoltan Farkas
 */
@ThreadSafe
@ParametersAreNonnullByDefault
public interface PermitSupplier {

  PermitSupplier UNLIMITED = new PermitSupplier() {
    @Override
    public boolean tryAcquire(final int nrPermits, final long deadlineNanos) {
      return true;
    }
  };

  PermitSupplier EMPTY = new PermitSupplier() {
    @Override
    public boolean tryAcquire(final int nrPermits, final long deadlineNanos) {
      return false;
    }
  };

  public interface Acquisition {

    Acquisition SUCCESS = new Acquisition() {
      @Override
      public boolean isSuccess() {
        return true;
      }

      @Override
      public long permitAvailableEstimateInNanos() {
        return 0L;
      }
    };

    static Acquisition failed(final long nanosUntilAvailable) {
      return new Acquisition() {
        @Override
        public boolean isSuccess() {
          return false;
        }

        @Override
        public long permitAvailableEstimateInNanos() {
          return nanosUntilAvailable;
        }
      };
    }

    /**
     * @return true is permit acquisition request successful. false otherwise.
     */
    boolean isSuccess();

    /**
     * @return if acquisition not successful the number of nanos in which permits will be available. -1L if N/A
     */
    long permitAvailableEstimateInNanos();
  }

  /**
   * Acquire one permit.
   *
   * @param timeout time to wait for permit to become available.
   * @param unit units of time.
   * @throws InterruptedException - operation interrupted.
   * @throws TimeoutException - timed out.
   */
  default void acquire(final long timeout, final TimeUnit unit)
          throws InterruptedException, TimeoutException {
    acquire(1, timeout, unit);
  }

  /**
   * Acquire a arbitrary number of permits.
   *
   * @param nrPermits - numer of permits to acquire.
   * @param timeout - time to wait for permit to become available.
   * @param unit - units of time.
   * @throws InterruptedException - operation interrupted.
   * @throws TimeoutException - timed out.
   */
  default void acquire(final int nrPermits, @Nonnegative final long timeout, final TimeUnit unit)
          throws InterruptedException, TimeoutException {
    if (!tryAcquire(nrPermits, timeout, unit)) {
      throw new TimeoutException("Cannot acquire timeout after " + timeout + " " + unit);
    }
  }

  /**
   * try to acquire a permit.
   *
   * @param timeout time to wait for permit to become available.
   * @param unit units of time.
   * @return true if permit acquired, false if timed out.
   * @throws InterruptedException - operation interrupted.
   */
  @CheckReturnValue
  default boolean tryAcquire(@Nonnegative final long timeout, final TimeUnit unit)
          throws InterruptedException {
    return tryAcquire(1, timeout, unit);
  }

  /**
   * try to acquire a number of permits.
   *
   * @param nrPermits number of permits to acquire.
   * @param timeout time to wait for permits to become available.
   * @param unit units of time.
   * @return true if permits acquired, false if timed out.
   * @throws InterruptedException - operation interrupted.
   */
  @CheckReturnValue
  default boolean tryAcquire(@Nonnegative final int nrPermits, @Nonnegative final long timeout, final TimeUnit unit)
          throws InterruptedException {
    if (timeout < 0) {
      throw new IllegalArgumentException("Invalid timeout " + timeout + ' ' + unit);
    }
    return tryAcquire(nrPermits, ExecutionContexts.computeDeadline(timeout, unit));
  }

  @CheckReturnValue
  boolean tryAcquire(@Nonnegative int nrPermits, long deadlineNanos) throws InterruptedException;


  @CheckReturnValue
  default Acquisition tryAcquireEx(@Nonnegative final int nrPermits,
          @Nonnegative final long timeout, final TimeUnit unit)
          throws InterruptedException {
    if (timeout < 0) {
      throw new IllegalArgumentException("Invalid timeout " + timeout + ' ' + unit);
    }
    return tryAcquireEx(nrPermits, ExecutionContexts.computeDeadline(timeout, unit));
  }

  @CheckReturnValue
  default Acquisition tryAcquireEx(@Nonnegative final int nrPermits, final long deadlineNanos)
          throws InterruptedException {
    boolean  res = tryAcquire(nrPermits, deadlineNanos);
    if (res) {
      return Acquisition.SUCCESS;
    } else {
      return Acquisition.failed(-1L);
    }
  }


  /**
   * @param nrPermits
   * @return true if at least one permit is added.
   */
  default boolean addPermits(final int nrPermits) {
    return false;
  }

  default Semaphore toSemaphore() {
    return new Semaphore() {
      @Override
      public void release(final int nrPermits) {
        // nothing to release.
      }

      @Override
      public boolean tryAcquire(final int nrPermits, final long timeout, final TimeUnit unit)
              throws InterruptedException {
        return PermitSupplier.this.tryAcquire(nrPermits, timeout, unit);
      }

      @Override
      public boolean tryAcquire(final long timeout, final TimeUnit unit) throws InterruptedException {
        return PermitSupplier.this.tryAcquire(timeout, unit);
      }

      @Override
      public void acquire(final int nrPermits, final long timeout, final TimeUnit unit)
              throws InterruptedException, TimeoutException {
        PermitSupplier.this.acquire(nrPermits, timeout, unit);
      }

      @Override
      public void acquire(final long timeout, final TimeUnit unit) throws InterruptedException, TimeoutException {
        PermitSupplier.this.acquire(timeout, unit);
      }

      @Override
      public boolean tryAcquire(final int nrPermits, final long deadlineNanos) throws InterruptedException {
        return PermitSupplier.this.tryAcquire(nrPermits, deadlineNanos);
      }

    };
  }

}
