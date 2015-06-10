
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

//CHECKSTYLE:OFF
import com.sun.management.HotSpotDiagnosticMXBean;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
//CHECKSTYLE:ON
import java.io.IOException;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.perf.MeasurementRecorder;
import org.spf4j.perf.impl.RecorderFactory;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.management.MBeanServer;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.Registry;

/**
 * This class allows you to poll and recordAt to a file the heap commited and heap used
 for your java process.
 *  start data recording by calling the startMemoryUsageSampling method,
 *  stop the data recording by calling the method: startMemoryUsageSampling.
 *
 * @author zoly
 */
@SuppressFBWarnings("IICU_INCORRECT_INTERNAL_CLASS_USE")
public final class MemoryUsageSampler {

    private MemoryUsageSampler() { }

    private static final MemoryMXBean MBEAN = ManagementFactory.getMemoryMXBean();

    private static ScheduledFuture<?> samplingFuture;
    private static AccumulatorRunnable accumulatorRunnable;

    static {
        org.spf4j.base.Runtime.queueHook(2, new AbstractRunnable(true) {
            @Override
            public void doRun() throws Exception {
                stop();
            }
        });
        Registry.export(MemoryUsageSampler.class);
    }

    public static synchronized void start(final long sampleTimeMilis) {
        start((int) sampleTimeMilis, (int) sampleTimeMilis * 10);
    }

    public static synchronized void start(final int sampleTimeMilis) {
        start(sampleTimeMilis, sampleTimeMilis * 10);
    }


    @JmxExport
    public static synchronized void start(@JmxExport("sampleTimeMillis") final int sampleTimeMilis,
            @JmxExport("accumulateIntervalMillis") final int accumulateIntervalMillis) {
        if (samplingFuture == null) {
            accumulatorRunnable = new AccumulatorRunnable(accumulateIntervalMillis);
            samplingFuture = DefaultScheduler.INSTANCE.scheduleWithFixedDelay(accumulatorRunnable,
                    sampleTimeMilis, sampleTimeMilis, TimeUnit.MILLISECONDS);
        } else {
            throw new IllegalStateException("Memory usage sampling already started " + samplingFuture);
        }
    }

    @JmxExport
    public static synchronized void stop() {
         if (samplingFuture != null) {
             samplingFuture.cancel(false);
             samplingFuture = null;
             accumulatorRunnable.close();
         }
    }

    @JmxExport
    public static synchronized boolean isStarted() {
        return samplingFuture != null;
    }

    private static class AccumulatorRunnable extends AbstractRunnable {

        public AccumulatorRunnable(final int accumulationIntervalMillis) {
            heapCommited =
                RecorderFactory.createScalableMinMaxAvgRecorder("heap-commited", "bytes", accumulationIntervalMillis);
            heapUsed =
                RecorderFactory.createScalableMinMaxAvgRecorder("heap-used", "bytes", accumulationIntervalMillis);
        }

        private final MeasurementRecorder heapCommited;
        private final MeasurementRecorder heapUsed;


        @Override
        public void doRun() throws Exception {
            MemoryUsage usage = MBEAN.getHeapMemoryUsage();
            heapCommited.record(usage.getCommitted());
            heapUsed.record(usage.getUsed());
        }

        public void close() {
            try {
                heapCommited.close();
                heapUsed.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        }
    }


    public static void dumpHeap(final String filename, final boolean liveObjectOnly) throws IOException {
        HOTSPOT_DIAGNOSTIC_INSTANCE.dumpHeap(filename, liveObjectOnly);
    }

    private static final String HOTSPOT_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";

    private static final HotSpotDiagnosticMXBean HOTSPOT_DIAGNOSTIC_INSTANCE = getHotspotMBean();


    public static HotSpotDiagnosticMXBean getHotspotDiagnosticBean() {
        return HOTSPOT_DIAGNOSTIC_INSTANCE;
    }

    private static HotSpotDiagnosticMXBean getHotspotMBean() {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            return ManagementFactory.newPlatformMXBeanProxy(server,
                HOTSPOT_BEAN_NAME, HotSpotDiagnosticMXBean.class);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }


}
