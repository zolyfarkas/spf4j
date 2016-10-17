
package org.spf4j.trace.impl;

import java.util.ArrayDeque;
import java.util.Deque;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.trace.SpanScope;
import org.spf4j.trace.TraceScope;

/**
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public class TraceScopeImpl implements TraceScope {

  private final CharSequence traceId;

  private Deque<SpanScope> spanStack;

  /**
   * Start or continue a trace.
   * @param traceId the trace id.
   * @param spanName the name of the span, can be null if this trace is a continuation.
   * @param spanId the current span id.
   */
  TraceScopeImpl(final CharSequence traceId, @Nullable final CharSequence spanName,
          final int spanId) {
    this.traceId = traceId;
    this.spanStack = new ArrayDeque<>(4);
    SpanScopeImpl scope = new SpanScopeImpl(spanName, spanId, spanStack);
    spanStack.addLast(scope);
  }

  @Override
  public SpanScope getCurrentSpan() {
    return spanStack.peekLast();
  }

  @Override
  public void close() {
  }

  @Override
  public CharSequence getTraceId() {
    return traceId;
  }

  @Override
  public void finish() {
  }

}
