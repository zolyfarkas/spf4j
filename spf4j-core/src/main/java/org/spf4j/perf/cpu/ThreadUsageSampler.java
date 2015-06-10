package org.spf4j.perf.cpu;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
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
    private static final List<StackTraceElement []> PEAK_THREAD_TRACES = new ArrayList<>();
    private static final BitSet PEAK_THREAD_DAEMON = new BitSet();

    public static void writePeakThreadInfo(final PrintStream out) throws IOException {
        out.println("Peak Threads:");
        int i = 0;
        boolean haveStacktraces = PEAK_THREAD_TRACES.size() > 0;
        for (String tname : PEAK_THREAD_NAMES) {
            out.print(tname);
            out.print(", daemon =");
            out.print(PEAK_THREAD_DAEMON.get(i));
            out.print(',');
            if (haveStacktraces) {
                out.print(' ');
                out.print(Arrays.toString(PEAK_THREAD_TRACES.get(i)));
            }
            out.println();
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
        org.spf4j.base.Runtime.queueHook(2, new AbstractRunnable(true) {
            @Override
            public void doRun() throws Exception {
                stop();
                writePeakThreadInfo(System.err);
            }

        });
        Registry.export(ThreadUsageSampler.class);
    }


   public static synchronized void start(final int sampleTime) {
       start(sampleTime, true);
   }

    @JmxExport
    public static synchronized void start(@JmxExport("sampleTimeMillis") final int sampleTime,
            @JmxExport("withStackTraces") final boolean withStackTraces) {
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
                            if (withStackTraces) {
                                StackTraceElement[][] stackTraces = FastStackCollector.getStackTraces(ths);
                                PEAK_THREAD_TRACES.clear();
                                PEAK_THREAD_TRACES.addAll(Arrays.asList(stackTraces));
                            }

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
