package com.google.common.io;

import java.io.IOException;
import java.io.Writer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

/**
 *
 * @author zoly
 */
@Fork(2)
@Threads(value = 8)
public class AppendableWriterBenchmark {

  @State(Scope.Benchmark)
  public static class BenchmarkState {

    public final char[] TEST_CHARS;

    {
      StringBuilder builder = new StringBuilder(1000);
      for (int i = 0; i < 1000; i++) {
        builder.append('A');
      }
      TEST_CHARS = builder.toString().toCharArray();
    }
  }

  @State(Scope.Thread)
  public static class ThreadState {
    public final StringBuilder sb = new StringBuilder(100000);
  }

  @Benchmark
  public final StringBuilder guavaAppendable(final BenchmarkState bs, final ThreadState ts) throws IOException {
    StringBuilder stringBuilder = ts.sb;
    stringBuilder.setLength(0);
    Writer writer = new AppendableWriter(stringBuilder);
    for (int i = 0; i < 100; i++) {
      writer.write(bs.TEST_CHARS);
    }
    writer.close();
    return stringBuilder;
  }

  @Benchmark
  public final StringBuilder spf4jAppendable(final BenchmarkState bs, final ThreadState ts) throws IOException {
    StringBuilder stringBuilder = ts.sb;
    stringBuilder.setLength(0);
    Writer writer = new org.spf4j.io.AppendableWriter(stringBuilder);
    for (int i = 0; i < 100; i++) {
      writer.write(bs.TEST_CHARS);
    }
    writer.close();
    return stringBuilder;
  }

}
