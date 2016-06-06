
/*
 * Copyright (c) 2015, Zoltan Farkas All Rights Reserved.
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
package org.spf4j.concurrent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.spf4j.base.Throwables;

/**
 *
 * @author zoly
 */
public class LifoThreadPoolExecutorTest {

    @Test
    public void testLifoExecSQ() throws InterruptedException, IOException {
        LifoThreadPoolExecutorSQP executor =
                new LifoThreadPoolExecutorSQP("test", 8, 8, 60000, 1024, 1024);
        testPool(executor);
    }

    @Test
    public void testLifoExecSQZeroQueue() throws InterruptedException, IOException {
        LifoThreadPoolExecutorSQP executor =
                new LifoThreadPoolExecutorSQP("test", 0, 16, 60000, 0, 1024);
        testPool(executor);
    }

    @Test
    public void testLifoExecSQShutdownNow() throws InterruptedException, IOException {
        LifoThreadPool executor =
                LifoThreadPoolBuilder.newBuilder().withCoreSize(2).withMaxSize(8).withQueueSizeLimit(1024).build();
        executor.execute(new Runnable() {

            @Override
            @SuppressFBWarnings("MDM_THREAD_YIELD")
            public void run() {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException ex) {
                    Throwables.writeTo(ex, System.err, Throwables.Detail.STANDARD);
                }
            }
        });

        executor.execute(new Runnable() {

            @Override
            @SuppressFBWarnings("MDM_THREAD_YIELD")
            public void run() {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        executor.shutdown();
        Assert.assertFalse(executor.awaitTermination(10, TimeUnit.MILLISECONDS));
        executor.shutdownNow();
        Assert.assertTrue(executor.awaitTermination(1000, TimeUnit.MILLISECONDS));

    }


    @Test
    @Ignore
    public void testJdkExec() throws InterruptedException, IOException {
        final LinkedBlockingQueue linkedBlockingQueue = new LinkedBlockingQueue(1024);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 8, 60000, TimeUnit.MILLISECONDS,
                linkedBlockingQueue);
        testPool(executor);
    }

    @Test
    @Ignore
    public void testJdkFJPExec() throws InterruptedException, IOException {
        ExecutorService executor = new ForkJoinPool(8);
        testPool(executor);
    }


    public static void testPool(final ExecutorService executor)
            throws InterruptedException, IOException {
        final LongAdder adder = new LongAdder();
        final int testCount = 20000000;
        long rejected = 0;
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                adder.increment();
            }
        };
        long start = System.currentTimeMillis();
        executor.execute(new Runnable() {

            @Override
            public void run() {
                throw new RuntimeException();
            }
        });
        for (int i = 0; i < testCount; i++) {
            try {
                executor.execute(runnable);
            } catch (RejectedExecutionException ex) {
                rejected++;
                runnable.run();
            }
        }
        executor.shutdown();
        boolean awaitTermination = executor.awaitTermination(10000, TimeUnit.MILLISECONDS);
        System.out.println("Stats for " + executor.getClass()
                + ", rejected = " + rejected + ", Exec time = " + (System.currentTimeMillis() - start));
        Assert.assertTrue(awaitTermination);
        Assert.assertEquals(testCount, adder.sum());
    }

}
