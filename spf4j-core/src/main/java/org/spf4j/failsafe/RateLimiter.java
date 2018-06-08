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
package org.spf4j.failsafe;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.AtomicDouble;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleUnaryOperator;
import org.spf4j.base.TimeSource;
import org.spf4j.concurrent.Atomics;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.concurrent.PermitSupplier;

/**
 * Token bucket algorithm base call rate limiter. see https://en.wikipedia.org/wiki/Token_bucket for more detail. Unlike
 * the Guava implementation this limiter the token replenishment is done in a separate thread.
 * As such permit acquisition methods are lower overhead,
 * at the cost of increasing the cost of RateLimiter object instance. (a scheduled runnable)
 *
 * @author Zoltan Farkas
 */
@Beta
public final class RateLimiter
        implements AutoCloseable, PermitSupplier {

  private final AtomicDouble permits;

  private final ScheduledFuture<?> replenisher;

  private final double permitsPerReplenishInterval;

  private final long permitReplenishIntervalMillis;

  private volatile long lastReplenishmentNanos;

  public RateLimiter(final double maxReqPerSecond,
          final int maxBurstSize) {
    this(maxReqPerSecond, maxBurstSize, DefaultScheduler.INSTANCE);
  }

  public RateLimiter(final double maxReqPerSecond,
          final int maxBurstSize,
          final ScheduledExecutorService scheduler) {
    double msPerReq = 1000d / maxReqPerSecond;
    if (msPerReq < 10) {
      msPerReq = 10d;
    }
    this.permitReplenishIntervalMillis = (long) msPerReq;
    this.permitsPerReplenishInterval = maxReqPerSecond * msPerReq / 1000;
    assert permitsPerReplenishInterval >= 1;
    if (maxBurstSize < permitsPerReplenishInterval) {
      throw new IllegalArgumentException("Invalid max burst size: " + maxBurstSize
              + ",  increase maxBurstSize to something larger than " + permitsPerReplenishInterval
              + " we assume a clock resolution of " + permitReplenishIntervalMillis
              + " and that is the minimum replenish interval");
    }
    this.permits = new AtomicDouble(permitsPerReplenishInterval);
    lastReplenishmentNanos = TimeSource.nanoTime();
    this.replenisher = scheduler.scheduleAtFixedRate(() -> {
      Atomics.accumulate(permits, permitsPerReplenishInterval, (left, right) -> {
        double result = left + right;
        return (result > maxBurstSize) ? maxBurstSize : result;
      });
      lastReplenishmentNanos = TimeSource.nanoTime();
    }, permitReplenishIntervalMillis, permitReplenishIntervalMillis, TimeUnit.MILLISECONDS);
  }

  /**
   * invocation will consume a exec permit if available.
   *
   * @return true if permit acquired. false otherwise.
   */
  public boolean tryAcquire() {
    return tryAcquire(1);
  }

  public boolean tryAcquire(final int nrPermits) {
    if (replenisher.isCancelled()) {
      throw new IllegalStateException("RateLimiter is closed: " + this);
    }
    return Atomics.maybeAccumulate(permits, nrPermits, (prev, x) -> {
      double dif = prev - x;
      // right will be -1d.
      return (dif < 0) ? prev : dif;
    });
  }

  private final class ReservationHandler implements DoubleUnaryOperator {

    private final long deadlineNanos;

    private final int permits;

    private long msUntilResourcesAvailable;

    ReservationHandler(final long deadlineNanos, final int permits) {
      this.deadlineNanos = deadlineNanos;
      this.permits = permits;
      this.msUntilResourcesAvailable = -1;
    }

    @Override
    public double applyAsDouble(final double prev) {
      double permitsNeeded = permits - prev;
      if (permitsNeeded <= 0) {
        return -permitsNeeded;
      }
      long lut = lastReplenishmentNanos;
      long currTime = TimeSource.nanoTime();
      long timeoutMs = TimeUnit.NANOSECONDS.toMillis(deadlineNanos - currTime);
      if (timeoutMs <= 0) {
        return prev;
      }
      long msUntilNextReplenishment
              = permitReplenishIntervalMillis - TimeUnit.NANOSECONDS.toMillis(currTime - lut);
      int numberOfReplenishMentsNeed = (int) Math.ceil(permitsNeeded / permitsPerReplenishInterval);
      if (numberOfReplenishMentsNeed <= 1) {
        if (timeoutMs < msUntilNextReplenishment) {
          return prev;
        } else {
          msUntilResourcesAvailable = msUntilNextReplenishment;
          return prev - permits;
        }
      } else {
        long msNeeded = msUntilNextReplenishment
                + (numberOfReplenishMentsNeed - 1) * permitReplenishIntervalMillis;
        if (msNeeded <= timeoutMs) {
          msUntilResourcesAvailable = msNeeded;
          return prev - permits;
        } else {
          return prev;
        }
      }
    }

    public long getMsUntilResourcesAvailable() {
      return msUntilResourcesAvailable;
    }

    @Override
    public String toString() {
      return "ReservationHandler{" + "deadlineNanos=" + deadlineNanos + ", permits=" + permits
              + ", msUntilResourcesAvailable=" + msUntilResourcesAvailable + '}';
    }
  }

  public double getNrPermits() {
    return permits.get();
  }

  @Override
  @SuppressFBWarnings("MDM_THREAD_YIELD") //fb has a point here...
  public boolean tryAcquire(final int nrPermits, final long timeout, final TimeUnit unit) throws InterruptedException {
    boolean tryAcquire = tryAcquire(nrPermits);
    if (tryAcquire) {
      return true;
    } else { // no curent permits available, reserve some slots and wait for them.
      ReservationHandler rh = new ReservationHandler(TimeSource.nanoTime() + unit.toNanos(timeout), nrPermits);
      boolean accd = Atomics.maybeAccumulate(permits, rh);
      if (accd) {
        Thread.sleep(rh.getMsUntilResourcesAvailable());
        return true;
      } else {
        return false;
      }
    }
  }


  @Override
  public void close() {
    replenisher.cancel(false);
  }

  public double getPermitsPerReplenishInterval() {
    return permitsPerReplenishInterval;
  }

  public long getPermitReplenishIntervalMillis() {
    return permitReplenishIntervalMillis;
  }

  public long getLastReplenishmentNanos() {
    return lastReplenishmentNanos;
  }

  @Override
  public String toString() {
    return "RateLimiter{" + "permits=" + permits
            + ", replenisher=" + replenisher + ", permitsPerReplenishInterval="
            + permitsPerReplenishInterval + ", permitReplenishIntervalMillis=" + permitReplenishIntervalMillis + '}';
  }

}
