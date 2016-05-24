
package org.spf4j.concurrent;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

/**
 *
 * @author zoly
 */
public final class AtomicReferenceExt<T> extends AtomicReference<T> {

  private static final long serialVersionUID = 1L;

  public AtomicReferenceExt(final T initialValue) {
    super(initialValue);
  }

  public AtomicReferenceExt() {
  }

  public UpdateResult<T> update(final UnaryOperator<T> function) {
    return Atomics.update(this, function);
  }

}
