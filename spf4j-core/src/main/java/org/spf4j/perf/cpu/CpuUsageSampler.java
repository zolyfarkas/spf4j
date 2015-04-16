
package org.spf4j.perf.cpu;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.perf.MeasurementRecorder;
import org.spf4j.perf.impl.RecorderFactory;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("IICU_INCORRECT_INTERNAL_CLASS_USE")
public final class CpuUsageSampler {

    private CpuUsageSampler() { }

    private static final com.sun.management.OperatingSystemMXBean OS_MBEAN;

    static {
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        if (operatingSystemMXBean instanceof com.sun.management.OperatingSystemMXBean) {
            OS_MBEAN = (com.sun.management.OperatingSystemMXBean) operatingSystemMXBean;
        } else {
            OS_MBEAN = null;
        }
    }


    private static ScheduledFuture<?> samplingFuture;

    static {
        if (OS_MBEAN != null) {
            java.lang.Runtime.getRuntime().addShutdownHook(new Thread(new AbstractRunnable(true) {
                @Override
                public void doRun() throws Exception {
                    stopCPUUsageSampling();
                }
            }, "shutdown-CPU-sampler"));
        }
    }

    public static synchronized void startCPUUsageSampling(final int sampleTime) {
        if (samplingFuture == null) {
            final MeasurementRecorder cpuUsage =
                RecorderFactory.createDirectRecorder("cpu-time", "ms", sampleTime);
            samplingFuture = DefaultScheduler.INSTANCE.scheduleWithFixedDelay(new AbstractRunnable() {

                private long lastValue = 0;

                @Override
                public void doRun() throws Exception {
                     long currTime = OS_MBEAN.getProcessCpuTime();
                     cpuUsage.record(currTime - lastValue);
                     lastValue = currTime;
                }
            }, sampleTime, sampleTime, TimeUnit.MILLISECONDS);
        } else {
            throw new IllegalStateException("Cpu time Sampling already started " + samplingFuture);
        }
    }

    public static synchronized void stopCPUUsageSampling() {
        if (samplingFuture != null) {
            samplingFuture.cancel(false);
            samplingFuture = null;
        }
    }

}
