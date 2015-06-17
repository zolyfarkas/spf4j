package org.spf4j.concurrent;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class LifoThreadPoolExecutorTest {

    public LifoThreadPoolExecutorTest() {
    }


    @Test
    public void testLifoExecUS() throws InterruptedException, IOException {
        int nrProcs = org.spf4j.base.Runtime.NR_PROCESSORS;
        LifoThreadPoolExecutorUS executor = new LifoThreadPoolExecutorUS(nrProcs / 4, nrProcs, 60000,
                new LinkedBlockingQueue(1024));
        testPool(executor);
    }

    @Test
    public void testLifoExec() throws InterruptedException, IOException {
        int nrProcs = org.spf4j.base.Runtime.NR_PROCESSORS;
        LifoThreadPoolExecutor executor = new LifoThreadPoolExecutor(nrProcs / 4, nrProcs, 60000,
                new LinkedBlockingQueue(1024));
        testPool(executor);
    }

    @Test
    public void testLifoExecSQ() throws InterruptedException, IOException {
        int nrProcs = org.spf4j.base.Runtime.NR_PROCESSORS;
        LifoThreadPoolExecutorSQ executor = new LifoThreadPoolExecutorSQ(nrProcs / 4, nrProcs, 60000,
                new LinkedBlockingQueue(1024));
        testPool(executor);
    }


    @Test
    public void testJdkExec() throws InterruptedException, IOException {
        int nrProcs = org.spf4j.base.Runtime.NR_PROCESSORS;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(nrProcs / 4, nrProcs, 60000, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue(1024));
        testPool(executor);
    }


    public static void testPool(final ExecutorService executor) throws InterruptedException, IOException {
        final LongAdder adder = new LongAdder();
        final int testCount = 1000000;
        for (int i = 0; i < testCount; i++) {
            try {
                executor.submit(new Runnable() {

                    @Override
                    public void run() {
                        adder.increment();
                    }
                });
            } catch (RejectedExecutionException ex) {
                adder.increment();
            }
        }
        executor.shutdown();
        boolean awaitTermination = executor.awaitTermination(10000, TimeUnit.MILLISECONDS);
        Assert.assertTrue(awaitTermination);
        Assert.assertEquals(testCount, adder.sum());
    }




}
