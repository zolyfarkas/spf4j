
package org.spf4j.trace;

import com.google.common.annotations.Beta;
import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.ThreadSafe;
import org.spf4j.base.Pair;

/**
 *
 * @author zoly
 */
@Beta
@CleanupObligation
@ParametersAreNonnullByDefault
@ThreadSafe
public interface TraceScope extends AutoCloseable, Consumer<StackTraceElement[]> {

  void log(LogLevel level, String format, Object ... arguments);

  void log(LogLevel level, Pair<String, Object> ... data);

  @DischargesObligation
  void close();

  <T> T executeSpan(CharSequence spanName, Function<TraceScope, T> something);

  void executeSpan(CharSequence spanName, Consumer<TraceScope> something);

  TraceScope startSpan(CharSequence spanName);

  <T> Callable<T> getTracedCallable(Callable<T> callable);

}
