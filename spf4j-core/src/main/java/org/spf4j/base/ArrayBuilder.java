/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    Arrays.fill(array, 0, size, null);
    size = 0;
  }

  private void ensureCapacity(final int minCapacity) {
    // overflow-conscious code
    if (minCapacity - array.length > 0) {
      expandCapacity(minCapacity);
    }
  }

  private void expandCapacity(final int minimumCapacity) {
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
    array = Arrays.copyOf(array, newCapacity);
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
