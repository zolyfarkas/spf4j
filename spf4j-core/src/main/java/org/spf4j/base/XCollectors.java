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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * @author Zoltan Farkas
 */
public final class XCollectors {

  private XCollectors() {
  }

  public static <T> Collector<T, ?, ArrayDeque<T>> last(final int limit) {
    return Collector.of(
            ArrayDeque<T>::new,
            (l, e) -> {
              if (l.size() >= limit) {
                l.removeFirst();
              }
              l.addLast(e);
            },
            (l1, l2) -> {
              throw new UnsupportedOperationException("Limiting collectors do not support combining");
            }
    );
  }

  public static <T, X extends T> Collector<T, ArrayDeque<T>,
         ArrayDeque<T>> last(final int limit, final X addIfLimited) {
    return  last(() -> new ArrayDeque<T>(), limit, addIfLimited);
  }

  public static <T, X extends T, D extends Deque<T>> Collector<T, D, D> last(final Supplier<D> dqSupp,
          final int limit, final X addIfLimited) {
    return Collector.of(dqSupp,
            (l, e) -> {
              l.addLast(e);
              limitDequeue(l, limit, addIfLimited);
            }, new BinaryOperator<D>() {
      @Override
      @SuppressFBWarnings("CFS_CONFUSING_FUNCTION_SEMANTICS")
      // Agree with FB here, however this confusing behavior is documented in Collector javadoc...
      public D apply(final D l1, final D l2) {
        l1.addAll(l2);
        limitDequeue(l1, limit, addIfLimited);
        return l1;
      }
    }, Collector.Characteristics.IDENTITY_FINISH);
  }

  public static <T> void limitDequeue(final Deque<T> l1, final int limit, final T addIfLimited) {
    int extra = l1.size() - limit;
    if (extra > 0) {
      for (int i = 0, m = extra + 1; i < m; i++) {
        l1.removeFirst();
      }
      l1.addFirst(addIfLimited);
    }
  }

  /**
   * THis is a backport from JDK9.
   */
  public static <T, A, R>
          Collector<T, ?, R> filtering(final Predicate<? super T> predicate,
                  final Collector<? super T, A, R> downstream) {
    BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
    return new CollectorImpl<>(downstream.supplier(),
            (r, t) -> {
              if (predicate.test(t)) {
                downstreamAccumulator.accept(r, t);
              }
            },
            downstream.combiner(), downstream.finisher(),
            downstream.characteristics());
  }

  @SuppressWarnings("unchecked")
  private static <I, R> Function<I, R> castingIdentity() {
    return (Function<I, R>) Function.identity();
  }

  static final class CollectorImpl<T, A, R> implements Collector<T, A, R> {

    private final Supplier<A> supplier;
    private final BiConsumer<A, T> accumulator;
    private final BinaryOperator<A> combiner;
    private final Function<A, R> finisher;
    private final Set<Collector.Characteristics> characteristics;

    CollectorImpl(final Supplier<A> supplier,
            final BiConsumer<A, T> accumulator,
            final BinaryOperator<A> combiner,
            final Function<A, R> finisher,
            final Set<Collector.Characteristics> characteristics) {
      this.supplier = supplier;
      this.accumulator = accumulator;
      this.combiner = combiner;
      this.finisher = finisher;
      this.characteristics = characteristics;
    }

    CollectorImpl(final Supplier<A> supplier,
            final BiConsumer<A, T> accumulator,
            final BinaryOperator<A> combiner,
            final Set<Collector.Characteristics> characteristics) {
      this(supplier, accumulator, combiner, castingIdentity(), characteristics);
    }

    @Override
    public BiConsumer<A, T> accumulator() {
      return accumulator;
    }

    @Override
    public Supplier<A> supplier() {
      return supplier;
    }

    @Override
    public BinaryOperator<A> combiner() {
      return combiner;
    }

    @Override
    public Function<A, R> finisher() {
      return finisher;
    }

    @Override
    public Set<Collector.Characteristics> characteristics() {
      return characteristics;
    }
  }

}
