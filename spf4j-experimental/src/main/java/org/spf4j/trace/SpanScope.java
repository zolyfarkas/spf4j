package org.spf4j.trace;

import java.util.function.Consumer;
import org.spf4j.base.NameValue;

/**
 * @author zoly
 */
public interface SpanScope extends Consumer<StackTraceElement[]>, AutoCloseable {

  SpanScope startSpan(CharSequence spanName);

  /**
   * th ID identifying this span. A Span is Globally identified by (traceID, spanID)
   * @return
   */
  int getSpanId();

  void log(NameValue ... data);

  void log(String name, Object value);

  void close();

}
