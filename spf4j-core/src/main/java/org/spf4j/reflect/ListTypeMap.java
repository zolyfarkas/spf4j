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
package org.spf4j.reflect;

import com.google.common.base.Function;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import org.spf4j.base.Pair;

/**
 * @author Zoltan Farkas
 */
@ThreadSafe
public final class ListTypeMap<H> implements TypeMap<H> {


  private final LinkedList<Pair<TypeToken, H>> registry;

  public ListTypeMap() {
    this.registry = new LinkedList<>();
  }

  @CheckReturnValue
  @Override
  public boolean putIfNotPresent(final Type type, final H appender) {
    synchronized (registry) {
      ListIterator<Pair<TypeToken, H>> listIterator = registry.listIterator();
      while (listIterator.hasNext()) {
        Pair<TypeToken, H> next = listIterator.next();
        final TypeToken nType = next.getFirst();
        if (nType.isSupertypeOf(type)) {
          if (nType.getType().equals(type)) {
            return false;
          }
          listIterator.previous();
          listIterator.add(Pair.of(TypeToken.of(type), appender));
          return true;
        }
      }
      listIterator.add((Pair) Pair.of(type, appender));
    }
    return true;
  }

  @CheckReturnValue
  @Nullable
  @Override
  public H put(final Type type, final H appender) {
    synchronized (registry) {
      ListIterator<Pair<TypeToken, H>> listIterator = registry.listIterator();
      while (listIterator.hasNext()) {
        Pair<TypeToken, H> next = listIterator.next();
        final TypeToken nType = next.getFirst();
        if (nType.isSupertypeOf(type)) {
          if (nType.getType().equals(type)) {
            return next.getSecond();
          }
          listIterator.previous();
          listIterator.add(Pair.of(TypeToken.of(type), appender));
          return null;
        }
      }
      listIterator.add((Pair) Pair.of(type, appender));
    }
    return null;
  }


  @CheckReturnValue
  @Override
  public boolean remove(final Type type) {
    synchronized (registry) {
      ListIterator<Pair<TypeToken, H>> listIterator = registry.listIterator();
      while (listIterator.hasNext()) {
        Pair<TypeToken, H> item = listIterator.next();
          if (item.getFirst().getType().equals(type)) {
            listIterator.remove();
            return true;
          }
      }
      return false;
    }
  }

  @Override
  public boolean replace(final Type type,
          final Function<H, H> replace) {
    synchronized (registry) {
      ListIterator<Pair<TypeToken, H>> listIterator = registry.listIterator();
      while (listIterator.hasNext()) {
        Pair<TypeToken, H> next = listIterator.next();
        final TypeToken nType = next.getFirst();
        if (nType.getType().equals(type)) {
          listIterator.set((Pair) Pair.of(nType, replace.apply(next.getSecond())));
          return true;
        }
      }
      return false;
    }
  }

  @Override
  public void replaceOrAdd(final Type type,
          final Function<H, H> replace, final H add) {
    synchronized (registry) {
      if (!replace(type, replace)) {
        if (put(type, add) != null) {
          throw new IllegalStateException("Illegal State " + this + ", type=" + type);
        }
      }
    }
  }


  @Override
  public H get(final Type t) {
    synchronized (registry) {
      for (Map.Entry<TypeToken, H> entry : registry) {
        TypeToken clasz = entry.getKey();
        if (clasz.isSupertypeOf(t)) {
          return entry.getValue();
        }
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return "ListTypeMap{" + "registry=" + registry + '}';
  }


}
