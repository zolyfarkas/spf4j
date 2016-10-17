
package org.spf4j.trace;

/**
 *
 * @author zoly
 */
public interface TracePart {

  CharSequence getTraceId();

  CharSequence getNodeId();

  boolean isLast();

}
