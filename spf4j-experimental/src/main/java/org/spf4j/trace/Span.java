package org.spf4j.trace;

import java.util.function.BiFunction;

/**
 *
 * @author zoly
 */
public interface Span extends io.opentracing.Span {

  <T> void accumulate(String key, T value, BiFunction<T,T,T> accFnc);

}
