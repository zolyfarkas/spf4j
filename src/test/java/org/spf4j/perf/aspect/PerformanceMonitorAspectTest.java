
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
import org.junit.Test;
import org.spf4j.perf.MeasurementRecorderSource;

/**
 *
 * @author zoly
 */
public final class PerformanceMonitorAspectTest {

    /**
     * Test of performanceMonitoredMethod method, of class PerformanceMonitorAspect.
     */
    @Test
    public void testPerformanceMonitoredMethod() throws Exception {
        for (int i = 0; i < 10; i++) {
            somethingTomeasure(i, "Test");
        }
    }

    @PerformanceMonitor(warnThresholdMillis = 1)
    public void somethingTomeasure(final int arg1, final String arg2) throws InterruptedException {
        Thread.sleep(10);
    }
    
    private static final LoadingCache<Class<? extends RecorderSourceInstance>, MeasurementRecorderSource> REC_SOURCES =
            CacheBuilder.newBuilder().concurrencyLevel(8).
            build(new CacheLoader<Class<? extends RecorderSourceInstance>, MeasurementRecorderSource>()
    {
        @Override
        public MeasurementRecorderSource load(final Class<? extends RecorderSourceInstance> key) throws Exception {
            return (MeasurementRecorderSource) key.getField("INSTANCE").get(null);
        }
    });
    
    @Test
    public void testPerformanceImplementation() throws Exception {
        MeasurementRecorderSource source;
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            source = (MeasurementRecorderSource) RecorderSourceInstance.RsNop.class.getField("INSTANCE").get(null);
        }
        long intTime = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            source = REC_SOURCES.get(RecorderSourceInstance.RsNop.class);
        }
        long endTime = System.currentTimeMillis();
        System.out.println("non cached " + (intTime - startTime));
        System.out.println("cached " + (endTime - intTime));
        
    }
    
}
