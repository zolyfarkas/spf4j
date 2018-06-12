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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleUnaryOperator;
import java.util.function.LongSupplier;
import javax.annotation.Signed;
import javax.annotation.concurrent.GuardedBy;
import org.spf4j.base.TimeSource;
import org.spf4j.concurrent.Atomics;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.concurrent.PermitSupplier;

/**
 * Token bucket algorithm base call rate limiter. see https://en.wikipedia.org/wiki/Token_bucket for more detail.
 * Unlike the Guava implementation, the token replenishment is done in a separate thread.
 * As such permit acquisition methods have lower overhead
 * (no System.nanotime invocation is made when permits available),
 * This lower overhead comes at the cost of increasing the cost of RateLimiter object instance. (a scheduled runnable)
 * This implementation also uses CAS to update the permit bucket which should yield better concurrency characteristics.
 * there is a backoff to handle high contention better as well
 * (see https://dzone.com/articles/wanna-get-faster-wait-bit)
 *
 * The different in performance can be observed in a benchmark where we try to limit 10000000 ops/s:
 *
 * Benchmark                                                                 Mode  Cnt        Score        Error  Units
 * com.google.common.util.concurrent.GuavaRateLimiterBenchmark.acquire      thrpt   10  8197739.576 ± 163437.214  ops/s
 * org.spf4j.failsafe.Spf4jRateLimiterBenchmark.acquire                     thrpt   10  9791489.540 ±  89480.010  ops/s
 *
 * The Guava implementation cannot really get as close to the desired rate as the spf4j implementation.
 *
 * The spf4j implementation tops of at about:
 *
 * Benchmark                               Mode  Cnt         Score        Error  Units
 * Spf4jRateLimiterBenchmark.acquire      thrpt   10  15270385.653 ± 507217.444  ops/s
 *
 * these benchmarks are artificial and really measure the overhead of permit acquisition alone without doing anything
 * else, real use cases will accompany a permit acquision with some work which impacts the CAS/lock acquisition perf...
 *
 * Rate Limiter also implements the PermitSupplier abstraction along with GuavaRateLimiter allowing interchangeability
 * between the 2 implementations based on what trade-of work better for you.
 * PermitSupplier allows interchangeability and combination with Semaphore (extends PermitSupplier) implementations.
 *
 * @author Zoltan Farkas
 */
@Beta
public final class RateLimiter
        implements AutoCloseable, PermitSupplier {

  private static final int RE_READ_TIME_AFTER_RETRIES = Integer.getInteger("spf4j.rateLimiter.reReadTimeAfterTries", 5);

  private static final long DEFAULT_MIN_REPLENISH_INTERVAL_MS
          = Long.getLong("spf4j.rateLimiter.defaultMinReplenishIntervalMs", 10L);

  private final AtomicLong permits;

  private final ScheduledFuture<?> replenisher;

  private final double permitsPerReplenishInterval;

  private final long permitReplenishIntervalMillis;

  private final LongSupplier nanoTimeSupplier;

  @GuardedBy("sync")
  private long lastReplenishmentNanos;

  private final Object sync;

  private final int concurrencyLevel;

  public RateLimiter(final double maxReqPerSecond,
          final int maxBurstSize, final long minReplenishIntervalMillis) {
    this(maxReqPerSecond, maxBurstSize, minReplenishIntervalMillis,
            DefaultScheduler.INSTANCE, TimeSource.nanoTimeSupplier());
  }

  public RateLimiter(final double maxReqPerSecond,
          final int maxBurstSize) {
    this(maxReqPerSecond, maxBurstSize, DefaultScheduler.INSTANCE);
  }

  public RateLimiter(final double maxReqPerSecond,
          final int maxBurstSize,
          final ScheduledExecutorService scheduler) {
    this(maxReqPerSecond, maxBurstSize, scheduler, TimeSource.nanoTimeSupplier());
  }

  public RateLimiter(final double maxReqPerSecond,
          final int maxBurstSize,
          final ScheduledExecutorService scheduler,
          final LongSupplier nanoTimeSupplier) {
    this(maxReqPerSecond, maxBurstSize, DEFAULT_MIN_REPLENISH_INTERVAL_MS, scheduler, nanoTimeSupplier);
  }

  public RateLimiter(final double maxReqPerSecond,
          final int maxBurstSize,
          final long minReplenishIntervalMillis,
          final ScheduledExecutorService scheduler,
          final LongSupplier nanoTimeSupplier) {
    this(maxReqPerSecond, maxBurstSize, minReplenishIntervalMillis,
            scheduler, nanoTimeSupplier, Atomics.MAX_BACKOFF_NANOS);
  }

  public RateLimiter(final double maxReqPerSecond,
          final int maxBurstSize,
          final long minReplenishIntervalMillis,
          final ScheduledExecutorService scheduler,
          final LongSupplier nanoTimeSupplier, final int concurrencyLevel) {
    this.concurrencyLevel = concurrencyLevel;
    this.sync = new Object();
    this.nanoTimeSupplier = nanoTimeSupplier;
    double msPerReq = 1000d / maxReqPerSecond;
    if (msPerReq < minReplenishIntervalMillis) {
      msPerReq = minReplenishIntervalMillis;
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
    this.permits = new AtomicLong(Double.doubleToRawLongBits(permitsPerReplenishInterval));
    lastReplenishmentNanos = nanoTimeSupplier.getAsLong();
    this.replenisher = scheduler.scheduleAtFixedRate(() -> {
      synchronized (sync) {
        Atomics.accumulate(permits, permitsPerReplenishInterval, (left, right) -> {
          double result = left + right;
          return (result > maxBurstSize) ? maxBurstSize : result;
        }, concurrencyLevel);
        lastReplenishmentNanos = nanoTimeSupplier.getAsLong();
      }
    }, permitReplenishIntervalMillis, permitReplenishIntervalMillis, TimeUnit.MILLISECONDS);
  }

  /**
   * Try to acquire a permit if available.
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
    return Atomics.maybeAccumulate(permits, (double) nrPermits, (prev, x) -> {
      double dif = prev - x;
      // right will be -1d.
      return (dif < 0) ? prev : dif;
    }, concurrencyLevel);
  }

  private final class ReservationHandler implements DoubleUnaryOperator {

    private long currTimeNanos;

    private final long deadlineNanos;

    private final int permits;

    private long msUntilResourcesAvailable;

    private int reTimeCount;

    ReservationHandler(final long currTimeNanos, final long timeoutMillis, final int permits) {
      this.currTimeNanos = currTimeNanos;
      this.deadlineNanos = currTimeNanos + timeoutMillis;
      this.permits = permits;
      this.msUntilResourcesAvailable = -1;
      this.reTimeCount = RE_READ_TIME_AFTER_RETRIES;
    }

    @Override
    public double applyAsDouble(final double prev) {
      double remaimingPermits = prev - permits;
      if (remaimingPermits >= 0) {
        return remaimingPermits;
      }
      if (reTimeCount > 0) {
        reTimeCount--;
      } else {
        // re-reading current time on every try is pointless since System.nanotime can take 30ns to exec...
        reTimeCount = RE_READ_TIME_AFTER_RETRIES;
        currTimeNanos = nanoTimeSupplier.getAsLong();
      }
      long timeoutMs = TimeUnit.NANOSECONDS.toMillis(deadlineNanos - currTimeNanos);
      if (timeoutMs <= 0) {
        return prev;
      }
      long msUntilNextReplenishment
              = permitReplenishIntervalMillis - TimeUnit.NANOSECONDS.toMillis(currTimeNanos - lastReplenishmentNanos);
      if (timeoutMs < msUntilNextReplenishment) {
          return prev; // not enough time to wait.
      }
      double permitsNeeded = -remaimingPermits;
      if (permitsNeeded <= permitsPerReplenishInterval) {
        msUntilResourcesAvailable = msUntilNextReplenishment;
        return remaimingPermits;
      } else {
        int numberOfReplenishMentsNeed = (int) Math.ceil(permitsNeeded / permitsPerReplenishInterval);
        long msNeeded = msUntilNextReplenishment
                + (numberOfReplenishMentsNeed - 1) * permitReplenishIntervalMillis;
        if (msNeeded <= timeoutMs) {
          msUntilResourcesAvailable = msNeeded;
          return remaimingPermits;
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
    return Double.longBitsToDouble(permits.get());
  }

  @Override
  @SuppressFBWarnings("MDM_THREAD_YIELD") //fb has a point here...
  public boolean tryAcquire(final int nrPermits, final long timeout, final TimeUnit unit) throws InterruptedException {
    long tryAcquireGetDelayMillis = tryAcquireGetDelayMillis(nrPermits, timeout, unit);
    if (tryAcquireGetDelayMillis == 0) {
      return true;
    } else if (tryAcquireGetDelayMillis > 0) {
      Thread.sleep(tryAcquireGetDelayMillis);
      return true;
    } else {
      return false;
    }
  }

  /**
   * @param nrPermits nr of permits to acquire
   * @param timeout the maximum time to wait to acquire the permits.
   * @param unit the unit of time to wait.
   * @return negative value if cannot acquire number of permits within time, or if positive it is the amount of ms
   * we can wait to act of the acquired permissions.
   * the user of this method needs to be trusted, since it can violate the contract.
   * @throws InterruptedException
   */
  @Signed
  long tryAcquireGetDelayMillis(final int nrPermits, final long timeout, final TimeUnit unit)
          throws InterruptedException {
    boolean tryAcquire = tryAcquire(nrPermits);
    if (tryAcquire) {
      return 0L;
    } else { // no curent permits available, reserve some slots and wait for them.
      ReservationHandler rh = new ReservationHandler(nanoTimeSupplier.getAsLong(), unit.toNanos(timeout), nrPermits);
      boolean accd;
      synchronized (sync) {
        accd = Atomics.maybeAccumulate(permits, rh, concurrencyLevel);
      }
      if (accd) {
        return rh.getMsUntilResourcesAvailable();
      } else {
        return -1L;
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
    synchronized (sync) {
      return lastReplenishmentNanos;
    }
  }

  @Override
  public String toString() {
    return "RateLimiter{" + "permits=" + Double.longBitsToDouble(permits.get())
            + ", replenisher=" + replenisher + ", permitsPerReplenishInterval="
            + permitsPerReplenishInterval + ", permitReplenishIntervalMillis=" + permitReplenishIntervalMillis + '}';
  }

}
