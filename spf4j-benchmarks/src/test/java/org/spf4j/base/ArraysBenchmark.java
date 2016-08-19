
package org.spf4j.base;

import java.io.IOException;
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
@Threads(value = 4)
public class ArraysBenchmark {

  @State(Scope.Thread)
  public static class ThreadState {
    String [] testArray = new String[1000];
    {
      java.util.Arrays.fill(testArray, "a");
    }
  }

    @Benchmark
    public void testSpf4jFillLarge(final ThreadState ts) throws IOException {
        org.spf4j.base.Arrays.fill(ts.testArray, 0, 1000, null);
    }

    @Benchmark
    public void testjdkFillLarge(final ThreadState ts) throws IOException {
        java.util.Arrays.fill(ts.testArray, 0, 1000, null);
    }

    @Benchmark
    public void testSpf4jFillSmall(final ThreadState ts) throws IOException {
        org.spf4j.base.Arrays.fill(ts.testArray, 0, 10, null);
    }

    @Benchmark
    public void testjdkFillSmall(final ThreadState ts) throws IOException {
        java.util.Arrays.fill(ts.testArray, 0, 10, null);
    }


    @Benchmark
    public void testSpf4jFillMedium(final ThreadState ts) throws IOException {
        org.spf4j.base.Arrays.fill(ts.testArray, 0, 100, null);
    }

    @Benchmark
    public void testjdkFillMedium(final ThreadState ts) throws IOException {
        java.util.Arrays.fill(ts.testArray, 0, 100, null);
    }


}
