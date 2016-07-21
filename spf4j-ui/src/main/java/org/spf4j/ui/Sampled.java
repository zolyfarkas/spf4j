
package org.spf4j.ui;

import java.util.Objects;

/**
 *
 * @author zoly
 */
public final class Sampled<T> {

  private final T obj;

  private final int nrSamples;

  public Sampled(final T obj, final int nrSamples) {
    this.obj = obj;
    this.nrSamples = nrSamples;
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 97 * hash + Objects.hashCode(this.obj);
    return 97 * hash + this.nrSamples;
  }

  @Override
  public boolean equals(final Object pobj) {
    if (this == pobj) {
      return true;
    }
    if (pobj == null) {
      return false;
    }
    if (getClass() != pobj.getClass()) {
      return false;
    }
    final Sampled<?> other = (Sampled<?>) pobj;
    if (this.nrSamples != other.nrSamples) {
      return false;
    }
    return Objects.equals(this.obj, other.obj);
  }

  public T getObj() {
    return obj;
  }

  public int getNrSamples() {
    return nrSamples;
  }

  @Override
  public String toString() {
    return "Sampled{" + "obj=" + obj + ", nrSamples=" + nrSamples + '}';
  }

}
