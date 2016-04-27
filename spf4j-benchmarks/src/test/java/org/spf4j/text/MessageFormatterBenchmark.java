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

  private static final ThreadLocal<StringBuilder> SB = new ThreadLocal<StringBuilder>() {
    @Override
    protected StringBuilder initialValue() {
      return new StringBuilder(128);
    }
  };

  private static final ThreadLocal<StringBuffer> SBF = new ThreadLocal<StringBuffer>() {
    @Override
    protected StringBuffer initialValue() {
      return new StringBuffer(128);
    }
  };

  @Benchmark
  public final CharSequence spf4jMessageFormatter() throws UnsupportedEncodingException, IOException {
    StringBuilder result = SB.get();
    result.setLength(0);
    org.spf4j.text.MessageFormat fmt = new org.spf4j.text.MessageFormat(
            "Here is some message wi parameter 0 = {0} and parameter 1 = {1} for testing performance", Locale.US);
    fmt.format(new Object[]{"[parameter 1]", "[parameter 2]"}, result, null);
    return result;
  }

  @Benchmark
  public final CharSequence jdkMessageFormatter() throws UnsupportedEncodingException, IOException {
    StringBuffer result = SBF.get();
    result.setLength(0);    
    java.text.MessageFormat fmt = new java.text.MessageFormat(
            "Here is some message wi parameter 0 = {0} and parameter 1 = {1} for testing performance", Locale.US);
    fmt.format(new Object[]{"[parameter 1]", "[parameter 2]"}, result, null);
    return result;
  }

  @Benchmark
  public final CharSequence slf4jMessageFormatter() throws IOException {
    StringBuilder result = SB.get();
    result.setLength(0);
    Slf4jMessageFormatter.format(result,
            "Here is some message wi parameter 0 = {} and parameter 1 = {} for testing performance",
            "[parameter 1]", "[parameter 2]");
    return result;
  }

}
