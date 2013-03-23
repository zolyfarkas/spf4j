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
import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.*;
import org.spf4j.base.AbstractRunnable;

/**
 * Utility that allow you to sample what the application is doing. It generates
 * a "Flame Graph" that allows you to quickly see you "heavy" operations.
 *
 * You can use JConsole to control the sampling while your application is
 * running.
 *
 * By using a sampling approach you can choose your overhead. (sampling takes
 * about 0.5 ms, so the default of 10Hz will give you 0.5% overhead)
 *
 * Collection is separated into CPU, WAIT and IO categories. I felt that most
 * important is to see what hogs your CPU because that is where, you can most
 * likely can do something about it.
 *
 * @author zoly
 */
@ThreadSafe
public class Sampler implements SamplerMBean {

    private volatile boolean stopped;
    private volatile long sampleTimeMillis;
    private volatile boolean isJmxRegistered;
    private final StackCollector stackCollector;
    private final ObjectName name;
    
    @GuardedBy("this")
    private Thread samplingThread;
    

    public Sampler() {
        this(100, new MxStackCollector());
    }
    
      public Sampler(long sampleTimeMillis) {
        this(sampleTimeMillis, new MxStackCollector());
    }
    
    public Sampler( StackCollector collector) {
        this(100, collector);
    }

    public Sampler(long sampleTimeMillis, StackCollector collector) {
        stopped = true;
        this.sampleTimeMillis = sampleTimeMillis;
        this.stackCollector = collector;
        try {
            this.name = new ObjectName("SPF4J:name=StackSampler");
        } catch (MalformedObjectNameException ex) {
            throw new RuntimeException(ex);
        } 
        this.isJmxRegistered = false;
    }


    public void registerJmx() throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        ManagementFactory.getPlatformMBeanServer().registerMBean(this, name);
        isJmxRegistered=true;
    }

    @Override
    public synchronized void start() {
        if (stopped) {
            stopped = false;
            final long stMillis = sampleTimeMillis;
            samplingThread = new Thread(new AbstractRunnable() {

                @SuppressWarnings("SleepWhileInLoop")
                @Override
                public void doRun() {
                    while (!stopped) {
                        stackCollector.sample();
                        try {
                            Thread.sleep(stMillis);
                        } catch (InterruptedException ex) {
                            stopped = true;
                        }
                    }
                }
            }, "Stack Sampling Thread");
            samplingThread.start();
        } else {
            throw new IllegalStateException("Sampling can only be started once");
        }

    }

    @Override
    public synchronized void generateHtmlMonitorReport(String fileName, final int chartWidth, final int maxDepth) throws IOException {

        final Writer writer = new BufferedWriter(new OutputStreamWriter( new FileOutputStream(fileName), Charsets.UTF_8));
        try {
            writer.append("<html>");

            stackCollector.applyOnSamples(new Function<SampleNode, SampleNode>() {

                @Override
                public SampleNode apply(SampleNode input) {
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
                public SampleNode apply(SampleNode input) {
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
        stopped = true;
        samplingThread.join();
    }

    @Override
    public long getSampleTimeMillis() {
        return sampleTimeMillis;
    }

    @Override
    public void setSampleTimeMillis(long sampleTimeMillis) {
        this.sampleTimeMillis = sampleTimeMillis;
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    @Override
    public List<String> generate(Properties props) throws IOException {
        int width = Integer.valueOf(props.getProperty("width", "1200"));
        int maxDepth = Integer.valueOf(props.getProperty("maxDepth", "1200"));
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
    public void dispose() throws InterruptedException{
        stop();
        try {
            if (isJmxRegistered)
                ManagementFactory.getPlatformMBeanServer().unregisterMBean(name);
        } catch (InstanceNotFoundException ex) {
            throw new RuntimeException(ex);
        } catch (MBeanRegistrationException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void generateCpuSvg(String fileName, final int chartWidth, final int maxDepth) throws IOException {
        final Writer writer = new BufferedWriter(new OutputStreamWriter( new FileOutputStream(fileName), Charsets.UTF_8));
        try {

            stackCollector.applyOnSamples(new Function<SampleNode, SampleNode>() {

                @Override
                public SampleNode apply(SampleNode input) {
                    if (input != null) {
                        SampleNode finput = input.filteredBy(WaitMethodClassifier.INSTANCE);
                        try {
                            StackVisualizer.generateSvg(writer, Method.ROOT, finput,0,0 ,chartWidth, maxDepth, "a");
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
    public void generateTotalSvg(String fileName, final int chartWidth, final int maxDepth) throws IOException {
        final Writer writer = new BufferedWriter(new OutputStreamWriter( new FileOutputStream(fileName), Charsets.UTF_8));
        try {

            stackCollector.applyOnSamples(new Function<SampleNode, SampleNode>() {

                @Override
                public SampleNode apply(SampleNode input) {
                    if (input != null) {
                    
                        try {
                            StackVisualizer.generateSvg(writer, Method.ROOT, input,0,0 ,chartWidth, maxDepth, "b");
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
    public void generateSvgHtmlMonitorReport(String fileName, final int chartWidth, final int maxDepth) throws IOException {
        final Writer writer = new BufferedWriter(new OutputStreamWriter( new FileOutputStream(fileName), Charsets.UTF_8));
        try {
            writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
"<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.0//EN\" \"http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd\">\n");
            writer.append("<html>");

            
            stackCollector.applyOnSamples(new Function<SampleNode, SampleNode>() {

                @Override
                public SampleNode apply(SampleNode input) {
                    if (input != null) {
                       
                            try {
                                writer.append("<h1>Total stats</h1>");
                                 StackVisualizer.generateSvg(writer, Method.ROOT, input,0,0 ,chartWidth, maxDepth, "a");
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        
                    }
                    return input;
                }
            });
            
            
            stackCollector.applyOnSamples(new Function<SampleNode, SampleNode>() {

                @Override
                public SampleNode apply(SampleNode input) {
                    if (input != null) {
                        SampleNode finput = input.filteredBy(WaitMethodClassifier.INSTANCE);
                        if (finput != null) {
                            try {
                                writer.append("<h1>CPU stats</h1>");
                                 StackVisualizer.generateSvg(writer, Method.ROOT, finput,0,0 ,chartWidth, maxDepth, "b");
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
    
}
