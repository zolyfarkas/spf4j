package org.spf4j.trace.impl;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import org.spf4j.base.NameValue;
import org.spf4j.stackmonitor.SampleNode;
import org.spf4j.trace.SpanScope;

/**
 * @author zoly
 */
public class SpanScopeImpl implements SpanScope {

  private final int spanId;

  private final int parentSpanId;

  private final CharSequence spanName;

  private final IntSupplier idSupplier;

  private final SpanEventHandler eh;

  private final long startTime;

  private long endTime;

  private SampleNode samples;

  /**
   * create a root SpanScope
   * @param spanName
   * @param id
   * @param onNewScope
   * @param onCLose
   */
  SpanScopeImpl(@Nullable final CharSequence spanName, final int id,
          SpanEventHandler eh) {
    this.spanName = spanName;
    this.spanId = id;
    this.parentSpanId = -1;
    idSupplier =  new AtomicInteger(id + 1)::getAndIncrement;
    this.eh = eh;
    this.startTime = System.currentTimeMillis();
    this.endTime = -1;
  }


  SpanScopeImpl(@Nullable final CharSequence spanName, final IntSupplier idSupplier, final int parentId,
          SpanEventHandler eh) {
    this.spanName = spanName;
    this.spanId = idSupplier.getAsInt();
    this.parentSpanId = parentId;
    this.idSupplier = idSupplier;
    this.eh = eh;
    this.startTime = System.currentTimeMillis();
    this.endTime = -1;
  }


  @Override
  public SpanScope startSpan(CharSequence spanName) {
    SpanScopeImpl spanScope = new SpanScopeImpl(spanName, idSupplier, spanId, eh);
    eh.newSpan(spanScope);
    return spanScope;
  }

  @Override
  public int getSpanId() {
    return spanId;
  }

  @Override
  public void log(NameValue... data) {
  }

  @Override
  public void log(String name, Object value) {
  }

  @Override
  public void close() {
    this.endTime = System.currentTimeMillis();
    eh.closeSpan(this);
  }

  @Override
  public void accept(StackTraceElement[] t) {
    if (samples == null) {
      samples = SampleNode.createSampleNode(t);
    } else {
      SampleNode.addToSampleNode(samples, t);
    }
  }

}
