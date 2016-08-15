
package org.spf4j.trace.impl;

import org.spf4j.trace.Span;

/**
 *
 * @author zoly
 */
public class SpanImpl implements Span {

  private final CharSequence id;
  private final CharSequence name;
  private final Span parent;
  private final long startTimeMilis;
  private final long elapsedTimeMillis;

  public SpanImpl(CharSequence id, CharSequence name, Span parent, long startTimeMilis, long elapsedTimeMillis) {
    this.id = id;
    this.name = name;
    this.parent = parent;
    this.startTimeMilis = startTimeMilis;
    this.elapsedTimeMillis = elapsedTimeMillis;
  }

  @Override
  public CharSequence getId() {
    return id;
  }

  @Override
  public CharSequence getName() {
    return name;
  }

  @Override
  public long getStartTimeMillis() {
    return startTimeMilis;
  }

  @Override
  public long getElapsedTimeMillis() {
    return elapsedTimeMillis;
  }

  @Override
  public Span getParent() {
    return parent;
  }


}
