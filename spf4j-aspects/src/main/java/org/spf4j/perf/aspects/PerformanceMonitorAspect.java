
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
package org.spf4j.perf.aspects;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.spf4j.perf.MeasurementRecorderSource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.concurrent.UnboundedLoadingCache;
import org.spf4j.annotations.PerformanceMonitor;
import org.spf4j.annotations.RecorderSourceInstance;

/**
 * Aspect that measures execution time and does performance logging
 * for all methods annotated with: PerformanceMonitor annotation.
 * 
 * @author zoly
 */
@Aspect
public final class PerformanceMonitorAspect {

    private static final Logger LOG = LoggerFactory.getLogger(PerformanceMonitorAspect.class);
    private static final LoadingCache<Class<? extends RecorderSourceInstance>, MeasurementRecorderSource> REC_SOURCES =
            new UnboundedLoadingCache<Class<? extends RecorderSourceInstance>, MeasurementRecorderSource>(
                    32, new CacheLoader<Class<? extends RecorderSourceInstance>, MeasurementRecorderSource>() {
        @Override
        public MeasurementRecorderSource load(final Class<? extends RecorderSourceInstance> key) throws Exception {
            return (MeasurementRecorderSource) key.getField("INSTANCE").get(null);
        }
    });

    @Around(value = "execution(@org.spf4j.annotations.PerformanceMonitor * *(..)) && @annotation(annot)",
            argNames = "pjp,annot")
    public Object performanceMonitoredMethod(final ProceedingJoinPoint pjp, final PerformanceMonitor annot)
            throws Throwable {
        final long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        final long elapsed = System.currentTimeMillis() - start;
        MeasurementRecorderSource mrs = REC_SOURCES.getUnchecked(annot.recorderSource());
        mrs.getRecorder(pjp.toLongString()).record(elapsed);
        final long warnThresholdMillis = annot.warnThresholdMillis();
        if (elapsed > warnThresholdMillis) {
            final long errorThresholdMillis = annot.errorThresholdMillis();
            if (elapsed > errorThresholdMillis) {
                LOG.error("Execution time  {} ms for {} exceeds error threshold of {} ms, arguments {}",
                            elapsed, pjp.toShortString(), errorThresholdMillis, pjp.getArgs());
            } else {
                LOG.warn("Execution time  {} ms for {} exceeds warning threshold of {} ms, arguments {}",
                            elapsed, pjp.toShortString(), warnThresholdMillis, pjp.getArgs());
            }
        } else {
            if (annot.defaultInfoLog()) {
                LOG.info("Execution time {} ms for {}, arguments {}",
                        elapsed, pjp.toShortString(), pjp.getArgs());
            } else {
                LOG.debug("Execution time {} ms for {}, arguments {}",
                        elapsed, pjp.toShortString(), pjp.getArgs());
            }
        }
        return result;
    }
}
