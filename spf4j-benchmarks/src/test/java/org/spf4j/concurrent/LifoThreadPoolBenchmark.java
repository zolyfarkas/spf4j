package org.spf4j.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import junit.framework.Assert;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

@State(Scope.Benchmark)
@Fork(2)
@Threads(value = 1)
public class LifoThreadPoolBenchmark {

    @Benchmark
    public void testLifoExec() throws InterruptedException {
        int nrProcs = org.spf4j.base.Runtime.NR_PROCESSORS;
        LifoThreadPoolExecutorSQP executor = new LifoThreadPoolExecutorSQP(nrProcs / 4, nrProcs, 60000,
                1024);
        testPool(executor);
    }


    @Benchmark
    public void testJdkExec() throws InterruptedException {
        int nrProcs = org.spf4j.base.Runtime.NR_PROCESSORS;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(nrProcs / 4, nrProcs, 60000, TimeUnit.MILLISECONDS,
                new LinkedBlockingDeque(1024));
        testPool(executor);
    }

    public static void testPool(final ExecutorService executor) throws InterruptedException {
        final LongAdder adder = new LongAdder();
        final int testCount = 10000;
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
