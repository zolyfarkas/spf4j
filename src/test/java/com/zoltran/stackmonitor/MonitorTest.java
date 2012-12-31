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

package com.zoltran.stackmonitor;

import com.google.common.base.Function;
import com.google.protobuf.CodedInputStream;
import com.zoltran.stackmonitor.proto.Converter;
import com.zoltran.stackmonitor.proto.gen.ProtoSampleNodes;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import javax.management.*;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.kohsuke.args4j.CmdLineException;

public class MonitorTest {

    @BeforeClass
    public static void init() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                StringWriter strw = new StringWriter();
                e.printStackTrace(new PrintWriter(strw));
                Assert.fail("Got Exception: " + strw.toString());
            }
        });
    }
    

    @Test
    @Ignore
    public void testError() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, CmdLineException, InterruptedException, MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        String report = File.createTempFile("stackSample", ".html").getPath();
        Monitor.main(new String[]{"-ASDF","-f",report, "-ss", "-si", "10", "-w","600", "-main", MonitorTest.class.getName()});
        System.out.println(report);
    }
    
    
    @Test
    public void testJmx() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, CmdLineException, InterruptedException, MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        String report = File.createTempFile("stackSample", ".html").getPath();
        Monitor.main(new String[]{"-f",report, "-ss", "-si", "10", "-w","600", "-main", MonitorTest.class.getName()});
        System.out.println(report);
    }
    
  
    @Test(timeout=20000)
    @Ignore
    public void testApphtml() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, CmdLineException, MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, InterruptedException, InstanceNotFoundException {
        String report = File.createTempFile("stackSampleHtml", ".html").getPath();
        Monitor.main(new String[]{"-nosvg", "-f",report, "-ss", "-si", "10", "-w","600", "-main", MonitorTest.class.getName()});
        System.out.println(report);
    }
    
    
    @Test(timeout=20000)
    @Ignore
    public void testApp2() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, CmdLineException, MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, InterruptedException {
        String report = File.createTempFile("stackSampleSimple", ".html").getPath();
        Monitor.main(new String[]{"-f",report, "-ss", "-si", "10", "-w","600", "-main", MonitorTest.class.getName()});
        System.out.println(report);
    }
    
    @Test(timeout=20000)
    @Ignore
    public void testApp() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, CmdLineException, MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, InterruptedException, InstanceNotFoundException {
        String report = File.createTempFile("stackSample", ".html").getPath();
        Monitor.main(new String[]{"-f",report, "-ss", "-si", "10", "-w","600", "-main", MonitorTest.class.getName()});
        System.out.println(report);
    }
    
    
    @Test
    @Ignore
    public void testProto() throws InterruptedException, MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, IOException {
        
        Sampler sampler = new Sampler(1);
        sampler.registerJmx();
        sampler.start();
        MonitorTest.main(new String [] {});
        String serializedFile = File.createTempFile("stackSample", ".samp").getPath();
        final FileOutputStream os = new FileOutputStream(serializedFile);
        try  {
            sampler.getStackCollector().applyOnSamples(new Function<SampleNode, SampleNode> () {

                @Override
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
            anotherOne.getStackCollector().applyOnSamples(new Function<SampleNode, SampleNode>() {

                @Override
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
        anotherOne.generateHtmlMonitorReport(report, 1000, 25);
        System.out.println(report);    
    }
    
    
    
    private static volatile boolean stopped;

    public static void main(String[] args) throws InterruptedException {
        stopped = false;
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < 20; i++) {
            Thread t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        while (!stopped) {
                            double rnd = Math.random();
                            if (rnd < 0.33) {                                
                                    doStuff1(rnd,50);                                
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

                private void doStuff1(double rnd, int depth) throws InterruptedException {
                    if (depth<=0) {
                        Thread.sleep(10);
                        System.out.println("Rnd:" + rnd);
                    } else {
                        doStuff1(rnd, depth -1);
                    }
                }
            }, "Thread" + i);
            t.start();
            threads.add(t);
        }
        Thread.sleep(5000);
        stopped = true;
        for(Thread t: threads) {
            t.join();
        }
        
    }
}
