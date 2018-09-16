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
package org.spf4j.perf.aspects;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.concurrent.TimeUnit;
import org.spf4j.perf.MeasurementRecorderSource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.concurrent.UnboundedLoadingCache;
import org.spf4j.annotations.PerformanceMonitor;
import org.spf4j.annotations.RecorderSourceInstance;
import org.spf4j.base.TimeSource;

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
            new UnboundedLoadingCache<>(32,
                    new CacheLoader<Class<? extends RecorderSourceInstance>, MeasurementRecorderSource>() {
        @Override
        public MeasurementRecorderSource load(final Class<? extends RecorderSourceInstance> key) throws Exception {
            return (MeasurementRecorderSource) key.getField("INSTANCE").get(null);
        }
    });
    @Around(value = "@annotation(annot)"
            + " && execution(@org.spf4j.annotations.PerformanceMonitor * *(..))",
            argNames = "pjp,annot")
    public Object performanceMonitoredMethod(final ProceedingJoinPoint pjp, final PerformanceMonitor annot)
            throws Throwable {
        final long start = TimeSource.nanoTime();
        Object result = pjp.proceed();
        final long elapsedNanos = TimeSource.nanoTime() - start;
        final long elapsed = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
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
