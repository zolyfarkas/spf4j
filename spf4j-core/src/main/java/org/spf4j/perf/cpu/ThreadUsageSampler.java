package org.spf4j.perf.cpu;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.perf.MeasurementRecorder;
import org.spf4j.perf.impl.RecorderFactory;
import org.spf4j.stackmonitor.FastStackCollector;

/**
 *
 * @author zoly
 */
public final class ThreadUsageSampler {

    private ThreadUsageSampler() {
    }

    private static final ThreadMXBean TH_BEAN = ManagementFactory.getThreadMXBean();

    private static ScheduledFuture<?> samplingFuture;

    private static final List<String> PEAK_THREAD_NAMES = new ArrayList<>();
    private static final BitSet PEAK_THREAD_DAEMON = new BitSet();

    static {
        java.lang.Runtime.getRuntime().addShutdownHook(new Thread(new AbstractRunnable(true) {
            @Override
            public void doRun() throws Exception {
                stopThreadUsageSampling();
                System.err.println("Peak Threads:");
                int i = 0;
                for (String tname : PEAK_THREAD_NAMES) {
                    System.err.println(tname + ", daemon =" + PEAK_THREAD_DAEMON.get(i));
                    i++;
                }
            }
        }, "shutdown-CPU-sampler"));
    }

    public static synchronized void startThreadUsageSampling(final int sampleTime) {
        if (samplingFuture == null) {
            final MeasurementRecorder cpuUsage
                    = RecorderFactory.createDirectRecorder("peak-thread-count", "count", sampleTime);
            samplingFuture = DefaultScheduler.INSTANCE.scheduleWithFixedDelay(new AbstractRunnable() {

                private int maxThreadsNr = 0;

                @Override
                public void doRun() throws Exception {
                    final int peakThreadCount = TH_BEAN.getPeakThreadCount();
                    cpuUsage.record(peakThreadCount);
                    if (peakThreadCount > maxThreadsNr) {
                        Thread[] ths = FastStackCollector.getThreads();
                        if (ths.length > PEAK_THREAD_NAMES.size()) {
                            PEAK_THREAD_NAMES.clear();
                            int i = 0;
                            for (Thread th : ths) {
                                PEAK_THREAD_NAMES.add(th.getName());
                                if (th.isDaemon()) {
                                    PEAK_THREAD_DAEMON.set(i);
                                } else {
                                    PEAK_THREAD_DAEMON.clear(i);
                                }
                                i++;
                            }
                        }
                        maxThreadsNr = peakThreadCount;
                    }
                    TH_BEAN.resetPeakThreadCount();
                }
            }, sampleTime, sampleTime, TimeUnit.MILLISECONDS);
        } else {
            throw new IllegalStateException("Thread sampling already started " + samplingFuture);
        }
    }

    public static synchronized void stopThreadUsageSampling() {
        if (samplingFuture != null) {
            samplingFuture.cancel(false);
            samplingFuture = null;
        }
    }

}
