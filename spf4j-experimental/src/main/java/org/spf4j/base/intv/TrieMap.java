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
package org.spf4j.base.intv;

import gnu.trove.map.TCharObjectMap;
import gnu.trove.map.hash.TCharObjectHashMap;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.spf4j.base.MutableHolder;
import org.spf4j.base.Pair;

/**
 *
 * @author Zoltan Farkas
 */
public final class TrieMap<T> {

  private static final class Node<T> {

    private TCharObjectMap<Node> nodes;

    private T value;

    boolean terminatesWord;

  }

  private static final Node root = new Node();

  public Optional<T> put(final CharSequence key, final T value) {
    return put(key, 0, key.length(), value);
  }

  public Optional<T> put(final CharSequence key, final int pfrom, final int to,
          final T value) {
    MutableHolder<Optional<T>> result = new MutableHolder<>();
    put(key, pfrom, to, (x) -> {
      result.setValue(x);
      return value;
    });
    return result.getValue();
  }

  public Optional<T> put(final CharSequence key, final int pfrom, final int to,
          final Function<Optional<T>, T> value) {
    int from = pfrom;
    Node<T> node = root;
    while (from < to) {
      char c = key.charAt(from);
      Node<T> newNode;
      if (node.nodes == null) {
        node.nodes = new TCharObjectHashMap<>();
        newNode = new Node();
        node.nodes.put(c, newNode);
      } else {
        newNode = node.nodes.get(c);
        if (newNode == null) {
          newNode = new Node();
          node.nodes.put(c, newNode);
        }
      }
      node = newNode;
      from = from + 1;
    }
    if (node.terminatesWord) {
      T existing = node.value;
      node.value = value.apply(Optional.of(existing));
      return Optional.of(existing);
    } else {
      node.terminatesWord = true;
      node.value = value.apply(Optional.empty());
      return Optional.empty();
    }
  }

  public Optional<T> get(final CharSequence key) {
    return get(key, 0, key.length());
  }

  public Optional<T> get(final CharSequence key, final int pfrom, final int to) {
    int from = pfrom;
    Node<T> node = root;
    while (from < to) {
      char c = key.charAt(from);
      if (node.nodes == null) {
        return Optional.empty();
      }
      node = node.nodes.get(c);
      if (node == null) {
        return Optional.empty();
      }
      from = from + 1;
    }
    if (node.terminatesWord) {
      return Optional.of(node.value);
    } else {
      return Optional.empty();
    }
  }

  public void forEach(final BiConsumer<CharSequence, T> consumer) {
    Deque<Pair<String, Node<T>>> process = new ArrayDeque<>();
    process.add(Pair.of("", root));
    Pair<String, Node<T>> p;
    while ((p = process.pollLast()) != null) {
      Node<T> node = p.getSecond();
      if (node.terminatesWord) {
        consumer.accept(p.getFirst(), node.value);
      }
      if (node.nodes != null) {
        final String sf = p.getFirst();
        node.nodes.forEachEntry((k, v) -> {
          process.addLast(Pair.of(sf + k, v));
          return true;
        });
      }
    }
  }

}
