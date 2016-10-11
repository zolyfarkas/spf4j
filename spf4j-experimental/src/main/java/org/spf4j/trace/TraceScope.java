
package org.spf4j.trace;

import com.google.common.annotations.Beta;
import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.ThreadSafe;

/**
 *
 * @author zoly
 */
@Beta
@CleanupObligation
@ParametersAreNonnullByDefault
@ThreadSafe
public interface TraceScope extends AutoCloseable, Consumer<StackTraceElement[]> {

  SpanScope startSpan(CharSequence spanName);

  @Nonnull
  SpanScope getCurrentSpan();
  
  @DischargesObligation
  void close();

  <T> Callable<T> getTracedCallable(Callable<T> callable);

  public TraceScope attachToThread();

  CharSequence getTraceId();

  int getCurrentSpanId();

  <T> T executeSpan(CharSequence spanName, Function<SpanScope, T> something);

  void executeSpan(CharSequence spanName, Consumer<SpanScope> something);


}
