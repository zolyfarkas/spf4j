package org.spf4j.ds;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 *
 * @author zoly
 */
public final class LinkedHashMapEx<K, V> extends LinkedHashMap<K, V>
        implements LinkedMap<K, V> {

  private static final Field TAIL;
  private static final long serialVersionUID = 1L;

  static {

    TAIL = AccessController.doPrivileged(new PrivilegedAction<Field>() {
      @Override
      public Field run() {
        try {
          Field field = LinkedHashMap.class.getDeclaredField("tail");
          field.setAccessible(true);
          return field;
        } catch (NoSuchFieldException | SecurityException ex) {
          throw new RuntimeException(ex);
        }
      }
    });

  }

  private Map.Entry<K, V> getTail() {
    try {
      return (Map.Entry<K, V>) TAIL.get(this);
    } catch (IllegalArgumentException | IllegalAccessException ex) {
      throw new RuntimeException(ex);
    }
  }

  public LinkedHashMapEx() {
    super();
  }

  public LinkedHashMapEx(final int initialCapacity, final float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  public LinkedHashMapEx(final int initialCapacity) {
    super(initialCapacity);
  }

  public LinkedHashMapEx(final Map<? extends K, ? extends V> m) {
    super(m);
  }

  public LinkedHashMapEx(final int initialCapacity, final float loadFactor, final boolean accessOrder) {
    super(initialCapacity, loadFactor, accessOrder);
  }

  @Nullable
  @Override
  public Map.Entry<K, V> getLastEntry() {
    return getTail();
  }

  @Nullable
  @Override
  public Map.Entry<K, V> pollLastEntry() {
    final Entry<K, V> lastEntry = getTail();
    if (lastEntry != null) {
      remove(lastEntry.getKey());
    }
    return lastEntry;
  }

}
