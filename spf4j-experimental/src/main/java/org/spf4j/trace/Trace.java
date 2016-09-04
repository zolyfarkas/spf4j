
package org.spf4j.trace;

/**
 *
 * @author zoly
 */
public interface Trace {


  enum TraceMandate {
    MANDATORY, OPTIONAL
  }

  CharSequence getTraceId();

  Span getRootSpan();

  TraceMandate getMandate();

}
