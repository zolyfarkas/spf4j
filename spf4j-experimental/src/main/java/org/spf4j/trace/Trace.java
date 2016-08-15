
package org.spf4j.trace;

/**
 *
 * @author zoly
 */
public interface Trace extends Span {


  enum TraceMandate {
    MANDATORY, OPTIONAL
  }

  CharSequence getTraceId();

  boolean getMandate();

}
