
package org.spf4j.io;

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
    String [] testArray = new String[1024];
    {
      java.util.Arrays.fill(testArray, "a");
    }
  }

    @Benchmark
    public void testSpf4jFill(final ThreadState ts) throws IOException {
        org.spf4j.base.Arrays.fill(ts.testArray, 0, 1024, null);
    }

    @Benchmark
    public void testjdkFill(final ThreadState ts) throws IOException {
        java.util.Arrays.fill(ts.testArray, 0, 1024, null);
    }




}
