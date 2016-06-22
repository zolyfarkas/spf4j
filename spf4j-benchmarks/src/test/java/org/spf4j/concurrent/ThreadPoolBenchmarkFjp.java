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

@Fork(2)
@Threads(value = 8)
public class ThreadPoolBenchmarkFjp {

  @State(Scope.Benchmark)
  public static class LazyFjp {

    public final ExecutorService EX = new ForkJoinPool(8);

    @TearDown
    public void close() {
      EX.shutdown();
      DefaultExecutor.INSTANCE.shutdown();
    }
  }

  @Benchmark
  public final long fjpBenchmark(final LazyFjp exec) throws InterruptedException, IOException, ExecutionException {
    return ThreadPoolBenchmark.testPool(exec.EX);
  }

}
