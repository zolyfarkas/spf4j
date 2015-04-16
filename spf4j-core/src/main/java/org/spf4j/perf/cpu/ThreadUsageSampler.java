package org.spf4j.perf.cpu;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.io.ByteArrayBuilder;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.Registry;
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

    public static void writePeakThreadInfo(final PrintStream out) throws IOException {
        out.println("Peak Threads:");
        int i = 0;
        for (String tname : PEAK_THREAD_NAMES) {
            out.println(tname + ", daemon =" + PEAK_THREAD_DAEMON.get(i) + ",\n");
            i++;
        }
    }

    @JmxExport
    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    public static String getPeakThreadInfo() {
        try (ByteArrayBuilder bab = new ByteArrayBuilder()) {
            PrintStream ps = new PrintStream(bab);
            writePeakThreadInfo(ps);
            return bab.toString(Charset.defaultCharset());
        } catch (IOException ex) {
           throw new RuntimeException(ex);
        }
    }

    static {
        java.lang.Runtime.getRuntime().addShutdownHook(new Thread(new AbstractRunnable(true) {
            @Override
            public void doRun() throws Exception {
                stopThreadUsageSampling();
                writePeakThreadInfo(System.err);
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
            if (Registry.unregister(ThreadUsageSampler.class) == null) {
                Registry.export(ThreadUsageSampler.class);
            }
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
