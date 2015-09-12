
package org.spf4j.concurrent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;

@State(Scope.Benchmark)
@Fork(2)
@Threads(value = 8)
public class ThreadPoolBenchmark {



   private static final class LazySpf {
       public static final ExecutorService EX = LifoThreadPoolBuilder.newBuilder()
               .withQueueSizeLimit(10000)
               .withMaxSize(8).build();
   }

    @TearDown
    public void close() {
        LazySpf.EX.shutdown();
        DefaultExecutor.INSTANCE.shutdown();
    }


   public static void testPool(final ExecutorService executor)
            throws InterruptedException, IOException, ExecutionException {
        final LongAdder adder = new LongAdder();
        final int testCount = 1000;
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                adder.increment();
            }
        };
        List<Future<?>> futures = new ArrayList<>(testCount);
        for (int i = 0; i < testCount; i++) {
            futures.add(executor.submit(runnable));
        }
        for (Future<?> future : futures) {
            future.get();
        }
        if (adder.longValue() != (long) testCount) {
            throw new RuntimeException("Something is wrong with thread pool " + adder.longValue());
        }
   }

    @Benchmark
    public final void spfLifoTpBenchmark() throws InterruptedException, IOException, ExecutionException {
        testPool(LazySpf.EX);
    }


}
