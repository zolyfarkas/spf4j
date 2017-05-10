
package org.spf4j.trace;

import javax.annotation.concurrent.ThreadSafe;

/**
 * the main tracing api.
 * @author zoly
 */
@ThreadSafe
public interface Tracer extends io.opentracing.Tracer {

  Span getThreadSpan(Thread thread);

}
