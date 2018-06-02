/*
 * Copyright 2018 SPF4J.
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
package org.spf4j.base.intv;

import gnu.trove.list.array.TLongArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * build a Limiter utility to limit access to a set of resources.
 *
 * @author Zoltan Farkas
 */
public final class RateLimiter {

  private final int maxNrRequests;

  private final long intervalNanos;

  private final TLongArrayList list;

  public RateLimiter(int maxNrRequests, long intervalNanos) {
    this.maxNrRequests = maxNrRequests;
    this.intervalNanos = intervalNanos;
    this.list = new TLongArrayList(maxNrRequests);
  }

  public int getMaxNrRequests() {
    return maxNrRequests;
  }

  public long getIntervalNanos() {
    return intervalNanos;
  }

  public synchronized <T> T execute(Callable<T> c) throws Exception {
    long currTime = System.nanoTime();
    long prevTime = currTime - intervalNanos;
    int i = 0;
    int l = list.size();
    while (i < l && (list.get(i) - prevTime < 0)) {
      i++;
    }
    list.remove(0, i);
    if (l - i < maxNrRequests) {
      list.add(currTime);
      return c.call();
    } else {
      throw new RejectedExecutionException();
    }
  }

  @Override
  public String toString() {
    return "RateLimiter{" + "maxNrRequests=" + maxNrRequests
            + ", intervalNanos=" + intervalNanos + ", list=" + list + '}';
  }

  public static final class Source {

    private final int maxNrRequests;

    private final long intervalNanos;

    private final ConcurrentMap<Object, RateLimiter> limiters;

    public Source(int maxNrRequests, long interval, TimeUnit intervalUnit) {
      this.maxNrRequests = maxNrRequests;
      this.intervalNanos = intervalUnit.toNanos(interval);
      this.limiters = new ConcurrentHashMap<>();
    }

    public RateLimiter getResourceLimiter(Object resource) {
      return limiters.computeIfAbsent(resource,
              (x) -> new RateLimiter(maxNrRequests, intervalNanos));
    }

  }

}
