/**
 * (c) Zoltan Farkas 2012
 */

package com.zoltran.stackmonitor;

import com.google.common.base.Function;
import com.google.protobuf.CodedInputStream;
import com.zoltran.stackmonitor.proto.Converter;
import com.zoltran.stackmonitor.proto.gen.ProtoSampleNodes;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.kohsuke.args4j.CmdLineException;

public class MonitorTest {

    @BeforeClass
    public static void init() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                StringWriter strw = new StringWriter();
                e.printStackTrace(new PrintWriter(strw));
                Assert.fail("Got Exception: " + strw.toString());
            }
        });
    }
    
    
    @Test
    @Ignore
    public void testJmx() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, CmdLineException, InterruptedException, MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        String report = File.createTempFile("stackSample", ".html").getPath();
        Monitor.main(new String[]{"-f",report, "-ss", "-si", "10", "-w","600", "-main", MonitorTest.class.getName()});
        System.out.println(report);
        Thread.sleep(100000);
    }
    
    
    @Test(timeout=20000)
    public void testApp() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, CmdLineException, MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, InterruptedException {
        String report = File.createTempFile("stackSample", ".html").getPath();
        Monitor.main(new String[]{"-f",report, "-ss", "-si", "10", "-w","600", "-main", MonitorTest.class.getName()});
        System.out.println(report);
    }
    
    @Test
    @Ignore
    public void testProto() throws InterruptedException, MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, IOException {
        
        Sampler sampler = new Sampler(1);
        sampler.init();
        sampler.start();
        MonitorTest.main(new String [] {});
        String serializedFile = File.createTempFile("stackSample", ".samp").getPath();
        final FileOutputStream os = new FileOutputStream(serializedFile);
        try  {
            sampler.applyOnCpuSamples(new Function<SampleNode, SampleNode> () {

                public SampleNode apply(SampleNode f) {
                    try {
                        Converter.fromSampleNodeToProto(f).writeTo(os);
                    } catch (IOException ex) {
                       throw new RuntimeException(ex);
                    }
                    return f;
                }

            });
        } finally {
            os.close();
        }
        sampler.stop();
        Sampler anotherOne = new Sampler(100);        
        FileInputStream fis = new FileInputStream(serializedFile);
        try {
            final CodedInputStream is = CodedInputStream.newInstance(fis);
            is.setRecursionLimit(1024);
            anotherOne.applyOnCpuSamples(new Function<SampleNode, SampleNode>() {

                public SampleNode apply(SampleNode f) {
                    try {
                        return Converter.fromProtoToSampleNode( ProtoSampleNodes.SampleNode.parseFrom(is) );
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }

            });     
        } finally {
           fis.close(); 
        }
        String report = File.createTempFile("stackSample", ".html").getPath();
        anotherOne.generateHtmlMonitorReport(report, 1000);
        System.out.println(report);    
    }
    
    
    
    private static volatile boolean stopped;

    public static void main(String[] args) throws InterruptedException {
        stopped = false;
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < 20; i++) {
            Thread t = new Thread(new Runnable() {

                public void run() {
                    try {
                        while (!stopped) {
                            double rnd = Math.random();
                            if (rnd < 0.33) {                                
                                    doStuff1(rnd);                                
                            } else if (rnd < 0.66) {
                                doStuff2(rnd);
                            } else {
                                doStuff3(rnd);
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                }

                private double doStuff3(double rnd) throws InterruptedException{                
                    Thread.sleep(1);
                    return  rnd * Math.pow(2, 10000);
                }

                private double doStuff2(double rnd) throws InterruptedException {
                    Thread.sleep(1);
                    for (int i = 0; i < 10000; i++) {
                        rnd = rnd + rnd;
                    }
                    return rnd;
                }

                private void doStuff1(double rnd) throws InterruptedException {
                    Thread.sleep(10);
                    System.out.println("Rnd:" + rnd);
                }
            }, "Thread" + i);
            t.start();
            threads.add(t);
        }
        Thread.sleep(5000);
        stopped = true;
        for(Thread t: threads)
            t.join();
        
    }
}
