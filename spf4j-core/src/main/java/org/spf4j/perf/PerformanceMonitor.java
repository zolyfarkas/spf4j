/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
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
 */
package org.spf4j.perf;

import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.perf.impl.NopMeasurementRecorder;

/**
 *
 * @author zoly
 */
public final class PerformanceMonitor {

  private static final Logger LOG = LoggerFactory.getLogger(PerformanceMonitor.class);

  private PerformanceMonitor() {
  }

  public static <T> T callAndMonitor(
          final long warnMillis, final long errorMillis, final Callable<T> callable) throws Exception {
    return performanceMonitoredCallable(warnMillis, errorMillis, callable).call();
  }

  public static <T> T callAndMonitor(final MeasurementRecorderSource mrs,
          final long warnMillis, final long errorMillis, final Callable<T> callable) throws Exception {
    return performanceMonitoredCallable(mrs, warnMillis, errorMillis, callable).call();
  }

  public static <T> T callAndMonitor(final MeasurementRecorderSource mrs,
          final long warnMillis, final long errorMillis, final Callable<T> callable,
          final boolean isLogInfo, final Object... detail) throws Exception {
    return performanceMonitoredCallable(mrs, warnMillis, errorMillis, callable, isLogInfo, detail)
            .call();
  }

  public static <T> Callable<T> performanceMonitoredCallable(final MeasurementRecorderSource mrs,
          final long warnMillis, final long errorMillis, final Callable<T> callable) {
    return performanceMonitoredCallable(mrs, warnMillis, errorMillis, callable, false);
  }

  public static <T> Callable<T> performanceMonitoredCallable(final MeasurementRecorderSource mrs,
          final long warnMillis, final long errorMillis, final Callable<T> callable,
          final boolean isLogInfo, final Object... detail) {

    return new Callable<T>() {
      @Override
      public T call() throws Exception {
        final long start = System.currentTimeMillis();
        T result = callable.call();
        final long elapsed = System.currentTimeMillis() - start;
        String callableName = callable.toString();
        mrs.getRecorder(callableName).record(elapsed);
        if (elapsed > warnMillis) {
          if (elapsed > errorMillis) {
            LOG.error("Execution time  {} ms for {} exceeds error threshold of {} ms, detail: {}",
                    elapsed, callableName, errorMillis, detail);
          } else {
            LOG.warn("Execution time  {} ms for {} exceeds warning threshold of {} ms, detail: {}",
                    elapsed, callableName, warnMillis, detail);
          }
        } else {
          if (isLogInfo) {
            LOG.info("Execution time {} ms for {}, detail: {}", elapsed, callableName, detail);
          } else {
            LOG.debug("Execution time {} ms for {}, detail: {}", elapsed, callableName, detail);
          }
        }
        return result;

      }
    };

  }

  public static <T> Callable<T> performanceMonitoredCallable(
          final long warnMillis, final long errorMillis, final Callable<T> callable) {
    return performanceMonitoredCallable(NopMeasurementRecorder.INSTANCE,
            warnMillis, errorMillis, callable, false);
  }

  public static <T> Callable<T> performanceMonitoredCallable(
          final long warnMillis, final long errorMillis, final Callable<T> callable,
          final boolean isLogInfo, final Object... detail) {
    return performanceMonitoredCallable(NopMeasurementRecorder.INSTANCE,
            warnMillis, errorMillis, callable, isLogInfo, detail);
  }

  public static <T> Callable<T> performanceMonitoredCallable(final MeasurementRecorder mr,
          final long warnMillis, final long errorMillis, final Callable<T> callable,
          final boolean isLogInfo, final Object... detail) {

    return new Callable<T>() {
      @Override
      public T call() throws Exception {
        final long start = System.currentTimeMillis();
        T result = callable.call();
        final long elapsed = System.currentTimeMillis() - start;
        mr.record(elapsed);
        String callableName = callable.toString();
        if (elapsed > warnMillis) {
          if (elapsed > errorMillis) {
            LOG.error("Execution time  {} ms for {} exceeds error threshold of {} ms, detail: {}",
                    elapsed, callableName, errorMillis, detail);
          } else {
            LOG.warn("Execution time  {} ms for {} exceeds warning threshold of {} ms, detail: {}",
                    elapsed, callableName, warnMillis, detail);
          }
        } else {
          if (isLogInfo) {
            LOG.info("Execution time {} ms for {}, detail: {}", elapsed, callableName, detail);
          } else {
            LOG.debug("Execution time {} ms for {}, detail: {}", elapsed, callableName, detail);
          }
        }
        return result;

      }
    };

  }
}
