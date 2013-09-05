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

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.base.Holder;
import org.spf4j.perf.memory.GCUsageSampler;
import org.spf4j.stackmonitor.proto.Converter;

/**
 * Utility to sample stack traces.
 * Stack traces can be persisted for later analysis.
 * 
 * @author zoly
 */
@ThreadSafe
public final class Sampler implements SamplerMBean {

    private volatile boolean stopped;
    private volatile int sampleTimeMillis;
    private volatile int dumpTimeMillis;
    private volatile boolean isJmxRegistered;
    private final StackCollector stackCollector;
    private final ObjectName name;
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
        try {
            this.name = new ObjectName("SPF4J:name=StackSampler");
        } catch (MalformedObjectNameException ex) {
            throw new RuntimeException(ex);
        }
        this.isJmxRegistered = false;
        this.filePrefix = dumpFolder + File.separator + dumpFilePrefix;
    }

    public void registerJmx()
            throws MalformedObjectNameException, InstanceAlreadyExistsException,
            MBeanRegistrationException, NotCompliantMBeanException {
        ManagementFactory.getPlatformMBeanServer().registerMBean(this, name);
        isJmxRegistered = true;
    }

    private static final StackTraceElement [] GC_FAKE_STACK = new StackTraceElement[] {
        new StackTraceElement("java.lang.System", "gc", "System.java", -1)
    };
    
    @Override
    public synchronized void start() {
        if (stopped) {
            stopped = false;
            final int stMillis = sampleTimeMillis;
            final int dumpCount = dumpTimeMillis / stMillis;
            final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
            samplingThread = new Thread(new AbstractRunnable() {
                
                @SuppressWarnings("SleepWhileInLoop")
                @Override
                public void doRun() throws IOException, InterruptedException {
                    final Thread samplerThread = Thread.currentThread();
                    int dumpCounter = 0;
                    int coarseCounter = 0;
                    int coarseCount = 2000 / stMillis;
                    boolean lstopped = stopped;
                    long prevGcTime = 0;
                    while (!lstopped) {
                        stackCollector.sample(samplerThread);
                        dumpCounter++;
                        coarseCounter++;
                        if (coarseCounter >= coarseCount) {
                            coarseCounter = 0;
                            lstopped = stopped;
                            long gcTime = GCUsageSampler.getGCTime(gcBeans);
                            if (gcTime > prevGcTime) {
                                int fakeSamples = (int) ((gcTime - prevGcTime)) / stMillis;
                                for (int i = 0; i < fakeSamples; i++) {
                                    stackCollector.addSample(GC_FAKE_STACK);
                                }
                                prevGcTime = gcTime;
                            }
                        }
                        if (dumpCounter >= dumpCount) {
                            long timeSinceLastDump = System.currentTimeMillis() - lastDumpTime;
                            if (timeSinceLastDump >= dumpTimeMillis) {
                                dumpCounter = 0;
                                dumpToFile();
                            } else {
                                dumpCounter -= (dumpTimeMillis - timeSinceLastDump) / stMillis;
                            }
                        }
                        Thread.sleep(stMillis);
                   }
                }
            }, "Stack Sampling Thread");
            samplingThread.start();
        } else {
            throw new IllegalStateException("Sampling can only be started once");
        }

    }
    private static final DateTimeFormatter TS_FORMAT = ISODateTimeFormat.basicDateTimeNoMillis();

    public void dumpToFile() throws IOException {
        dumpToFile(null);
    }
    
    /**
     * Dumps the sampled stacks to file.
     * the collected samples are reset
     * @param id - id will be added to file name
     * returns the name of the file.
     * @throws IOException
     */
    
    public synchronized String dumpToFile(@Nullable final String id) throws IOException {
        final Holder<String> result = new Holder<String>();
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

    @Override
    public synchronized void generateHtmlMonitorReport(final String fileName, final int chartWidth, final int maxDepth)
            throws IOException {
        final Writer writer
                = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), Charsets.UTF_8));
        try {
            writer.append("<html>");

            stackCollector.applyOnSamples(new Function<SampleNode, SampleNode>() {
                @Override
                public SampleNode apply(final SampleNode input) {
                    if (input != null) {
                        SampleNode finput = input;
                        try {
                            writer.append("<h1>Total stats</h1>");
                            StackVisualizer.generateHtmlTable(writer, Method.ROOT, finput, chartWidth, maxDepth);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                    return input;
                }
            });


            stackCollector.applyOnSamples(new Function<SampleNode, SampleNode>() {
                @Override
                public SampleNode apply(final SampleNode input) {
                    if (input != null) {
                        SampleNode finput = input.filteredBy(WaitMethodClassifier.INSTANCE);
                        try {
                            writer.append("<h1>CPU stats</h1>");
                            StackVisualizer.generateHtmlTable(writer, Method.ROOT, finput, chartWidth, maxDepth);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                    return input;
                }
            });

            writer.append("</html>");

        } finally {
            writer.close();
        }

    }

    @Override
    public synchronized void stop() throws InterruptedException {
        if (!stopped) {
            stopped = true;
            samplingThread.join();
        }
    }

    @Override
    public int getSampleTimeMillis() {
        return sampleTimeMillis;
    }

    @Override
    public void setSampleTimeMillis(final int sampleTimeMillis) {
        this.sampleTimeMillis = sampleTimeMillis;
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    @Override
    public List<String> generate(final Properties props) throws IOException {
        int width = Integer.parseInt(props.getProperty("width", "1200"));
        int maxDepth = Integer.parseInt(props.getProperty("maxDepth", "1200"));
        String fileName = File.createTempFile("stack", ".html").getAbsolutePath();
        generateHtmlMonitorReport(fileName, width, maxDepth);
        return Arrays.asList(fileName);
    }

    @Override
    public List<String> getParameters() {
        return Arrays.asList("width");
    }

    @Override
    public void clear() {
        stackCollector.clear();
    }

    public StackCollector getStackCollector() {
        return stackCollector;
    }

    @PreDestroy
    public void dispose() throws InterruptedException, InstanceNotFoundException {
        stop();
        try {
            if (isJmxRegistered) {
                ManagementFactory.getPlatformMBeanServer().unregisterMBean(name);
            }
        } catch (InstanceNotFoundException ex) {
            throw new RuntimeException(ex);
        } catch (MBeanRegistrationException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void generateCpuSvg(final String fileName, final int chartWidth, final int maxDepth) throws IOException {
        final Writer writer
                = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), Charsets.UTF_8));
        try {

            stackCollector.applyOnSamples(new Function<SampleNode, SampleNode>() {
                @Override
                public SampleNode apply(final SampleNode input) {
                    if (input != null) {
                        SampleNode finput = input.filteredBy(WaitMethodClassifier.INSTANCE);
                        try {
                            StackVisualizer.generateSvg(writer, Method.ROOT, finput, 0, 0, chartWidth, maxDepth, "a");
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                    return input;
                }
            });
        } finally {
            writer.close();
        }
    }

    @Override
    public void generateTotalSvg(final String fileName, final int chartWidth, final int maxDepth) throws IOException {
        final Writer writer
                = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), Charsets.UTF_8));
        try {

            stackCollector.applyOnSamples(new Function<SampleNode, SampleNode>() {
                @Override
                public SampleNode apply(final SampleNode input) {
                    if (input != null) {

                        try {
                            StackVisualizer.generateSvg(writer, Method.ROOT, input, 0, 0, chartWidth, maxDepth, "b");
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                    return input;
                }
            });
        } finally {
            writer.close();
        }
    }

    @Override
    public void generateSvgHtmlMonitorReport(final String fileName, final int chartWidth, final int maxDepth)
            throws IOException {
        final Writer writer
                = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), Charsets.UTF_8));
        try {
            writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                    + "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.0//EN\""
                    + " \"http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd\">\n");
            writer.append("<html>");


            stackCollector.applyOnSamples(new Function<SampleNode, SampleNode>() {
                @Override
                public SampleNode apply(final SampleNode input) {
                    if (input != null) {

                        try {
                            writer.append("<h1>Total stats</h1>");
                            StackVisualizer.generateSvg(writer, Method.ROOT, input, 0, 0, chartWidth, maxDepth, "a");
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }

                    }
                    return input;
                }
            });


            stackCollector.applyOnSamples(new Function<SampleNode, SampleNode>() {
                @Override
                public SampleNode apply(final SampleNode input) {
                    if (input != null) {
                        SampleNode finput = input.filteredBy(WaitMethodClassifier.INSTANCE);
                        if (finput != null) {
                            try {
                                writer.append("<h1>CPU stats</h1>");
                                StackVisualizer.generateSvg(writer, Method.ROOT,
                                        finput, 0, 0, chartWidth, maxDepth, "b");
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }
                    return input;
                }
            });


            writer.append("</html>");

        } finally {
            writer.close();
        }

    }

    @Override
    public int getDumpTimeMillis() {
        return dumpTimeMillis;
    }

    @Override
    public void setDumpTimeMillis(final int dumpTimeMillis) {
        this.dumpTimeMillis = dumpTimeMillis;
    }

}
