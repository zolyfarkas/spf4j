package org.spf4j.base;

import com.google.common.annotations.Beta;
import java.io.Serializable;

/**
 *
 * @author zoly
 */
@Beta
public final class NameValue<T> extends Pair<String, T> {

  public NameValue(final String first, final T second) {
    super(first, second);
  }

  public static <V extends Serializable> NameValue<V> of(final String name, final V value) {
    return new NameValue<>(name, value);
  }
}
