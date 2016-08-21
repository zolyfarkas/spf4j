package org.spf4j.base;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

/**
 *
 * @author zoly
 */
@State(Scope.Benchmark)
@Fork(2)
@Threads(value = 8)
public class RandomBenchmark {

  private static final IntMath.XorShift32ThreadSafe RND = new IntMath.XorShift32ThreadSafe();

  @State(Scope.Thread)
  public static class ThreadState {

    final IntMath.XorShift32 rnd = new IntMath.XorShift32();

  }

  @State(Scope.Thread)
  public static class ThreadState2 {

    final ThreadLocalRandom rnd = ThreadLocalRandom.current();

  }


  @Benchmark
  public int testSpf4jRandomThreadLocal() throws IOException {
    return RND.nextInt();
  }

  @Benchmark
  public int testSpf4jRandomLocal(ThreadState ts) throws IOException {
    return ts.rnd.nextInt();
  }

  @Benchmark
  public int testJdkRandomLocal(ThreadState2 ts) throws IOException {
    return ts.rnd.nextInt();
  }

  @Benchmark
  public int testJdkRandomThreadLocal() throws IOException {
    return ThreadLocalRandom.current().nextInt();
  }


}
