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

import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author zoly
 */
public class MutableHolder<T> implements Supplier<T> {

  private T value;

  /**
   * @deprecated use MutableHolder.of
   */
  @Deprecated
  public MutableHolder(final T value) {
    this.value = value;
  }

  /**
   * @deprecated use MutableHolder.of
   */
  @Deprecated
  public MutableHolder() {
    this.value = null;
  }

  public static <T> MutableHolder<T> of(final T value) {
    return (MutableHolder<T>) new MutableHolder<>(value);
  }

  public static <T extends Comparable> ComparableHolder<T> of(final T value) {
    return ComparableHolder.of(value);
  }

  public final T getValue() {
    return value;
  }

  public final void setValue(final T value) {
    this.value = value;
  }

  @Override
  public final String toString() {
    return "MutableHolder{" + "value=" + value + '}';
  }

  @Override
  public final int hashCode() {
    return 79 + Objects.hashCode(this.value);
  }

  @Override
  public final boolean equals(final Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final MutableHolder<?> other = (MutableHolder<?>) obj;
    return Objects.equals(this.value, other.value);
  }

  @Override
  public final T get() {
    return value;
  }

  public static final class ComparableHolder<T extends Comparable>
          extends MutableHolder<T>
          implements Comparable<ComparableHolder<T>> {

    public ComparableHolder(final T value) {
      super(value);
    }

    public ComparableHolder() {
      super(null);
    }

    public static <T extends Comparable> ComparableHolder<T> of(final T value) {
      return (ComparableHolder<T>) new ComparableHolder<>(value);
    }

    @Override
    public int compareTo(final ComparableHolder<T> o) {
      T thisVal = getValue();
      T otherVal = o.getValue();
      if (thisVal == null) {
        if (otherVal == null) {
          return 0;
        } else {
          return -1;
        }
      } else {
        if (otherVal == null) {
          return 1;
        } else {
          return thisVal.compareTo(otherVal);
        }
      }
    }

  }



}
