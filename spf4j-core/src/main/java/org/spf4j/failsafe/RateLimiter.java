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
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongBinaryOperator;
import java.util.function.LongSupplier;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;
import javax.annotation.Signed;
import javax.annotation.concurrent.GuardedBy;
import org.spf4j.base.ExecutionContexts;
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

  private final long permitsPerReplenishInterval;

  private final long permitReplenishIntervalNanos;

  private final LongSupplier nanoTimeSupplier;

  @GuardedBy("sync")
  private long lastReplenishmentNanos;

  private final Object sync;

  private final int maxBackoffNanos;

  private final LongBinaryOperator accumulate;

  public RateLimiter(final long permitsPerReplenishInterval,
          final Duration replenishmentInterval,
          final long maxBurstSize, final long minReplenishInterval, final TimeUnit tu) {
    this(permitsPerReplenishInterval, replenishmentInterval, maxBurstSize, minReplenishInterval, tu,
            DefaultScheduler.INSTANCE, TimeSource.nanoTimeSupplier());
  }

  public RateLimiter(final long permitsPerReplenishInterval,
          final Duration replenishmentInterval,
          final long maxBurstSize) {
    this(permitsPerReplenishInterval, replenishmentInterval, maxBurstSize, DefaultScheduler.INSTANCE);
  }

  public RateLimiter(final long permitsPerReplenishInterval,
          final Duration replenishmentInterval,
          final long maxBurstSize,
          final ScheduledExecutorService scheduler) {
    this(permitsPerReplenishInterval, replenishmentInterval, maxBurstSize, scheduler, TimeSource.nanoTimeSupplier());
  }

  public RateLimiter(final long permitsPerReplenishInterval,
          final Duration replenishmentInterval,
          final long maxBurstSize,
          final ScheduledExecutorService scheduler,
          final LongSupplier nanoTimeSupplier) {
    this(permitsPerReplenishInterval, replenishmentInterval, maxBurstSize, DEFAULT_MIN_REPLENISH_INTERVAL_MS,
            TimeUnit.MILLISECONDS, scheduler, nanoTimeSupplier);
  }

  public RateLimiter(final long permitsPerReplenishInterval,
          final Duration replenishmentInterval,
          final long maxBurstSize,
          final long minReplenishInterval,
          final TimeUnit tu,
          final ScheduledExecutorService scheduler,
          final LongSupplier nanoTimeSupplier) {
    this(permitsPerReplenishInterval, replenishmentInterval, maxBurstSize, minReplenishInterval, tu,
            scheduler, nanoTimeSupplier, Atomics.MAX_BACKOFF_NANOS);
  }

  /**
   * create the rate limiter.
   *
   * @param maxAvailablePermits the maximum number of permits that can accumulate.
   * @param minReplenishInterval the minimum replenish interval, since a scheduler replenishes the tokens asynchronously
   * there is a limit that is relative to the system clock resolution.
   * @param tu the time unit of the minimum replenish interval.
   * @param scheduler the scheduler to use to replenish the bucket.
   * @param nanoTimeSupplier the time supplier.
   * @param maxBackoffNanos expected concurrency level
   */
  public RateLimiter(final long permitsPerReplenishInterval,
          final Duration replenishmentInterval,
          final long maxAvailablePermits,
          final long minReplenishInterval,
          final TimeUnit tu,
          final ScheduledExecutorService scheduler,
          final LongSupplier nanoTimeSupplier,
          final int maxBackoffNanos) {
    this(permitsPerReplenishInterval, replenishmentInterval, 0, maxAvailablePermits, minReplenishInterval, tu,
            scheduler, nanoTimeSupplier, maxBackoffNanos);
  }

  /**
   * create the rate limiter.
   *
   * @param maxAvailablePermits the maximum number of permits that can accumulate.
   * @param minReplenishInterval the minimum replenish interval, since a scheduler replenishes the tokens asynchronously
   * there is a limit that is relative to the system clock resolution.
   * @param tu the time unit of the minimum replenish interval.
   * @param scheduler the scheduler to use to replenish the bucket.
   * @param nanoTimeSupplier the time supplier.
   * @param maxBackoffNanos expected concurrency level
   */
  public RateLimiter(final long permitsPerReplenishInterval,
          final Duration replenishmentInterval,
          final long initialNrOfPermits,
          final long maxAvailablePermits,
          final long minReplenishInterval,
          final TimeUnit tu,
          final ScheduledExecutorService scheduler,
          final LongSupplier nanoTimeSupplier,
          final int maxBackoffNanos) {
    this.maxBackoffNanos = maxBackoffNanos;
    this.sync = new Object();
    this.nanoTimeSupplier = nanoTimeSupplier;
    this.permitReplenishIntervalNanos = replenishmentInterval.toNanos();
    this.permitsPerReplenishInterval = permitsPerReplenishInterval;
    assert permitsPerReplenishInterval >= 1;
    if (maxAvailablePermits < permitsPerReplenishInterval) {
      throw new IllegalArgumentException("Invalid max burst size: " + maxAvailablePermits
              + ",  increase maxBurstSize to something larger than " + permitsPerReplenishInterval
              + " we assume a clock resolution of " + permitReplenishIntervalNanos
              + " and that is the minimum replenish interval");
    }
    this.permits = new AtomicLong(initialNrOfPermits);
    lastReplenishmentNanos = nanoTimeSupplier.getAsLong();
    accumulate = (long left, long right) -> {
      long result = left + right;
      return (result > maxAvailablePermits) ? maxAvailablePermits : result;
    };
    this.replenisher = scheduler.scheduleAtFixedRate(() -> {
      synchronized (sync) {
        Atomics.accumulate(permits, permitsPerReplenishInterval, accumulate, maxBackoffNanos);
        lastReplenishmentNanos = nanoTimeSupplier.getAsLong();
      }
    }, permitReplenishIntervalNanos, permitReplenishIntervalNanos, TimeUnit.NANOSECONDS);
  }

  @Override
  public boolean addPermits(final int nrPermits) {
    return Atomics.maybeAccumulate(permits, nrPermits, accumulate, maxBackoffNanos);
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
    return Atomics.maybeAccumulate(permits, nrPermits, (long prev, long x) -> {
      long dif = prev - x;
      // right will be -1d.
      return (dif < 0) ? prev : dif;
    }, maxBackoffNanos);
  }

  private final class ReservationHandler implements LongBinaryOperator {

    private long currTimeNanos;

    private final long deadlineNanos;

    private long nsUntilResourcesAvailable;

    private int reTimeCount;

    ReservationHandler(final long currTimeNanos, final long deadlineNanos) {
      this.currTimeNanos = currTimeNanos;
      this.deadlineNanos = deadlineNanos;
      this.nsUntilResourcesAvailable = -1;
      this.reTimeCount = RE_READ_TIME_AFTER_RETRIES;
    }

    @Override
    public long applyAsLong(final long prev, final long nrPermits) {
      long remaimingPermits = prev - nrPermits;
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
      long timeoutNs = deadlineNanos - currTimeNanos;
      if (timeoutNs <= 0) {
        return prev;
      }
      long nsUntilNextReplenishment
              = permitReplenishIntervalNanos - (currTimeNanos - lastReplenishmentNanos);
      if (timeoutNs < nsUntilNextReplenishment) {
          return prev; // not enough time to wait.
      }
      double permitsNeeded = -remaimingPermits;
      if (permitsNeeded <= permitsPerReplenishInterval) {
        nsUntilResourcesAvailable = nsUntilNextReplenishment;
        return remaimingPermits;
      } else {
        int numberOfReplenishMentsNeed = (int) Math.ceil(permitsNeeded / permitsPerReplenishInterval);
        long nsNeeded = nsUntilNextReplenishment
                + (numberOfReplenishMentsNeed - 1) * permitReplenishIntervalNanos;
        if (nsNeeded <= timeoutNs) {
          nsUntilResourcesAvailable = nsNeeded;
          return remaimingPermits;
        } else {
          return prev;
        }
      }
    }

    public long getNsUntilResourcesAvailable() {
      return nsUntilResourcesAvailable;
    }

    @Override
    public String toString() {
      return "ReservationHandler{" + "deadlineNanos=" + deadlineNanos + ", permits=" + permits
              + ", nsUntilResourcesAvailable=" + nsUntilResourcesAvailable + '}';
    }
  }

  public long getNrPermits() {
    return permits.get();
  }

  @Override
  @SuppressFBWarnings("MDM_THREAD_YIELD") //fb has a point here...
  public boolean tryAcquire(final int nrPermits, final long deadlineNanos) throws InterruptedException {
    long tryAcquireGetDelayNanos = tryAcquireGetDelayNanos(nrPermits, deadlineNanos);
    if (tryAcquireGetDelayNanos == 0) {
      return true;
    } else if (tryAcquireGetDelayNanos > 0) {
      TimeUnit.NANOSECONDS.sleep(tryAcquireGetDelayNanos);
      return true;
    } else {
      return false;
    }
  }


  @CheckReturnValue
  @Override
  public boolean tryAcquire(@Nonnegative final int nrPermits, @Nonnegative final long timeout, final TimeUnit unit)
          throws InterruptedException {
    if (timeout < 0) {
      throw new IllegalArgumentException("incalid timeout " + timeout + ' ' + unit);
    }
    boolean tryAcquire = tryAcquire(nrPermits);
    if (tryAcquire) {
      return true;
    }
    if (timeout == 0) {
      return false;
    }
    long tryAcquireGetDelayNanos = forceReserve(ExecutionContexts.computeDeadline(timeout, unit), nrPermits);
    if (tryAcquireGetDelayNanos == 0) {
      return true;
    } else if (tryAcquireGetDelayNanos > 0) {
      TimeUnit.NANOSECONDS.sleep(tryAcquireGetDelayNanos);
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
  long tryAcquireGetDelayNanos(final int nrPermits, final long deadlineNanos) {
    boolean tryAcquire = tryAcquire(nrPermits);
    if (tryAcquire) {
      return 0L;
    } else {
      return forceReserve(deadlineNanos, nrPermits);
    }
  }

  private long forceReserve(final long deadlineNanos, final int nrPermits) {
    // no curent permits available, reserve the slots and get the wait time
    if (replenisher.isCancelled()) {
      throw new IllegalStateException("RateLimiter is closed: " + this);
    }
    ReservationHandler rh = new ReservationHandler(nanoTimeSupplier.getAsLong(), deadlineNanos);
    boolean accd;
    synchronized (sync) {
      accd = Atomics.maybeAccumulate(permits, nrPermits, rh, maxBackoffNanos);
    }
    if (accd) {
      return rh.getNsUntilResourcesAvailable();
    } else {
      return -1L;
    }
  }

  @Override
  public void close() {
    replenisher.cancel(false);
  }

  public long getPermitsPerReplenishInterval() {
    return permitsPerReplenishInterval;
  }

  public long getPermitReplenishIntervalNanos() {
    return permitReplenishIntervalNanos;
  }

  public long getLastReplenishmentNanos() {
    synchronized (sync) {
      return lastReplenishmentNanos;
    }
  }

  @Override
  public String toString() {
    return "RateLimiter{" + "permits=" + permits.get()
            + ", replenisher=" + replenisher + ", permitsPerReplenishInterval="
            + permitsPerReplenishInterval + ", permitReplenishIntervalNanos=" + permitReplenishIntervalNanos + '}';
  }

}
