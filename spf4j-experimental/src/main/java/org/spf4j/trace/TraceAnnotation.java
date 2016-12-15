
package org.spf4j.trace;

import org.spf4j.base.NameValue;

/**
 *
 * @author zoly
 */
final class TraceAnnotation {

  private final long timeStamp;

  private final NameValue[] values;

  public TraceAnnotation(long timeStamp, NameValue[] values) {
    this.timeStamp = timeStamp;
    this.values = values;
  }

  public long getTimeStamp() {
    return timeStamp;
  }

  public NameValue[] getValues() {
    return values;
  }

  @Override
  public String toString() {
    return "TraceAnnotation{" + "timeStamp=" + timeStamp + ", values=" + values + '}';
  }


}
