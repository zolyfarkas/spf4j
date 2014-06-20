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

import java.util.List;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class SimpleStackCollectorTest {

    /**
     * Test of sample method, of class SimpleStackCollector.
     */
    @Test
    public void testSample() {
        System.out.println("sample");
        SimpleStackCollector instance = new SimpleStackCollector();
        StackTraceElement[] st1 = new StackTraceElement[3];
        st1[0] = new StackTraceElement("C1", "m1", "C1.java", 10);
        st1[1] = new StackTraceElement("C1", "m2", "C1.java", 11);
        st1[2] = new StackTraceElement("C1", "m3", "C1.java", 12);
        instance.addSample(st1);
        System.out.println(instance);
        Assert.assertEquals(4, instance.getNrNodes());

        StackTraceElement[] st2 = new StackTraceElement[1];
        st2[0] = new StackTraceElement("C1", "m1", "C1.java", 10);
        instance.addSample(st2);
        System.out.println(instance);
        Assert.assertEquals(5, instance.getNrNodes());

        StackTraceElement[] st3 = new StackTraceElement[3];
        st3[0] = new StackTraceElement("C2", "m1", "C2.java", 10);
        st3[1] = new StackTraceElement("C2", "m2", "C2.java", 11);
        st3[2] = new StackTraceElement("C2", "m3", "C2.java", 12);
        instance.addSample(st3);
        System.out.println(instance);

        StackTraceElement[] st4 = new StackTraceElement[3];
        st4[0] = new StackTraceElement("C1", "m1", "C1.java", 10);
        st4[1] = new StackTraceElement("C1", "m2", "C1.java", 11);
        st4[2] = new StackTraceElement("C1", "m4", "C1.java", 14);
        instance.addSample(st4);

        System.out.println(instance);
        
        Assert.assertTrue(Thread.currentThread().getStackTrace()[0].getClassName()
                == Thread.currentThread().getStackTrace()[0].getClassName());
        Assert.assertTrue(Thread.currentThread().getStackTrace()[0].getMethodName()
                == Thread.currentThread().getStackTrace()[0].getMethodName());
        
    }
    
    @Test
    @Ignore
    public void testPerformance() throws InterruptedException {
        List<Thread> startTestThreads = DemoTest.startTestThreads(50);
        SimpleStackCollector simple = new SimpleStackCollector();
        FastStackCollector mx = new FastStackCollector();
        testPerf(simple, 10000);
        testPerf(mx, 10000);
        System.gc();
        Thread.sleep(1000);
        long simpleTime = testPerf(simple, 20000);
        long mxTime = testPerf(mx, 20000);
        
        System.out.println("mx = " + mxTime);
        System.out.println("simple = " + simpleTime);
        Assert.assertTrue("cond " + mxTime + " <= " + simpleTime,
                mxTime <= simpleTime);
        double relativeDiff = Math.abs(1 - (double) mxTime / simpleTime);
        System.out.println("relDiff = " + relativeDiff);
        Assert.assertTrue(relativeDiff > 0.05); // should be at least a 5% improovement
        DemoTest.stopTestThreads(startTestThreads);
    }
    
    private static long testPerf(final StackCollector collector, final int count)  {
        long startTime = System.currentTimeMillis();
        Thread current = Thread.currentThread();
        for (int i = 0; i < count; i++) {
            collector.sample(current);
        }
        return System.currentTimeMillis() - startTime;
    }
    
    
}
