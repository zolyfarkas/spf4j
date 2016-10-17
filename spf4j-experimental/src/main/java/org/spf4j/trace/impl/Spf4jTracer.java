
package org.spf4j.trace.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.spf4j.trace.TraceScope;
import org.spf4j.trace.Tracer;

/**
 *
 * @author zoly
 */
public class Spf4jTracer implements Tracer {



  private final ConcurrentMap<Thread, TraceScope> thread2Scope = new ConcurrentHashMap<>();

  private final ThreadLocal<TraceScope> scope = new ThreadLocal<TraceScope>() {
    @Override
    protected TraceScope initialValue() {
      return NopTraceScope.INSTANCE;
    }

  };

  private void attachTraceScopeToCurrentThread(final TraceScope trace) {
    thread2Scope.put(Thread.currentThread(), trace);
    scope.set(trace);
  }


  @Override
  public TraceScope getTraceScope() {
    return scope.get();
  }

  @Override
  public TraceScope getTraceScope(final Thread thread) {
    return thread2Scope.getOrDefault(thread, null);
  }

  @Override
  public TraceScope newTrace(CharSequence traceId, CharSequence spanName) {
    TraceScopeImpl trace = new TraceScopeImpl(traceId, spanName, 0);
    attachTraceScopeToCurrentThread(trace);
    return trace;
  }

  @Override
  public TraceScope continueTrace(CharSequence traceId, int spanId) {
    TraceScopeImpl trace = new TraceScopeImpl(traceId, null, spanId);
    attachTraceScopeToCurrentThread(trace);
    return trace;
  }

  @Override
  public TraceScope maybeNewTrace(CharSequence spanName, CharSequence traceId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> Callable<T> getTracedCallable(Callable<T> callable) {
    final TraceScope trace = this.getTraceScope();
    final CharSequence traceId = trace.getTraceId();
    final int currentSpanId = trace.getCurrentSpan().getSpanId();
    return new Callable<T>() {
      @Override
      public T call() throws Exception {
        attachTraceScopeToCurrentThread(new TraceScopeImpl(traceId, null, currentSpanId));
        return callable.call();
      }
    };
  }

}
