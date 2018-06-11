/*
 * Copyright 2017 SPF4J.
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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.LongSupplier;

/**
 * Utility method to convert between TimeSource.nanoTime and System.currentTimeMillis
 * @author Zoltan Farkas
 */
public final class TimeSource {

  private static final LongSupplier TIMESUPP;

  static {
    String cfgTimeSource = System.getProperty("spf4j.timeSource");
    if (cfgTimeSource == null) {
      TIMESUPP = () -> System.nanoTime();
    } else if ("systemTime".equals(cfgTimeSource)) {
      // System time is about 3 times faster on macOSX (30ns vs 10ns):
      // and worth considerign is more than millisecond precission is not needed:
      // TimingBenchmark.getCurrentTimeMillis         thrpt   10  108291306.615 ± 3493136.191  ops/s
      // TimingBenchmark.getNanoTime                  thrpt   10  31056232.360 ± 1804247.295  ops/s
      TIMESUPP = new SystemTimeProvider();
    } else {
      try {
        TIMESUPP = (LongSupplier) Class.forName(cfgTimeSource).newInstance();
      } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
        throw new ExceptionInInitializerError(ex);
      }
    }

  }

  private TimeSource() { }

  /**
   * Get JVM. time source. Default implementation calls System.nanotime.
   *
   * @return
   */
  public static long nanoTime() {
    return TIMESUPP.getAsLong();
  }

  public static LongSupplier nanoTimeSupplier() {
    return TIMESUPP;
  }

  public static long getDeadlineNanos(final long timeout, final TimeUnit timeUnit) {
    if (timeout < 0) {
      throw new IllegalArgumentException("Invalid timeout " + timeout + " " + timeUnit);
    }
    return nanoTime() + timeUnit.toNanos(timeout);
  }

  public static long getDeadlineNanos(final long currentTimeNanos, final long timeout, final TimeUnit timeUnit) {
    if (timeout < 0) {
      throw new IllegalArgumentException("Invalid timeout " + timeout + " " + timeUnit);
    }
    return currentTimeNanos + timeUnit.toNanos(timeout);
  }


  public static long getTimeToDeadlineStrict(final long deadlineNanos, final TimeUnit timeUnit)
          throws TimeoutException {
    long timeoutNanos = deadlineNanos - nanoTime();
    if (timeoutNanos < 0) {
      throw new TimeoutException("Exceeded deadline " + deadlineNanos + " with " + (-timeoutNanos));
    }
    return timeUnit.convert(timeoutNanos, TimeUnit.NANOSECONDS);
  }

  public static long getTimeToDeadline(final long deadlineNanos, final TimeUnit timeUnit) {
    long timeoutNanos = deadlineNanos - nanoTime();
    return timeUnit.convert(timeoutNanos, TimeUnit.NANOSECONDS);
  }

  private static final class SystemTimeProvider implements LongSupplier {

    private static final long LOCAL_EPOCH = System.currentTimeMillis();

    @Override
    public long getAsLong() {
      return TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis() - LOCAL_EPOCH);
    }

  }


}
