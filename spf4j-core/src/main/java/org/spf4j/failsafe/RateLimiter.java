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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.spf4j.concurrent.Atomics;
import org.spf4j.concurrent.DefaultScheduler;

/**
 * Token bucket implemented rate limiter.
 *
 * @author Zoltan Farkas
 */
@Beta
public final class RateLimiter implements Closeable {

  private final AtomicDouble permits;

  private final RejectedExecutionHandler rejectHandler;

  private final ScheduledFuture<?> replenisher;

  public interface RejectedExecutionHandler {

    <T> T reject(Callable<T> callable);
  }

  public RateLimiter(final long tokenRefreshIntervalMillis, final int maxReqPerSecond,
          final int maxBurstSize) {
    this(tokenRefreshIntervalMillis, maxReqPerSecond, maxBurstSize, new RejectedExecutionHandler() {
      @Override
      public <T> T reject(final Callable<T> callable) {
       throw new RejectedExecutionException("No buckets available for " + callable);
      }
    });
  }

  public RateLimiter(final int maxReqPerSecond,
          final int maxBurstSize,
          final RejectedExecutionHandler rejectionHandler) {
    this(10, maxReqPerSecond, maxBurstSize, rejectionHandler);
  }

  public RateLimiter(final long tokenRefreshIntervalMillis, final double maxReqPerSecond,
          final int maxBurstSize,
          final RejectedExecutionHandler rejectionHandler) {
    this.rejectHandler = rejectionHandler;
    double reqPerRefreshInterval = maxReqPerSecond * tokenRefreshIntervalMillis / 1000;
    if (maxBurstSize < reqPerRefreshInterval) {
      throw new IllegalArgumentException("Invalid paramters " + tokenRefreshIntervalMillis
              + ", " + maxReqPerSecond + ", " + maxBurstSize + " lower tokenRefreshIntervalMillis,"
                      + "or increase maxBurstSize");
    }
    this.permits = new AtomicDouble(reqPerRefreshInterval);
    this.replenisher = DefaultScheduler.INSTANCE.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        Atomics.getAndAccumulate(permits, reqPerRefreshInterval, (left, right) -> {
          double result = left + right;
          return (result > maxBurstSize) ? maxBurstSize : result;
        });
      }
    }, tokenRefreshIntervalMillis, tokenRefreshIntervalMillis, TimeUnit.MILLISECONDS);
  }

  public <T> T execute(final Callable<T> callable) throws Exception {
    if (replenisher.isCancelled()) {
      throw new RejectedExecutionException("RateLimiter is closed, cannot execute " + callable);
    }
    double nrbAvail = Atomics.getAndAccumulate(permits, -1, (left, right) -> {
      double result = left + right;
      return (result < 0) ? 0 : result;
    });
    if (nrbAvail > 1) {
      return callable.call();
    } else {
      return rejectHandler.reject(callable);
    }
  }

  @Override
  public void close() throws IOException {
    replenisher.cancel(false);
  }

  @Override
  public String toString() {
    return "RateLimiter{" + "permits=" + permits + ", rejectHandler="
            + rejectHandler + ", replenisher=" + replenisher + '}';
  }

}
