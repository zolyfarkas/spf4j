package org.spf4j.concurrent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import junit.framework.Assert;
import org.junit.Test;
import org.spf4j.stackmonitor.FastStackCollector;

/**
 *
 * @author zoly
 */
public class LifoThreadPoolExecutorTest2 {

    public LifoThreadPoolExecutorTest2() {
    }

    @Test
    public void testLifoExecSQ() throws InterruptedException, IOException, ExecutionException {
        LifoThreadPoolExecutorSQP executor =
                new LifoThreadPoolExecutorSQP("test", 0, 8, 1000, 1024, 1024);
        testPoolThreadDynamics(executor);
    }

    @Test
    public void testJdkExec() throws InterruptedException, IOException, ExecutionException {
        final LinkedBlockingQueue linkedBlockingQueue = new LinkedBlockingQueue(1024);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 8, 1000, TimeUnit.MILLISECONDS,
                linkedBlockingQueue);
        testPoolThreadDynamics(executor);
    }


    public static void testPoolThreadDynamics(final ExecutorService executor)
            throws InterruptedException, IOException, ExecutionException {
        testMaxParallel(executor, 3);
        if (executor instanceof LifoThreadPoolExecutorSQP) {
            LifoThreadPoolExecutorSQP le = (LifoThreadPoolExecutorSQP) executor;
            Assert.assertEquals(3, le.getThreadCount());
            testMaxParallel(executor, 2);
            Assert.assertEquals(2, le.getThreadCount());
        }
        executor.shutdown();
        boolean awaitTermination = executor.awaitTermination(10000, TimeUnit.MILLISECONDS);
        Assert.assertTrue(awaitTermination);
    }

    public static void testMaxParallel(final ExecutorService executor, final int maxParallel)
            throws ExecutionException, InterruptedException {
        final LongAdder adder = new LongAdder();
        final int testCount = 1000;
        long rejected = 0;
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                adder.increment();
                long sleep = (long) (25 * Math.random());
                if (sleep > 0) {
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };
        long start = System.currentTimeMillis();
        List<Future<?>> futures = new ArrayList<>(maxParallel);
        for (int i = 0; i < testCount; i++) {
            try {
                futures.add(executor.submit(runnable));
            } catch (RejectedExecutionException ex) {
                rejected++;
                runnable.run();
            }
            if (i % maxParallel == 0) {
                for (Future fut : futures) {
                    fut.get();
                }
                futures.clear();
            }
        }
        for (Future fut : futures) {
            fut.get();
        }
        Assert.assertEquals(testCount, adder.sum());
        System.out.println("Stats for " + executor.getClass()
                + ", rejected = " + rejected + ", Exec time = " + (System.currentTimeMillis() - start));
        System.out.println("Threads: " + Arrays.toString(FastStackCollector.getThreads()));
    }





}
