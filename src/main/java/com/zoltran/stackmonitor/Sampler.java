/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.stackmonitor;

import com.google.common.base.Function;
import java.io.*;
import java.lang.management.ManagementFactory;
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
@edu.umd.cs.findbugs.annotations.SuppressWarnings("I18N")
public class Sampler implements SamplerMBean {

    private volatile boolean stopped;
    private volatile long sampleTimeMillis;
    private final MxStackCollector stackCollector;
    @GuardedBy("this")
    private Thread samplingThread;
    

    public Sampler() {
        this(100);
    }

    public Sampler(long sampleTimeMillis) {
        stopped = true;
        this.sampleTimeMillis = sampleTimeMillis;
        this.stackCollector = new MxStackCollector(this);

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
                        stackCollector.sample();
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

    @Override
    public synchronized void generateHtmlMonitorReport(String fileName, final int chartWidth) throws IOException {

        final Writer writer = new BufferedWriter(new FileWriter(fileName));
        try {
            writer.append("<html>");

            stackCollector.applyOnCpuSamples(new Function<SampleNode, SampleNode>() {

                @Override
                public SampleNode apply(SampleNode input) {
                    if (input != null) {
                        try {
                            writer.append("<h1>CPU stats</h1>");
                            generateHtmlTable(writer, Method.ROOT, input, chartWidth);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                    return input;
                }
            });



            stackCollector.applyOnWaitSamples(new Function<SampleNode, SampleNode>() {

                @Override
                public SampleNode apply(SampleNode input) {
                    if (input != null) {
                        try {
                            writer.append("<h1>WAIT stats</h1>");
                            generateHtmlTable(writer, Method.ROOT, input, chartWidth);
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
    private static final String[] COLORS = {"#CCE01B",
        "#DDE01B", "#EEE01B", "#FFE01B", "#FFD01B",
        "#FFC01B", "#FFA01B", "#FF901B", "#FF801B",
        "#FF701B", "#FF601B", "#FF501B", "#FF401B"};

    private static void generateHtmlTable(Writer writer, Method m, SampleNode node, int tableWidth) throws IOException {
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
        String fileName = File.createTempFile("stack", ".html").getAbsolutePath();
        generateHtmlMonitorReport(fileName, width);
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

    public MxStackCollector getStackCollector() {
        return stackCollector;
    }
    
    
    
}
