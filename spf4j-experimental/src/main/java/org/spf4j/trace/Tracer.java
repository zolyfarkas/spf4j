
package org.spf4j.trace;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 *
 * @author zoly
 */
@ThreadSafe
public interface Tracer {

  /**
   * Get current traceScope.
   * @return
   */
  @Nullable
  TraceScope getTraceScope();

  TraceScope continueOrNewTrace(final int parentSpanId,
          final CharSequence spanName,
          @Nullable CharSequence traceId);

}
