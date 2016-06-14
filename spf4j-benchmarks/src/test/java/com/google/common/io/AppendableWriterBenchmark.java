
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
@State(Scope.Benchmark)
@Fork(2)
@Threads(value = 8)
public class AppendableWriterBenchmark {

    public static final char[] TEST_CHARS;


    static {
        StringBuilder builder = new StringBuilder(1000);
        for (int i = 0; i < 1000; i++) {
            builder.append('A');
        }
        TEST_CHARS = builder.toString().toCharArray();
    }

    @Benchmark
    public final StringBuilder guavaAppendable() throws  IOException {
      final StringBuilder stringBuilder = new StringBuilder(100000);
      Writer writer = new AppendableWriter(stringBuilder);
      for (int i = 0; i < 100; i++) {
        writer.write(TEST_CHARS);
      }
      writer.close();
      return stringBuilder;
    }

    @Benchmark
    public final StringBuilder spf4jAppendable() throws IOException {
      final StringBuilder stringBuilder = new StringBuilder(100000);
      Writer writer = new  org.spf4j.io.AppendableWriter(stringBuilder);
      for (int i = 0; i < 100; i++) {
        writer.write(TEST_CHARS);
      }
      writer.close();
      return stringBuilder;
    }


  public static void main(String[] args) throws IOException {
    AppendableWriterBenchmark benchmark = new AppendableWriterBenchmark();
    for (int k = 0; k < 20; k++) {
      long start = System.nanoTime();
      StringBuilder result = null;
      for (int i = 0; i < 100000; i++) {
        result = benchmark.spf4jAppendable();
      }
      long elapsed = System.nanoTime() - start;
      System.out.println("Ops/s: "
              +  ((double) 100000 / ((double) elapsed / 1000000000)) + result.charAt(0));
    }
  }

}
