package org.spf4j.concurrent;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
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
    public void testLifoExecSQ() throws InterruptedException, IOException {
        int nrProcs = org.spf4j.base.Runtime.NR_PROCESSORS;
        final LinkedBlockingQueue linkedBlockingQueue = new LinkedBlockingQueue(1024);
        LifoThreadPoolExecutorSQP executor =
                new LifoThreadPoolExecutorSQP(nrProcs / 4, nrProcs, 60000, linkedBlockingQueue, 1024);
        testPool(executor, linkedBlockingQueue);
    }

    @Test
    public void testJdkExec() throws InterruptedException, IOException {
        int nrProcs = org.spf4j.base.Runtime.NR_PROCESSORS;
        final LinkedBlockingQueue linkedBlockingQueue = new LinkedBlockingQueue(1024);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(nrProcs / 4, nrProcs, 60000, TimeUnit.MILLISECONDS,
                linkedBlockingQueue);
        testPool(executor, linkedBlockingQueue);
    }

    public static void testPool(final ExecutorService executor, final BlockingQueue queue)
            throws InterruptedException, IOException {
        final LongAdder adder = new LongAdder();
        final int testCount = 20000000;
        long qSize = 0;
        long count = 0;
        long rejected = 0;
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                adder.increment();
//                try {
//                    Thread.sleep(1);
//                } catch (InterruptedException ex) {
//                    throw new RuntimeException(ex);
//                }
            }
        };
        for (int i = 0; i < testCount; i++) {
            try {
                executor.submit(runnable);
            } catch (RejectedExecutionException ex) {
                rejected++;
                runnable.run();
            }
            if (i % 200 == 0) {
                qSize += queue.size();
                count++;
            }
        }
        executor.shutdown();
        boolean awaitTermination = executor.awaitTermination(10000, TimeUnit.MILLISECONDS);
        System.out.println("Stats for " + executor.getClass() + " avgQSize = " + qSize / count
                + ", rejected = " + rejected);
        Assert.assertTrue(awaitTermination);
        Assert.assertEquals(testCount, adder.sum());
    }

}
