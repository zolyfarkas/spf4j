
package org.spf4j.concurrent;

/**
 *
 * @author zoly
 */
public final class UpdateResult<T> {

  private final boolean objectUpdated;

  private final T obj;

  private UpdateResult(final boolean objectUpdated, final T obj) {
    this.objectUpdated = objectUpdated;
    this.obj = obj;
  }

  public static <T> UpdateResult<T> updated(final T object) {
    return new UpdateResult<>(true, object);
  }

  public static <T> UpdateResult<T> same(final T object) {
    return new UpdateResult<>(false, object);
  }

  public boolean isObjectUpdated() {
    return objectUpdated;
  }

  public T getObj() {
    return obj;
  }

  @Override
  public String toString() {
    return "UpdateResult{" + "objectUpdated=" + objectUpdated + ", obj=" + obj + '}';
  }

}
