
package org.spf4j.trace;

import com.google.common.annotations.Beta;
import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;
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
public interface TraceScope extends AutoCloseable {

  void log(LogLevel level, String format, Object ... arguments);

  void log(LogLevel level, Pair<String, Object> ... data);

  @Nullable
  TraceScope getParent();

  void addSample(final long ts, StackTraceElement [] sample);

  @DischargesObligation
  void close();

  <T> T execute(Callable<T> something);

  void execute(Runnable something);

  TraceScope startSpan(CharSequence spanName);

  <T> Callable<T> getTracedCallable(Callable<T> callable);


}
