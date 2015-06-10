
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

import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import java.lang.management.GarbageCollectorMXBean;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.perf.MeasurementRecorder;
import org.spf4j.perf.impl.RecorderFactory;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.Registry;

/**
 * This class allows you to poll and recordAt to a file the heap commited and heap used for your java process.
 * start data
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
        org.spf4j.base.Runtime.queueHook(2, new AbstractRunnable(true) {
            @Override
            public void doRun() throws Exception {
                stop();
            }
        });
        Registry.export(GCUsageSampler.class);
    }

    @JmxExport
    public static synchronized void start(@JmxExport("sampleTimeMillis") final int sampleTime) {
        if (samplingFuture == null) {
            final MeasurementRecorder gcUsage =
                RecorderFactory.createDirectRecorder("gc-time", "ms", sampleTime);
            samplingFuture = DefaultScheduler.INSTANCE.scheduleWithFixedDelay(new AbstractRunnable() {

                private final TObjectLongMap lastValues = new TObjectLongHashMap();

                @Override
                public void doRun() throws Exception {
                    synchronized (lastValues) {
                        gcUsage.record(getGCTimeDiff(MBEANS, lastValues));
                    }
                }
            }, sampleTime, sampleTime, TimeUnit.MILLISECONDS);
        } else {
            throw new IllegalStateException("GC usage sampling already started " + samplingFuture);
        }
    }

    @JmxExport
    public static synchronized void stop() {
        if (samplingFuture != null) {
            samplingFuture.cancel(false);
            samplingFuture = null;
        }
    }

    public static long getGCTimeDiff(final List<GarbageCollectorMXBean> gcBeans, final TObjectLongMap lastValues) {
        long gcTime = 0;
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            long prevVal = lastValues.get(gcBean);
            long currVal = gcBean.getCollectionTime();
            gcTime += currVal - prevVal;
            lastValues.put(gcBean, currVal);
        }
        return gcTime;
    }

    public static long getGCTime(final List<GarbageCollectorMXBean> gcBeans) {
        long gcTime = 0;
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            gcTime += gcBean.getCollectionTime();
        }
        return gcTime;
    }

    @JmxExport
    public static long getGCTime() {
        return getGCTime(MBEANS);
    }

    @JmxExport
    public static synchronized boolean isStarted() {
        return samplingFuture != null;
    }

}
