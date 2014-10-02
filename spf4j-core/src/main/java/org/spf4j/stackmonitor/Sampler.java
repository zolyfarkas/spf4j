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
package org.spf4j.stackmonitor;

import com.google.common.base.Function;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.base.IntMath;
import org.spf4j.base.MutableHolder;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.Registry;
import org.spf4j.perf.memory.GCUsageSampler;
import org.spf4j.stackmonitor.proto.Converter;

/**
 * Utility to sample stack traces.
 * Stack traces can be persisted for later analysis.
 * 
 * please read http://sape.inf.usi.ch/sites/default/files/publication/pldi10.pdf
 * pure java stack sampling will probably have safepoint bias.
 * 
 * @author zoly
 */
@ThreadSafe
public final class Sampler {

    private volatile boolean stopped;
    private volatile int sampleTimeMillis;
    private volatile int dumpTimeMillis;
    private final StackCollector stackCollector;
    private volatile long lastDumpTime = System.currentTimeMillis();
    
    @GuardedBy("this")
    private Thread samplingThread;
    private final String filePrefix;

    public Sampler() {
        this(100, 3600000, new FastStackCollector());
    }

    public Sampler(final int sampleTimeMillis) {
        this(sampleTimeMillis, 3600000, new MxStackCollector());
    }

    public Sampler(final StackCollector collector) {
        this(100, 3600000, collector);
    }
    
    public Sampler(final int sampleTimeMillis, final int dumpTimeMillis, final StackCollector collector) {
        this(sampleTimeMillis, dumpTimeMillis, collector,
                System.getProperty("perf.db.folder", System.getProperty("java.io.tmpdir")),
                System.getProperty("perf.db.name", ManagementFactory.getRuntimeMXBean().getName()));
    }

    public Sampler(final int sampleTimeMillis, final int dumpTimeMillis, final StackCollector collector,
            final String dumpFolder, final String dumpFilePrefix) {
        stopped = true;
        this.sampleTimeMillis = sampleTimeMillis;
        this.dumpTimeMillis = dumpTimeMillis;
        this.stackCollector = collector;
        this.filePrefix = dumpFolder + File.separator + dumpFilePrefix;
    }

    public void registerJmx() {
        Registry.export(this);
    }

    private static final StackTraceElement [] GC_FAKE_STACK = new StackTraceElement[] {
        new StackTraceElement("java.lang.System", "gc", "System.java", -1)
    };
    
    private final IntMath.XorShift32 random = new IntMath.XorShift32();
    
    private static final int STOP_FLAG_READ_MILLIS = 2000;
    
    @JmxExport(description = "start stack sampling")
    public synchronized void start() {
        if (stopped) {
            stopped = false;
            final int stMillis = sampleTimeMillis;
            final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
            samplingThread = new Thread(new AbstractRunnable() {
                
                @SuppressWarnings("SleepWhileInLoop")
                @SuppressFBWarnings("MDM_THREAD_YIELD")
                @Override
                public void doRun() throws IOException, InterruptedException {
                    final Thread samplerThread = Thread.currentThread();
                    int dumpCounterMs = 0;
                    int coarseCounter = 0;
                    int coarseCount = STOP_FLAG_READ_MILLIS / stMillis;
                    boolean lstopped = stopped;
                    long prevGcTime = 0;
                    int sleepTime = 0;
                    while (!lstopped) {
                        stackCollector.sample(samplerThread);
                        dumpCounterMs += sleepTime;
                        coarseCounter++;
                        if (coarseCounter >= coarseCount) {
                            coarseCounter = 0;
                            lstopped = stopped;
                            long gcTime = GCUsageSampler.getGCTime(gcBeans);
                            if (gcTime > prevGcTime) {
                                int fakeSamples = (int) ((gcTime - prevGcTime)) / stMillis;
                                for (int i = 0; i < fakeSamples; i++) { // can be optimized
                                    stackCollector.addSample(GC_FAKE_STACK);
                                }
                                prevGcTime = gcTime;
                            }
                        }
                        if (dumpCounterMs >= dumpTimeMillis) {
                            long timeSinceLastDump = System.currentTimeMillis() - lastDumpTime;
                            if (timeSinceLastDump >= dumpTimeMillis) {
                                dumpCounterMs = 0;
                                dumpToFile();
                            } else {
                                dumpCounterMs -= dumpTimeMillis - timeSinceLastDump;
                            }
                        }
                        sleepTime = stMillis + random.nextInt() % stMillis;
                        Thread.sleep(sleepTime);
                   }
                }
            }, "Stack Sampling Thread");
            samplingThread.start();
        } else {
            throw new IllegalStateException("Sampling can only be started once");
        }

    }
    
    private static final DateTimeFormatter TS_FORMAT = ISODateTimeFormat.basicDateTimeNoMillis();

    @JmxExport(description = "save stack samples to file")
    public String dumpToFile() throws IOException {
        return dumpToFile(null);
    }
    
    /**
     * Dumps the sampled stacks to file.
     * the collected samples are reset
     * @param id - id will be added to file name
     * returns the name of the file.
     * @throws IOException
     */
    
    @JmxExport(name = "dumpToSpecificFile", description = "save stack samples to file")
    public synchronized String dumpToFile(
            @JmxExport(name = "fileName", description = "the file name to save to")
            @Nullable final String id) throws IOException {
        final MutableHolder<String> result = new MutableHolder<String>();
        stackCollector.applyOnSamples(new Function<SampleNode, SampleNode>() {
            @Override
            public SampleNode apply(final SampleNode input) {
                try {
                    if (input != null) {
                        String fileName = filePrefix + "_" + ((id == null) ? "" : id + "_")
                                + TS_FORMAT.print(lastDumpTime) + "_"
                                + TS_FORMAT.print(System.currentTimeMillis()) + ".ssdump";
                        final BufferedOutputStream bos = new BufferedOutputStream(
                                new FileOutputStream(fileName));
                        try {
                            Converter.fromSampleNodeToProto(input).writeTo(bos);
                            lastDumpTime = System.currentTimeMillis();
                            result.setValue(fileName);
                        } finally {
                            bos.close();
                        }
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                return null;
            }
        });

        return result.getValue();
    }


    @JmxExport(description = "stop stack sampling")
    public synchronized void stop() throws InterruptedException {
        if (!stopped) {
            stopped = true;
            samplingThread.join(STOP_FLAG_READ_MILLIS << 2);
            if (samplingThread.isAlive()) {
                throw new RuntimeException("Sampling thread failed to stoo in " + (STOP_FLAG_READ_MILLIS << 2));
            }
        }
    }

    @JmxExport(description = "stack sample time in milliseconds")
    public int getSampleTimeMillis() {
        return sampleTimeMillis;
    }

    @JmxExport
    public void setSampleTimeMillis(final int sampleTimeMillis) {
        this.sampleTimeMillis = sampleTimeMillis;
    }

    @JmxExport(description = "is the stack sampling stopped")
    public boolean isStopped() {
        return stopped;
    }


    @JmxExport(description = "clear in memeory collected stack samples")
    public void clear() {
        stackCollector.clear();
    }

    public StackCollector getStackCollector() {
        return stackCollector;
    }

    @PreDestroy
    public void dispose() throws InterruptedException {
        stop();
        Registry.unregister(this);
    }



    @JmxExport(description = "interval in milliseconds to save stack stamples periodically")
    public int getDumpTimeMillis() {
        return dumpTimeMillis;
    }

    @JmxExport
    public void setDumpTimeMillis(final int dumpTimeMillis) {
        this.dumpTimeMillis = dumpTimeMillis;
    }

}
