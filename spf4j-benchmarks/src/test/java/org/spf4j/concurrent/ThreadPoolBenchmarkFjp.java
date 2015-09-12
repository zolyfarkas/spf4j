
package org.spf4j.concurrent;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;

@State(Scope.Benchmark)
@Fork(2)
@Threads(value = 8)
public class ThreadPoolBenchmarkFjp {


   private static final class LazyFjp {
       public static final ExecutorService EX = new ForkJoinPool(8);
   }


    @TearDown
    public void close() {
        LazyFjp.EX.shutdown();
        DefaultExecutor.INSTANCE.shutdown();
    }


    @Benchmark
    public final void fjpBenchmark() throws InterruptedException, IOException, ExecutionException {
        ThreadPoolBenchmark.testPool(LazyFjp.EX);
    }



}
