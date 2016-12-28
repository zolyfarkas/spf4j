
package org.spf4j.trace.impl;

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
  public CharSequence getTraceId() {
    return "NOP";
  }


  @Override
  public void scopeClose() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }


}
