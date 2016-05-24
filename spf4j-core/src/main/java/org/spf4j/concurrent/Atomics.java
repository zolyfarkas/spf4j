package org.spf4j.concurrent;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

/**
 *
 * @author zoly
 */
public final class Atomics {

  private Atomics() {
  }

  public static <T> UpdateResult<T> update(final AtomicReference<T> ar, final UnaryOperator<T> function) {
    T initial;
    T newObj;
    do {
      initial = ar.get();
      newObj = function.apply(initial);
      if (initial.equals(newObj)) {
        return UpdateResult.same(initial);
      } else if (initial == newObj) {
        throw new IllegalStateException("Function " + function + " is mutating " + initial + ", this is not allowed");
      }
    } while (!ar.compareAndSet(initial, newObj));
    return UpdateResult.updated(newObj);
  }

}
