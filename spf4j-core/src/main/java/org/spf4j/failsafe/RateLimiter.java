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
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.spf4j.base.Callables;
import org.spf4j.base.UncheckedExecutionException;
import org.spf4j.concurrent.Atomics;
import org.spf4j.concurrent.DefaultScheduler;

/**
 * Token bucket implemented rate limiter.
 *
 * @author Zoltan Farkas
 */
@Beta
public final class RateLimiter<T, C extends Callable<? extends T>> implements Closeable, Executor {

  private final AtomicDouble permits;

  private final RejectedExecutionHandler rejectHandler;

  private final ScheduledFuture<?> replenisher;

  private final double permitsPerReplenishInterval;

  private final long permitReplenishIntervalMillis;

  @FunctionalInterface
  public interface RejectedExecutionHandler<T, C extends Callable<? extends T>> {

     T reject(RateLimiter limiter, C callable, long msAfterWhichResourceAvailable) throws Exception;
  }

  public RateLimiter(final double maxReqPerSecond,
          final int maxBurstSize) {
    this(maxReqPerSecond, maxBurstSize, new RejectedExecutionHandler<T, C>() {
      @Override
      public T reject(final RateLimiter limiter, final C callable,
              final long msAfterWhichResourceAvailable) {
       throw new RejectedExecutionException("No buckets available for " + callable + ", resources avail after "
               + msAfterWhichResourceAvailable + "ms");
      }
    });
  }

    public RateLimiter(final double maxReqPerSecond,
          final int maxBurstSize,
          final RejectedExecutionHandler rejectionHandler) {
      this(maxReqPerSecond, maxBurstSize, rejectionHandler, DefaultScheduler.INSTANCE);
    }


  public RateLimiter(final double maxReqPerSecond,
          final int maxBurstSize,
          final RejectedExecutionHandler rejectionHandler,
          final ScheduledExecutorService scheduler) {
    this.rejectHandler = rejectionHandler;
    double msPerReq =  1000d / maxReqPerSecond;
    if (msPerReq < 10) {
      msPerReq = 10d;
    }
    this.permitReplenishIntervalMillis = (long) msPerReq;
    this.permitsPerReplenishInterval = maxReqPerSecond * msPerReq / 1000;
    if (maxBurstSize < permitsPerReplenishInterval) {
      throw new IllegalArgumentException("Invalid max burst size: " + maxBurstSize
              + ",  increase maxBurstSize to something larger than " + permitsPerReplenishInterval
              + " we assume a clock resolution of " + permitReplenishIntervalMillis
              + " and that is the minimum replenish inteval");
    }
    this.permits = new AtomicDouble(permitsPerReplenishInterval);
    this.replenisher = scheduler.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        Atomics.getAndAccumulate(permits, permitsPerReplenishInterval, (left, right) -> {
          double result = left + right;
          return (result > maxBurstSize) ? maxBurstSize : result;
        });
      }
    }, permitReplenishIntervalMillis, permitReplenishIntervalMillis, TimeUnit.MILLISECONDS);
  }

  public <T> T execute(final C callable) throws Exception {
    if (replenisher.isCancelled()) {
      throw new IllegalStateException("RateLimiter is closed, cannot execute " + callable);
    }
    double nrbAvail = Atomics.getAndAccumulate(permits, -1, (left, right) -> {
      double result = left + right;
      return (result < 0) ? 0 : result;
    });
    if (nrbAvail >= 1.0) {
      return (T) callable.call();
    } else {
      double dtrm = (double) permitReplenishIntervalMillis;
      double msFor1Permit = dtrm / permitsPerReplenishInterval;
      if (msFor1Permit < dtrm) {
        msFor1Permit = dtrm;
      }
      return (T) rejectHandler.reject(this, callable, (long) msFor1Permit);
    }
  }

  @Override
  public void execute(final Runnable command) {
    try {
      execute((C) Callables.from(command));
    } catch (Exception ex) {
      throw new UncheckedExecutionException(ex);
    }
  }

  public Callable<T> toLimitedCallable(final C callable) {
    return () -> this.execute(callable);
  }

  @Override
  public void close() throws IOException {
    replenisher.cancel(false);
  }

  public double getPermitsPerReplenishInterval() {
    return permitsPerReplenishInterval;
  }

  public long getPermitReplenishIntervalMillis() {
    return permitReplenishIntervalMillis;
  }

  @Override
  public String toString() {
    return "RateLimiter{" + "permits=" + permits + ", rejectHandler="
            + rejectHandler + ", replenisher=" + replenisher + '}';
  }

}
