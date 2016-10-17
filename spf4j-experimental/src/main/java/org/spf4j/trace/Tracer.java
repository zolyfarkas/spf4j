
package org.spf4j.trace;

import java.util.concurrent.Callable;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import org.spf4j.concurrent.ScalableSequence;
import org.spf4j.concurrent.UIDGenerator;

/**
 * the main tracing api.
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

  @Nullable
  TraceScope getTraceScope(Thread thread);

  default TraceScope newTrace(@Nonnull final CharSequence spanName) {
    return newTrace(ID_GEN.get(), spanName);
  }

  default TraceScope maybeNewTrace(@Nonnull final CharSequence spanName) {
    return maybeNewTrace(ID_GEN.get(), spanName);
  }


  /**
   * Always initiate a new trace.
   * the implementation must initiate a store trace information. (If unable, context must error)
   *
   * @param spanName
   * @param traceId
   * @return
   */
  TraceScope newTrace(@Nonnull final CharSequence traceId, @Nonnull final CharSequence spanName);

  /**
   * Initiate a new trace if conditions allow. (load....)
   * @param spanName
   * @param traceId
   * @return
   */
  TraceScope maybeNewTrace(@Nonnull final CharSequence traceId, @Nonnull final CharSequence spanName);

  /**
   * Continue a trace initiated on a different node.
   * @param traceId
   * @param spanId
   * @return
   */
  TraceScope continueTrace(@Nonnull final CharSequence traceId, final int spanId);

  default TraceScope continueOrNewTrace(@Nullable CharSequence traceId,
          final int parentSpanId,
          @Nullable final CharSequence spanName) {
    if (traceId != null) {
      if (parentSpanId >= 0) {
        if (spanName != null) {
          TraceScope scope = continueTrace(traceId, parentSpanId);
          scope.getCurrentSpan().startSpan(spanName);
          return scope;
        } else {
          return continueTrace(traceId, parentSpanId);
        }
      } else if (spanName != null) {
        return maybeNewTrace(traceId, spanName);
      } else {
        throw new IllegalArgumentException("Invalid input " + traceId + ", " + parentSpanId + ", " + spanName);
      }
    } else if (spanName != null) {
      return maybeNewTrace(spanName);
    } else {
        throw new IllegalArgumentException("Invalid input " + traceId + ", " + parentSpanId + ", " + spanName);
    }
  }


  <T> Callable<T> getTracedCallable(Callable<T> callable);


}
