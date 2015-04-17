
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

import org.spf4j.base.AbstractRunnable;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.perf.MeasurementRecorder;
import org.spf4j.perf.impl.RecorderFactory;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.Registry;

/**
 * This class allows you to poll and record to a file the heap commited and heap used
 * for your java process.
 *  start data recording by calling the startMemoryUsageSampling method,
 *  stop the data recording by calling the method: startMemoryUsageSampling.
 *
 * @author zoly
 */
public final class MemoryUsageSampler {

    private MemoryUsageSampler() { }

    private static final int AGG_INTERVAL =
            Integer.parseInt(System.getProperty("perf.memory.sampleAggMillis", "300000"));

    private static final MeasurementRecorder HEAP_COMMITED =
            RecorderFactory.createScalableMinMaxAvgRecorder("heap-commited", "bytes", AGG_INTERVAL);
    private static final MeasurementRecorder HEAP_USED =
            RecorderFactory.createScalableMinMaxAvgRecorder("heap-used", "bytes", AGG_INTERVAL);

    private static final MemoryMXBean MBEAN = ManagementFactory.getMemoryMXBean();

    private static ScheduledFuture<?> samplingFuture;


    static {
        java.lang.Runtime.getRuntime().addShutdownHook(new Thread(new AbstractRunnable(true) {
            @Override
            public void doRun() throws Exception {
                stop();
            }
        }, "shutdown-memory-sampler"));
        Registry.export(MemoryUsageSampler.class);
    }

    @JmxExport
    public static synchronized void start(@JmxExport("sampleTimeMillis") final long sampleTime) {
        if (samplingFuture == null) {
            samplingFuture = DefaultScheduler.INSTANCE.scheduleWithFixedDelay(new AbstractRunnable() {

                @Override
                public void doRun() throws Exception {
                    MemoryUsage usage = MBEAN.getHeapMemoryUsage();
                    HEAP_COMMITED.record(usage.getCommitted());
                    HEAP_USED.record(usage.getUsed());
                }
            }, sampleTime, sampleTime, TimeUnit.MILLISECONDS);
        } else {
            throw new IllegalStateException("Memory usage sampling already started " + samplingFuture);
        }
    }

    @JmxExport
    public static synchronized void stop() {
         if (samplingFuture != null) {
             samplingFuture.cancel(false);
             samplingFuture = null;
         }
    }

    @JmxExport
    public static synchronized boolean isStarted() {
        return samplingFuture != null;
    }


}
