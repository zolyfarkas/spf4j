
package org.spf4j.concurrent;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;

@State(Scope.Benchmark)
@Fork(2)
@Threads(value = 8)
public class ThreadPoolBenchmarkStdJdk {


   private static final class LazyStd {
       public static final ExecutorService EX = Executors.newFixedThreadPool(8);
   }


    @TearDown
    public void close() {
        LazyStd.EX.shutdown();
        DefaultExecutor.INSTANCE.shutdown();
    }


    @Benchmark
    public final void stdJdkBenchmark() throws InterruptedException, IOException, ExecutionException {
        ThreadPoolBenchmark.testPool(LazyStd.EX);
    }



}
