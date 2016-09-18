package org.spf4j.trace;

import java.util.function.Consumer;
import org.spf4j.base.Pair;

/**
 *
 * @author zoly
 */
public interface SpanBuilder extends Consumer<StackTraceElement[]>, AutoCloseable {


  void log(Pair<String, Object> ... data);


  Span build();

}
