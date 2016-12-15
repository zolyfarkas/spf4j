
package org.spf4j.trace;

import com.google.common.annotations.Beta;
import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
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
public interface TraceScope extends AutoCloseable {

  @Nonnull
  SpanScope getCurrentSpan();

  /**
   * Terminates this trace.
   */
  @DischargesObligation
  void close();

  /**
   *  the trace.
   */
  @DischargesObligation
  void scopeClose();

  CharSequence getTraceId();

}
