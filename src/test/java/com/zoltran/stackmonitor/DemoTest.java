/**
 * (c) Zoltan Farkas 2012
 */

package com.zoltran.stackmonitor;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import javax.management.*;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kohsuke.args4j.CmdLineException;

public class DemoTest {

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
    public void testJmx() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, CmdLineException, InterruptedException, MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        String report = File.createTempFile("stackSampleDemo", ".html").getPath();
        Monitor.main(new String[]{"-simple","-f",report, "-ss", "-si", "1", "-w","600", "-main", DemoTest.class.getName()});
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
                            doStuff();
                            Thread.sleep(1);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                }

                private double doStuff() {                
                    return getStuff(10) * getStuff(10) * getStuff(10) * getStuff(10);
                }
                
                private double getStuff(double nr) {
                    return Math.exp(nr);
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
