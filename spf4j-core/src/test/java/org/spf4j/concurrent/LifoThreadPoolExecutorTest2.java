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
                new LifoThreadPoolExecutorSQP("test", 2, 8, 1000, 1024, 1024);
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
        testMaxParallel(executor, 4);
        if (executor instanceof LifoThreadPoolExecutorSQP) {
            LifoThreadPoolExecutorSQP le = (LifoThreadPoolExecutorSQP) executor;
            Assert.assertEquals(4, le.getThreadCount());
            testMaxParallel(executor, 2);
            Assert.assertEquals(2, le.getThreadCount());
        }
        executor.shutdown();
        boolean awaitTermination = executor.awaitTermination(10000, TimeUnit.MILLISECONDS);
        Assert.assertTrue(awaitTermination);
    }

    public static void testMaxParallel(final ExecutorService executor, final int maxParallel)
            throws  InterruptedException {
        final LongAdder adder = new LongAdder();
        final LongAdder exNr = new LongAdder();
        int nrExCaught = 0;
        final int testCount = 1000;
        long rejected = 0;
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                adder.increment();
                long sleep = (long) (50 * Math.random());
                if (sleep < 10) {
                    exNr.increment();
                    throw new RuntimeException();
                }
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
            futures.add(executor.submit(runnable));
            if (i % maxParallel == 0) {
                nrExCaught += consume(futures);
            }
        }
        nrExCaught += consume(futures);
        Assert.assertEquals(testCount, adder.sum());
        Assert.assertEquals(nrExCaught, exNr.sum());
        System.out.println("Stats for " + executor.getClass()
                + ", rejected = " + rejected + ", Exec time = " + (System.currentTimeMillis() - start));
        System.out.println("Threads: " + Arrays.toString(FastStackCollector.getThreads()));
    }

    public static int consume(List<Future<?>> futures) throws InterruptedException {
        int nrEx = 0;
        for (Future fut : futures) {
            try {
                fut.get();
            } catch (ExecutionException ex) {
                nrEx++;
            }
        }
        futures.clear();
        return nrEx;
    }


}
