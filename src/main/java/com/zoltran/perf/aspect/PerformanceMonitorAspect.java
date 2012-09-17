
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
package com.zoltran.perf.aspect;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.zoltran.perf.MeasurementRecorderSource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.bouncycastle.asn1.x509.V1TBSCertificateGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zoly
 */
@Aspect
public class PerformanceMonitorAspect
{

    private static final Logger LOG = LoggerFactory.getLogger(PerformanceMonitorAspect.class);
    private static final LoadingCache<Class<? extends RecorderSourceInstance>, MeasurementRecorderSource> mrecSources =
            CacheBuilder.newBuilder().concurrencyLevel(8).
            build(new CacheLoader<Class<? extends RecorderSourceInstance>, MeasurementRecorderSource>()
    {
        @Override
        public MeasurementRecorderSource load(Class<? extends RecorderSourceInstance> key) throws Exception
        {
            return (MeasurementRecorderSource) key.getField("INSTANCE").get(null);
        }
    });

    @Around("execution(@com.zoltran.perf.aspect * *(..)) && annotation(annot)")
    public Object performanceMonitoredMethod(ProceedingJoinPoint pjp, PerformanceMonitor annot) throws Throwable
    {
        final long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        final long elapsed = System.currentTimeMillis() - start;
        MeasurementRecorderSource mrs = mrecSources.getUnchecked(annot.recorderSource());
        mrs.getRecorder(pjp.toLongString()).record(elapsed);
        if (elapsed > annot.warnThresholdMillis()) {
            if (elapsed > annot.errorThresholdMillis()) {
                LOG.error("Execution time  {} ms for {} exceeds error threshold, arguments {}", new Object[]{
                            elapsed, pjp.toShortString(), pjp.getArgs()
                        });
            } else {
                LOG.warn("Execution time  {} ms for {} exceeds warning threshold, arguments {}", new Object[]{
                            elapsed, pjp.toShortString(), pjp.getArgs()
                        });
            }
        } else {
            LOG.debug("Execution time {} ms for {}, arguments {}", new Object[]{
                        elapsed, pjp.toShortString(), pjp.getArgs()
                    });
        }
        return result;
    }
}
