package org.spf4j.trace;

import java.util.function.Consumer;
import org.spf4j.base.NameValue;

/**
 *
 * @author zoly
 */
public interface SpanBuilder extends Consumer<StackTraceElement[]>, AutoCloseable {

  int getSpanId();

  void log(NameValue ... data);

  void log(String name, Object value);

  Span build();

  void close();

}
