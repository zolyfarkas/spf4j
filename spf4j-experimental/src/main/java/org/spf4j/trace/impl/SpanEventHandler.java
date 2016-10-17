
package org.spf4j.trace.impl;

import org.spf4j.trace.SpanScope;

/**
 *
 * @author zoly
 */
public interface SpanEventHandler {

  void newSpan(SpanScope spanScope);

  void closeSpan(SpanScope spanScope);


}
