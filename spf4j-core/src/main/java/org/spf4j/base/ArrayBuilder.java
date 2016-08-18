package org.spf4j.base;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Array;
import java.util.Arrays;

/**
 *
 * @author zoly
 */
public final class ArrayBuilder<T> {

  private T[] array;

  private int size;

  public ArrayBuilder(final int initialSize, final Class<T> elementType) {
    this.array = (T[]) Array.newInstance(elementType, initialSize);
    this.size = 0;
  }

  public void add(final T element) {
    int sp1 = size + 1;
    ensureCapacity(sp1);
    array[size] = element;
    size = sp1;
  }

  public void clear() {
    java.util.Arrays.fill(array, 0, size, null);
    size = 0;
  }

  private void ensureCapacity(final int minCapacity) {
    // overflow-conscious code
    if (minCapacity - array.length > 0) {
      expandCapacity(minCapacity);
    }
  }

  void expandCapacity(final int minimumCapacity) {
    int newCapacity = array.length + array.length >> 1;
    if (newCapacity < minimumCapacity) {
      newCapacity = minimumCapacity;
    }
    if (newCapacity < 0) {
      if (minimumCapacity < 0) { // overflow
        throw new OutOfMemoryError();
      }
      newCapacity = Integer.MAX_VALUE;
    }
    array = java.util.Arrays.copyOf(array, newCapacity);
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public T[] getArray() {
    return array;
  }

  public int getSize() {
    return size;
  }

  @Override
  public String toString() {
    return "ArrayBuilder{" + "array=" + Arrays.toString(array) + ", size=" + size + '}';
  }

}
