
package org.spf4j.trace.impl;

import com.google.common.annotations.Beta;
import org.spf4j.trace.Tracer;
import org.spf4j.trace.TraceScope;

/**
 *
 * @author zoly
 */
@Beta
public final class TracerImpl implements Tracer {


  private final ThreadLocal<TraceScope> TRACE_LOCAL = new ThreadLocal<>();

  @Override
  public TraceScope startSpan(CharSequence spanName) {
  }

  @Override
  public TraceScope continueSpan(CharSequence spanName, CharSequence spanId) {
    TraceScope exScope = TRACE_LOCAL.get();
  }

  @Override
  public void closeSpanScope(final TraceScope scope) {
    TRACE_LOCAL.set(null);
  }

}
