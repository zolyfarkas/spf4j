
package org.spf4j.text;

import org.spf4j.base.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
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
public class MessageFormatterBenchmark {

  
    @Benchmark
    public final CharSequence spf4jMessageFormatter() throws UnsupportedEncodingException, IOException {
      org.spf4j.text.MessageFormat fmt = new org.spf4j.text.MessageFormat(
              "Here is some message wi parameter 0 = {0} and parameter 1 = {1} for testing performance", Locale.US);
      StringBuilder result = new StringBuilder(128);
      fmt.format(new Object [] {"[parameter 1]", "[parameter 2]"}, result, null);
      return result;
    }

    @Benchmark
    public final CharSequence jdkMessageFormatter() throws UnsupportedEncodingException, IOException {
      java.text.MessageFormat fmt = new java.text.MessageFormat(
              "Here is some message wi parameter 0 = {0} and parameter 1 = {1} for testing performance", Locale.US);
      StringBuffer result = new StringBuffer(128);
      fmt.format(new Object [] {"[parameter 1]", "[parameter 2]"}, result, null);
      return result;
    }
    
    
    @Benchmark
    public final CharSequence slf4jMessageFormatter() throws IOException {
      StringBuilder  result = new StringBuilder(128);
      Slf4jMessageFormatter.format(result,
              "Here is some message wi parameter 0 = {} and parameter 1 = {} for testing performance",
              "[parameter 1]", "[parameter 2]");
      return result;
    }

}
