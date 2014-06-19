
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
package org.spf4j.perf.memory;

import java.lang.management.GarbageCollectorMXBean;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.perf.MeasurementRecorder;
import org.spf4j.perf.impl.RecorderFactory;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * This class allows you to poll and record to a file the heap commited and heap used for your java process. start data
 * recording by calling the startMemoryUsageSampling method, stop the data recording by calling the method:
 * startMemoryUsageSampling.
 *
 * @author zoly
 */
public final class GCUsageSampler {

    private GCUsageSampler() {
    }
    private static final List<GarbageCollectorMXBean> MBEANS = ManagementFactory.getGarbageCollectorMXBeans();
    private static ScheduledFuture<?> samplingFuture;

    static {
        java.lang.Runtime.getRuntime().addShutdownHook(new Thread(new AbstractRunnable(true) {
            @Override
            public void doRun() throws Exception {
                stopGCUsageSampling();
            }
        }, "shutdown-memory-sampler"));
    }

    public static synchronized void startGCUsageSampling(final int sampleTime) {
        if (samplingFuture == null) {
            final MeasurementRecorder gcUsage =
                RecorderFactory.createDirectRecorder("gc-time", "ms", sampleTime);
            samplingFuture = DefaultScheduler.INSTANCE.scheduleWithFixedDelay(new AbstractRunnable() {
                @Override
                public void doRun() throws Exception {
                    gcUsage.record(getGCTime(MBEANS));
                }
            }, sampleTime, sampleTime, TimeUnit.MILLISECONDS);
        }
    }

    public static synchronized void stopGCUsageSampling() {
        if (samplingFuture != null) {
            samplingFuture.cancel(false);
            samplingFuture = null;
        }
    }

    public static long getGCTime(final List<GarbageCollectorMXBean> gcBeans) {
        long gcTime = 0;
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            gcTime += gcBean.getCollectionTime();
        }
        return gcTime;
    }
}
