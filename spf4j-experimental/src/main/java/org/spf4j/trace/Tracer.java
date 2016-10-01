
package org.spf4j.trace;

import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import org.spf4j.concurrent.ScalableSequence;
import org.spf4j.concurrent.UIDGenerator;

/**
 *
 * @author zoly
 */
@ThreadSafe
public interface Tracer {

  Supplier<CharSequence> ID_GEN = new UIDGenerator(new ScalableSequence(0, 32), "tr", 1475333096102L);

  /**
   * Get current traceScope.
   * @return
   */
  @Nullable
  TraceScope getTraceScope();

  default TraceScope newTrace(@Nonnull final CharSequence spanName) {
    return newTrace(spanName, ID_GEN.get());
  }

  TraceScope newTrace(@Nonnull final CharSequence spanName, @Nonnull final CharSequence traceId);

  TraceScope continueTrace(@Nonnull final CharSequence traceId, final int spanId);

  default TraceScope continueOrNewTrace(@Nullable CharSequence traceId,
          final int parentSpanId,
          @Nullable final CharSequence spanName) {
    if (traceId != null) {
      if (parentSpanId >= 0) {
        if (spanName != null) {
          TraceScope scope = continueTrace(traceId, parentSpanId);
          scope.startSpan(spanName);
          return scope;
        } else {
          return continueTrace(traceId, parentSpanId);
        }
      } else if (spanName != null) {
        return newTrace(spanName, traceId);
      } else {
        throw new IllegalArgumentException("Invalid input " + traceId + ", " + parentSpanId + ", " + spanName);
      }
    } else if (spanName != null) {
      return newTrace(spanName);
    } else {
        throw new IllegalArgumentException("Invalid input " + traceId + ", " + parentSpanId + ", " + spanName);
    }
  }

}
