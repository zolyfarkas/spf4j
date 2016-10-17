
package org.spf4j.trace.impl;

import org.spf4j.base.NameValue;
import org.spf4j.trace.Span;
import org.spf4j.trace.SpanScope;

/**
 * @author zoly
 */
public class NopSpanScope implements SpanScope {

  private NopSpanScope() { }

  public static final NopSpanScope INSTANCE = new NopSpanScope();


  @Override
  public SpanScope startSpan(CharSequence spanName) {
    return this;
  }

  @Override
  public int getSpanId() {
    return -1;
  }

  @Override
  public void log(NameValue... data) {
  }

  @Override
  public void log(String name, Object value) {
  }

  @Override
  public Span finish() {
    return null;
  }

  @Override
  public void close() {
  }

  @Override
  public void accept(StackTraceElement[] t) {
  }

}
