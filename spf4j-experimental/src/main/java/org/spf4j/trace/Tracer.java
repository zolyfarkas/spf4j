
package org.spf4j.trace;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 *
 * @author zoly
 */
@ThreadSafe
public interface Tracer {

  TraceScope getTraceScope();

  TraceScope continueOrNewTrace(CharSequence spanName, @Nullable CharSequence traceId);

}
