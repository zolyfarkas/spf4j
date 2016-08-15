
package org.spf4j.trace.impl;

import org.spf4j.base.Pair;
import org.spf4j.base.Writeable;
import org.spf4j.stackmonitor.SampleNode;
import org.spf4j.trace.LogLevel;
import org.spf4j.trace.TraceScope;

/**
 *
 * @author zoly
 */
public class SpanScopeImpl implements TraceScope {

  private final SampleNode samples;

  private final TraceScope parent;

  private final long spanStartTime;

  public SpanScopeImpl() {
    samples = SampleNode.createSampleNode(Thread.currentThread().getStackTrace());
    this.parent = null;
    this.spanStartTime = System.currentTimeMillis();
  }

  @Override
  public void log(LogLevel level, String format, Object... arguments) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void log(Pair<String, Writeable>... data) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public TraceScope getParent() {
    return parent;
  }

  @Override
  public TraceScope startSpan(CharSequence spanName) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void addSample(long ts, StackTraceElement[] sample) {
    SampleNode.addToSampleNode(samples, sample);
  }

  @Override
  public void detach() {
  }

  @Override
  public TraceScope attach() {
    return null;
  }

  @Override
  public void close(){
  }

}
