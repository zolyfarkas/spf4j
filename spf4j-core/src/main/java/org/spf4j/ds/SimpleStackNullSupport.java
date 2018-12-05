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
package org.spf4j.ds;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import javax.annotation.ParametersAreNullableByDefault;
import org.spf4j.base.Arrays;

/**
 * A simple stack implementation that supports null elements.
 * @author Zoltan Farkas
 * @param <T>
 */
@ParametersAreNullableByDefault
@SuppressWarnings("checkstyle:VisibilityModifier")
public class SimpleStackNullSupport<T>
        implements List<T> {

  /**
   * stack default initial size
   */
  private static final int DEFAULT_SIZE = 32;

  /**
   * the stack storage
   */
  T[] elems;

  /**
   * the top element position
   */
  private int top;

  /**
   * construct a stack with specified size
   */
  public SimpleStackNullSupport(final int size) {
    elems = (T[]) new Object[size];
    top = 0;
  }

  /**
   * Construct a stack, default size is 20
   */
  public SimpleStackNullSupport() {
    this(DEFAULT_SIZE);
  }

  /**
   * check if stack is empty
   *
   * @return boolean
   */
  @Override
  public final boolean isEmpty() {
    return top == 0;
  }

  /**
   * push object into stack
   *
   * @param o Object
   */
  public final void push(final T o) {
    int t = top + 1;
    ensureCapacity(t);
    elems[top] = o;
    top = t;
  }

  public final int pushAndGetIdx(final T o) {
    int t = top;
    top++;
    ensureCapacity(top);
    elems[t] = o;
    return t;
  }

  private void ensureCapacity(final int minCapacity) {
    int oldCapacity = elems.length;
    if (minCapacity > oldCapacity) {
      int newCapacity = (oldCapacity * 3) / 2 + 1;
      if (newCapacity < minCapacity) {
        newCapacity = minCapacity;
      }
      // minCapacity is usually close to size, so this is a win:
      elems = java.util.Arrays.copyOf(elems, newCapacity);
    }
  }

  /**
   * Push more objects into the stack
   *
   * @param args
   */
  public final void pushAll(final T... args) {
    if (args.length == 0) {
      return;
    }
    int newTop = top + args.length;
    ensureCapacity(newTop);
    System.arraycopy(args, 0, elems, top, args.length);
    top = newTop;
  }

  /**
   * pops object out of stack
   *
   * @return Object
   */
  public final T pop() {
    final T o = elems[--top];
    elems[top] = null;
    return o;
  }


  public final void remove() {
    elems[--top] = null;
  }

  public final T[] pop(final int n) {
    int ot = top;
    top -= n;
    T[] result = (T[]) new Object[n];
    for (int i = top, j = 0; i < ot; i++, j++) {
      result[j] = elems[i];
      elems[i] = null;
    }
    return result;
  }

  public final void popTo(final T[] to, final int n) {
    int ot = top;
    top -= n;
    for (int i = top, j = 0; i < ot; i++, j++) {
      to[j] = elems[i];
      elems[i] = null;
    }
  }

  public final void removeFromTop(final int n) {
    int ot = top;
    top -= n;
    for (int i = top; i < ot; i++) {
      elems[i] = null;
    }
  }

  public final T[] popUntil(final T until) {
    int i = top - 1;
    while (elems[i] != until) {
      i--;
    }
    T[] result = java.util.Arrays.copyOfRange(elems, i + 1, top);
    java.util.Arrays.fill(elems, i, top, null);
    top = i;
    return result;
  }

  /**
   * take a look at the top of stack
   *
   * @return Object
   */
  public final T peek() {
    return elems[top - 1];
  }

  public final T peekFromTop(final int n) {
    return elems[top - 1 - n];
  }

  public final void replaceFromTop(final int n, final T value) {
    elems[top - 1 - n] = value;
  }

  public final T[] peek(final int n) {
    return java.util.Arrays.copyOfRange(elems, top - n, top);
  }

  public final T[] peekUntil(final T until) {
    int i = top - 1;
    while (elems[i] != until) {
      i--;
    }
    return java.util.Arrays.copyOfRange(elems, i + 1, top);
  }

  public final T peekElemAfter(final T until) {
    int i = top - 1;
    while (elems[i] != until) {
      i--;
    }
    return elems[i - 1];
  }

  /**
   * Clear the stack - also makes sure the stack objects are not referenced anymore
   */
  @Override
  public final void clear() {
    for (int i = 0; i < top; i++) {
      elems[i] = null;
    }
    top = 0;
  }

  /**
   * get the curent stack pos relative to base
   *
   * @return
   */
  public final int getPtr() {
    return top;
  }

  /**
   * get element from stack at index relative to base
   *
   * @param ptr
   * @return
   */
  public final T getFromPtr(final int ptr) {
    if (ptr < 0 || ptr >= top) {
      throw new IndexOutOfBoundsException(
              "Trying to get from invalid index: " + ptr + " from: " + this);
    }
    return elems[ptr];
  }

  /**
   * returns a character separated string with the stack elements
   *
   * @param separator
   * @return String
   */
  public final String toString(final char separator) {
    if (top == 0) {
      return "";
    }
    final StringBuilder result = new StringBuilder(32);
    result.append(elems[0]);
    for (int i = 1; i < top; i++) {
      result.append(separator);
      result.append(elems[i]);
    }
    return result.toString();
  }

  /**
   * to string representation. can be subclassed.
   */
  @Override
  public String toString() {
    return toString('.');
  }

  @Override
  public final int size() {
    return top;
  }

  /**
   * Not implemented, can be overwritten.
   */
  @Override
  public boolean contains(final Object o) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not implemented, can be overwritten.
   */
  @Override
  public Iterator<T> iterator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public final Object[] toArray() {
    return java.util.Arrays.copyOf(elems, top);
  }

  /**
   * Not implemented, can be overwritten.
   */
  @Override
  public <T> T[] toArray(final T[] a) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final boolean add(final T e) {
    push(e);
    return true;
  }

  /**
   * Can be overwritten for better implementation.
   * @param o element to remove.
   * @return
   */
  @Override
  public boolean remove(final Object o) {
    if (o == null) {
      for (int i = 0; i < top; i++) {
        if (elems[i] == null) {
          fastRemove(i);
          return true;
        }
      }
    } else {
      for (int i = 0; i < top; i++) {
        if (o.equals(elems[i])) {
          fastRemove(i);
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Not implemented, can be overwritten.
   */
  @Override
  public boolean containsAll(final Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not implemented, can be overwritten.
   */
  @Override
  public boolean addAll(final Collection<? extends T> c) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not implemented, can be overwritten.
   */
  @Override
  public boolean removeAll(final Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not implemented, can be overwritten.
   */
  @Override
  public boolean retainAll(final Collection<?> c) {
    throw new UnsupportedOperationException();
  }


  @Override
  public final boolean equals(final Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final SimpleStackNullSupport<T> other = (SimpleStackNullSupport<T>) obj;
    if (this.top != other.top) {
      return false;
    }
    return Arrays.deepEquals(this.elems, other.elems, 0, top);
  }

  /**
   * can be overwritten for better implementation.
   */
  @Override
  public int hashCode() {
    int hashCode = 7;
    for (int i = 0; i < top; i++) {
      Object obj = elems[i];
      hashCode = 31 * hashCode + (obj == null ? 0 : obj.hashCode());
    }
    return hashCode;
  }

  /**
   * Not implemented, can be overwritten.
   */
  @Override
  public boolean addAll(final int index, final Collection<? extends T> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final T get(final int index) {
    return elems[index];
  }

  @Override
  public final T set(final int index, final T element) {
    ensureCapacity(index);
    T result = elems[index];
    elems[index] = element;
    return result;
  }

  /**
   * Not implemented, can be overwritten.
   */
  @Override
  public void add(final int index, final T element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final T remove(final int index) {
    if (index >= top && index < 0) {
      throw new IndexOutOfBoundsException("Invalid index " + index + ", current size " + top);
    }
    T result = elems[index];
    int next = index + 1;
    int numMoved = top - next;
    if (numMoved > 0) {
      System.arraycopy(elems, next, elems, index, numMoved);
    }
    elems[--top] = null;
    return result;
  }

  final void fastRemove(final int index) {
    int next = index + 1;
    int numMoved = top - next;
    if (numMoved > 0) {
      System.arraycopy(elems, next, elems, index, numMoved);
    }
    elems[--top] = null;
  }

  /**
   * Not implemented, can be overwritten.
   */
  @Override
  public int indexOf(final Object o) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not implemented, can be overwritten.
   */
  @Override
  public int lastIndexOf(final Object o) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not implemented, can be overwritten.
   */
  @Override
  public ListIterator<T> listIterator() {
    throw new UnsupportedOperationException();
  }

  /**
   * Not implemented, can be overwritten.
   */
  @Override
  public ListIterator<T> listIterator(final int index) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not implemented, can be overwritten.
   */
  @Override
  public List<T> subList(final int fromIndex, final int toIndex) {
    throw new UnsupportedOperationException();
  }
}
