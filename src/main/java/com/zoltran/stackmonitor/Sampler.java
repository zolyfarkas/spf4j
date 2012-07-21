/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.stackmonitor;

import com.google.common.base.Function;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.*;

/**
 * Utility that allow you to sample what the application is doing.
 * It generates a "Flame Graph" that allows you to quickly see you "heavy" 
 * operations.
 * 
 * You can use JConsole to control the sampling while your application is running.
 * 
 * By using a sampling approach you can choose your overhead.
 * (sampling takes about 0.5 ms, so the default of 10Hz will give you 0.5% overhead)
 * 
 * Collection is separated into CPU, WAIT and IO categories.
 * I felt that most important is to see what hogs your CPU because that is where,
 * you can most likely can do something about it.
 * 
 * @author zoly
 */
@ThreadSafe
@edu.umd.cs.findbugs.annotations.SuppressWarnings("I18N")
public class Sampler implements SamplerMBean {

    private volatile boolean stopped;
    private volatile long sampleTimeMillis;
    
    private final Object sampleSync = new Object();
    @GuardedBy("sampleSync")
    private SampleNode cpuSamples;
    @GuardedBy("sampleSync")
    private SampleNode waitSamples;
    @GuardedBy("this")
    private Thread samplingThread;
    
    private final ThreadMXBean threadMX = ManagementFactory.getThreadMXBean();
    
    
    public Sampler() {
        this(100);
    }

    public Sampler(long sampleTimeMillis) {
        stopped = true;
        this.sampleTimeMillis = sampleTimeMillis;
        cpuSamples = null;
        waitSamples = null;

    }

    @PostConstruct
    public void init() throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        ManagementFactory.getPlatformMBeanServer().registerMBean(this, new ObjectName("StackMonitor:name=StackSampler"));
    }

    @Override
    public synchronized void start() {
        if (stopped) {
            stopped = false;
            samplingThread = new Thread(new Runnable() {

                @SuppressWarnings("SleepWhileInLoop")
                public void run() {
                    while (!stopped) {
                        ThreadInfo [] stackDump = threadMX.dumpAllThreads(true, true);
                        recordStackDump(stackDump);
                        try {
                            Thread.sleep(sampleTimeMillis);
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

    private void recordStackDump(ThreadInfo [] stackDump) {
        synchronized (sampleSync) {
            for (ThreadInfo entry : stackDump) {
                StackTraceElement[] stackTrace = entry.getStackTrace();
                if (stackTrace.length > 0 && !(entry.getThreadId() == Thread.currentThread().getId())) {
                    Thread.State state = entry.getThreadState();
                    if (state == Thread.State.BLOCKED ||
                         state == Thread.State.TIMED_WAITING || state == Thread.State.WAITING   ) {
                        if (waitSamples == null) {
                            waitSamples = new SampleNode(stackTrace, stackTrace.length - 1);
                        } else {
                            waitSamples.addSample(stackTrace, stackTrace.length - 1);
                        }

                    }  else if (state == Thread.State.RUNNABLE){
                        if (cpuSamples == null) {
                            cpuSamples = new SampleNode(stackTrace, stackTrace.length - 1);
                        } else {
                            cpuSamples.addSample(stackTrace, stackTrace.length - 1);
                        }

                    }
                }
            }
        }
    }

    @Override
    public synchronized void generateHtmlMonitorReport(String fileName, int chartWidth) throws IOException {
        synchronized (sampleSync) {
            Writer writer = new FileWriter(fileName);
            try {
                writer.append("<html>");
                if (cpuSamples != null) {
                    writer.append("<h1>CPU stats</h1>");
                    generateCpuHtmlTable(writer, chartWidth);
                }
                if (waitSamples != null) {
                    writer.append("<h1>WAIT stats</h1>");
                    generateWaitHtmlTable(writer, chartWidth);
                }
                writer.append("</html>");

            } finally {
                writer.close();
            }
        }

    }

    public boolean generateCpuHtmlTable(Writer writer, int width) throws IOException {
        synchronized (sampleSync) {
            if (cpuSamples != null) {
                generateHtmlTable(writer, Method.ROOT, cpuSamples, width);
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean generateWaitHtmlTable(Writer writer, int width) throws IOException {
        synchronized (sampleSync) {
            if (waitSamples != null) {
                generateHtmlTable(writer, Method.ROOT, waitSamples, width);
                return true;
            } else {
                return false;
            }
        }
    }

    private static final String[] COLORS = {"#CCE01B",
        "#DDE01B", "#EEE01B", "#FFE01B", "#FFD01B",
        "#FFC01B", "#FFA01B", "#FF901B", "#FF801B",
        "#FF701B", "#FF601B", "#FF501B", "#FF401B"};

    private void generateHtmlTable(Writer writer, Method m, SampleNode node, int tableWidth) throws IOException {
        writer.append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"overflow:hidden;table-layout:fixed;width:").
                append(Integer.toString(tableWidth)).append("\"><tr><td title=\"");
        m.toWriter(writer);
        writer.append("\" style=\"vertical-align:top ;background:").
                append(COLORS[(int) (Math.random() * COLORS.length)]).append("\">");
        m.toWriter(writer);
        writer.append("</td></tr>\n");
        int totalSamples = node.getCount();

        Map<Method, SampleNode> subNodes = node.getSubNodes();
        if (subNodes != null) {
            writer.append("<tr><td><table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"overflow:hidden;table-layout:fixed\"><tr>\n");
            for (Map.Entry<Method, SampleNode> entry : subNodes.entrySet()) {
                int width = entry.getValue().getCount() * tableWidth / totalSamples;
                writer.append("<td style=\"vertical-align:top; width:").append(Integer.toString(width)).append("px\">");
                generateHtmlTable(writer, entry.getKey(), entry.getValue(), width);
                writer.append("</td>");
            }
            writer.append("</tr></table></td></tr>");
        }
        writer.append("</table>\n");
    }

    @PreDestroy
    @Override
    public synchronized void stop() throws InterruptedException {
        stopped = true;
        samplingThread.join();
    }

    public void clear() {
        synchronized (sampleSync) {
            cpuSamples = null;
            waitSamples = null;
        }
    }

    public long getSampleTimeMillis() {
        return sampleTimeMillis;
    }

    public void setSampleTimeMillis(long sampleTimeMillis) {
        this.sampleTimeMillis = sampleTimeMillis;
    }

    public boolean isStopped() {
        return stopped;
    }

    public SampleNode applyOnCpuSamples(Function<SampleNode, SampleNode> predicate) {
        synchronized (sampleSync) {
            return cpuSamples = predicate.apply(cpuSamples);
        }
    }

    public SampleNode applyOnWaitSamples(Function<SampleNode, SampleNode> predicate) {
        synchronized (sampleSync) {
            return waitSamples = predicate.apply(waitSamples);
        }
    }

    @Override
    public List<String> generate(Properties props) throws IOException {
           int width = Integer.valueOf(props.getProperty("width", "1200"));
           String fileName = File.createTempFile("stack", ".html").getAbsolutePath();
           generateHtmlMonitorReport(fileName, width);
           return Arrays.asList(fileName);
    }

    @Override
    public List<String> getParameters() {
        return Arrays.asList("width");
    }

}
