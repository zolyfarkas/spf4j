
package com.google.common.io;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.spf4j.base.IntMath;

/**
 *
 * @author zoly
 */
@State(Scope.Benchmark)
@Fork(2)
@Threads(value = 4)
public class AppendableWriterBenchmark {

    public static final char[] TEST_STRING;

    static {
        IntMath.XorShift32 rnd = new IntMath.XorShift32();
        StringBuilder builder = new StringBuilder(1000);
        for (int i = 0; i < 1000; i++) {
            builder.append('A' + Math.abs(rnd.nextInt()) % 22);
        }
        TEST_STRING = builder.toString().toCharArray();
    }

    @Benchmark
    public final StringBuilder guavaAppendable() throws UnsupportedEncodingException, IOException {
      final StringBuilder stringBuilder = new StringBuilder(100000);
      Writer writer = new AppendableWriter(stringBuilder);
      for (int i = 0; i < 100; i++) {
        writer.write(TEST_STRING);
      }
      writer.close();
      return stringBuilder;
    }

    @Benchmark
    public final StringBuilder spf4jAppendable() throws IOException {
            final StringBuilder stringBuilder = new StringBuilder(100000);
      Writer writer = new  org.spf4j.io.AppendableWriter(stringBuilder);
      for (int i = 0; i < 100; i++) {
        writer.write(TEST_STRING);
      }
      writer.close();
      return stringBuilder;        
    }



}
