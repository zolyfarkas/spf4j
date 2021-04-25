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

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * /**
 * A simple stack implementation that does not support null elements.
 * @author Zoltan Farkas
 * @param <T>
 */
@ParametersAreNonnullByDefault
public final class SimpleStack<T> extends SimpleStackNullSupport<T> {

  public SimpleStack(final int size) {
    super(size);
  }

  public SimpleStack() {
  }

  /**
   * take a look at the top of stack
   * returns null if there is no element.
   * @return Object
   */
  @Nullable
  @Override
  public T peek() {
    if (top > 0) {
      return elems[top - 1];
    } else {
      return null;
    }
  }

  @Nullable
  public T peekAndPush(final T o) {
    int t = top + 1;
    ensureCapacity(t);
    elems[top] = o;
    T result;
    if (top >= 0) {
      result = elems[top - 1];
    } else {
      result = null;
    }
    top = t;
    return result;
  }

  @Nullable
  public T pollLast() {
    if (size() <= 0) {
      return null;
    } else {
      return pop();
    }
  }

  @Override
  public boolean remove(final Object o) {
    for (int i = 0, l = top; i < l; i++) {
      if (o.equals(elems[i])) {
        fastRemove(i);
        return true;
      }
    }
    return false;
  }

  /**
   * Not implemented, can be overwritten.
   */
  @Override
  public int indexOf(final Object o) {
    for (int i = 0, l = top; i < l; i++) {
      if (o.equals(elems[i])) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Not implemented, can be overwritten.
   */
  @Override
  public int lastIndexOf(final Object o) {
    for (int i = size() - 1; i >=  0; i--) {
      if (o.equals(elems[i])) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public boolean contains(final Object o) {
    for (int i = 0; i < top; i++) {
      if (elems[i].equals(o)) {
        return true;
      }
    }
    return false;
  }


  public void pushNull() {
    throw new UnsupportedOperationException();
  }


}
