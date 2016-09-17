
package org.spf4j.ds;

import gnu.trove.set.hash.THashSet;
import java.util.Collection;

public final class IdentityHashSet<T> extends THashSet<T> {

  public IdentityHashSet() {
  }

  public IdentityHashSet(final int initialCapacity) {
    super(initialCapacity);
  }

  public IdentityHashSet(final int initialCapacity, final float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  public IdentityHashSet(final Collection<? extends T> collection) {
    super(collection);
  }

  @Override
  protected int hash(final Object notnull) {
    return System.identityHashCode(notnull);
  }

  @Override
  protected boolean equals(final Object notnull, final Object two) {
    return notnull == two;
  }
}
