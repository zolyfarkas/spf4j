
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
package org.spf4j.perf.aspect;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.spf4j.perf.MeasurementRecorderSource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zoly
 */
@Aspect
public final class PerformanceMonitorAspect {

    private static final Logger LOG = LoggerFactory.getLogger(PerformanceMonitorAspect.class);
    private static final LoadingCache<Class<? extends RecorderSourceInstance>, MeasurementRecorderSource> REC_SOURCES =
            CacheBuilder.newBuilder().concurrencyLevel(8).
            build(new CacheLoader<Class<? extends RecorderSourceInstance>, MeasurementRecorderSource>()
    {
        @Override
        public MeasurementRecorderSource load(final Class<? extends RecorderSourceInstance> key) throws Exception {
            return (MeasurementRecorderSource) key.getField("INSTANCE").get(null);
        }
    });
    
    
    

    @Around(value = "execution(@org.spf4j.perf.aspect.PerformanceMonitor * *(..)) && @annotation(annot)",
            argNames = "pjp,annot")
    public Object performanceMonitoredMethod(final ProceedingJoinPoint pjp, final PerformanceMonitor annot)
            throws Throwable {
        final long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        final long elapsed = System.currentTimeMillis() - start;
        MeasurementRecorderSource mrs = REC_SOURCES.getUnchecked(annot.recorderSource());
        mrs.getRecorder(pjp.toLongString()).record(elapsed);
        if (elapsed > annot.warnThresholdMillis()) {
            if (elapsed > annot.errorThresholdMillis()) {
                LOG.error("Execution time  {} ms for {} exceeds error threshold of {} ms, arguments {}",
                            elapsed, pjp.toShortString(), annot.errorThresholdMillis(), pjp.getArgs());
            } else {
                LOG.warn("Execution time  {} ms for {} exceeds warning threshold of {} ms, arguments {}",
                            elapsed, pjp.toShortString(), annot.warnThresholdMillis(), pjp.getArgs());
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
