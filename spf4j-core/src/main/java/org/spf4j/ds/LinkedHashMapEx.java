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

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Linked hashmap that allows access to the Last Entry efficiently.
 * @author zoly
 */
public final class LinkedHashMapEx<K, V> extends LinkedHashMap<K, V>
        implements LinkedMap<K, V> {

  private static final long serialVersionUID = 1L;
  private static final Field TAIL;

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
