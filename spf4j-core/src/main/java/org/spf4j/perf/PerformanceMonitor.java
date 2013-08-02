/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.perf;

import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zoly
 */
public final class PerformanceMonitor {

    private PerformanceMonitor() {
    }
    
    private static final Logger LOG = LoggerFactory.getLogger(PerformanceMonitor.class);

    
    public static <T> T callAndMonitor(final MeasurementRecorderSource mrs,
            final long warnMillis, final long errorMillis, final Callable<T> callable,
            final String callableName) throws Exception {
        return performanceMonitoredCallable(mrs, warnMillis, errorMillis, callable, callableName).call();
    }
    
    public static <T> T callAndMonitor(final MeasurementRecorderSource mrs,
            final long warnMillis, final long errorMillis, final Callable<T> callable,
            final String callableName, final boolean isLogInfo, final Object... detail) throws Exception {
        return performanceMonitoredCallable(mrs, warnMillis, errorMillis, callable, callableName, isLogInfo, detail)
                .call();
    }
    
    public static <T> Callable<T> performanceMonitoredCallable(final MeasurementRecorderSource mrs,
            final long warnMillis, final long errorMillis, final Callable<T> callable,
            final String callableName) {
        return performanceMonitoredCallable(mrs, warnMillis, errorMillis, callable, callableName, false);
    }

    public static <T> Callable<T> performanceMonitoredCallable(final MeasurementRecorderSource mrs,
            final long warnMillis, final long errorMillis, final Callable<T> callable,
            final String callableName, final boolean isLogInfo, final Object... detail) {

        return new Callable<T>() {
            @Override
            public T call() throws Exception {
                final long start = System.currentTimeMillis();
                T result = callable.call();
                final long elapsed = System.currentTimeMillis() - start;

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
                        LOG.info("Execution time {} ms for {}, detail: {}", elapsed, callableName);
                    } else {
                        LOG.debug("Execution time {} ms for {}, detail: {}", elapsed, callableName);
                    }
                }
                return result;

            }
        };

    }
}
