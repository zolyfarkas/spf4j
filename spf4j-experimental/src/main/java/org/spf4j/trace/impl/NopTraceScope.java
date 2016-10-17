
package org.spf4j.trace.impl;

import java.util.concurrent.Callable;
import org.spf4j.trace.SpanScope;
import org.spf4j.trace.TraceScope;

/**
 * @author zoly
 */
public final class NopTraceScope implements TraceScope {

  private NopTraceScope() { }

  public static final NopTraceScope INSTANCE = new NopTraceScope();

  @Override
  public SpanScope getCurrentSpan() {
    return NopSpanScope.INSTANCE;
  }

  @Override
  public void close() {
  }

  @Override
  public <T> Callable<T> getTracedCallable(Callable<T> callable) {
    return callable;
  }

  @Override
  public TraceScope attachToThread() {
    return this;
  }

  @Override
  public CharSequence getTraceId() {
    return "NOP";
  }


}
