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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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
import org.spf4j.base.AbstractRunnable;

public final class MonitorTest {

    @BeforeClass
    public static void init() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(final Thread t, final Throwable e) {
                StringWriter strw = new StringWriter();
                e.printStackTrace(new PrintWriter(strw));
                Assert.fail("Got Exception: " + strw.toString());
            }
        });
    }

    @Test
    @Ignore
    public void testError() throws ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException,
            IOException, CmdLineException, InterruptedException, MalformedObjectNameException,
            InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        String report = File.createTempFile("stackSample", ".html").getPath();
        Monitor.main(new String[]{"-ASDF", "-f", report, "-ss", "-si", "10", "-w", "600", "-main",
            MonitorTest.class.getName()});
        System.out.println(report);
    }

    @Test
    public void testJmx() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, IOException, CmdLineException,
            InterruptedException, MalformedObjectNameException, InstanceAlreadyExistsException,
            MBeanRegistrationException, NotCompliantMBeanException {
        String report = File.createTempFile("stackSample", ".html").getPath();
        Monitor.main(new String[]{"-ss", "-si", "10", "-main",
            MonitorTest.class.getName()});
        System.out.println(report);
    }




    private static volatile boolean stopped;

    public static void main(final String[] args) throws InterruptedException {
        stopped = false;
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < 20; i++) {
            Thread t = new Thread(new AbstractRunnable() {
                @Override
                public void doRun() throws InterruptedException {
                        while (!stopped) {
                            double rnd = Math.random();
                            if (rnd < 0.33) {
                                doStuff1(rnd, 50);
                            } else if (rnd < 0.66) {
                                doStuff2(rnd);
                            } else {
                                doStuff3(rnd);
                            }
                        } 
                }

                private double doStuff3(final double rnd) throws InterruptedException {
                    Thread.sleep(1);
                    if (rnd > 0.8) {
                        doStuff2(rnd);
                    }
                    return rnd * Math.pow(2, 10000);
                }

                private double doStuff2(final double prnd) throws InterruptedException {
                    double rnd = prnd;
                    Thread.sleep(1);
                    for (int j = 0; j < 10000; j++) {
                        rnd = rnd + rnd;
                    }
                    return rnd;
                }

                private void doStuff1(final double rnd, final int depth) throws InterruptedException {
                    if (depth <= 0) {
                        Thread.sleep(10);
                        System.out.println("Rnd:" + rnd);
                    } else {
                        doStuff1(rnd, depth - 1);
                    }
                }
            }, "Thread" + i);
            t.start();
            threads.add(t);
        }
        Thread.sleep(5000);
        stopped = true;
        for (Thread t : threads) {
            t.join();
        }

    }
}
