package org.spf4j.concurrent;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;

@Fork(2)
@Threads(value = 8)
public class ThreadPoolBenchmark {

  @State(Scope.Benchmark)
  public static class LazySpf {

    public final ExecutorService EX = LifoThreadPoolBuilder.newBuilder()
            .withQueueSizeLimit(10000)
            .withMaxSize(8).build();

    @TearDown
    public void close() {
      EX.shutdown();
      DefaultExecutor.INSTANCE.shutdown();
    }

  }

  public static long testPool(final ExecutorService executor)
          throws InterruptedException, IOException, ExecutionException {
    final LongAdder adder = new LongAdder();
    final int testCount = 1000;
    final Runnable runnable = new Runnable() {
      @Override
      public void run() {
        adder.increment();
      }
    };
    Future[] futures = new Future[testCount];
    for (int i = 0; i < testCount; i++) {
      futures[i] = executor.submit(runnable);
    }
    for (Future<?> future : futures) {
      future.get();
    }
    long longValue = adder.longValue();
    if (longValue != (long) testCount) {
      throw new RuntimeException("Something is wrong with thread pool " + longValue);
    }
    return longValue;
  }

  @Benchmark
  public final long spfLifoTpBenchmark(final LazySpf exec) throws InterruptedException, IOException, ExecutionException {
    return testPool(exec.EX);
  }

}
