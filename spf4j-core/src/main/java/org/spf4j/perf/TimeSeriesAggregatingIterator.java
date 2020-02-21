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
package org.spf4j.perf;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.ToLongFunction;

public final class TimeSeriesAggregatingIterator<T> implements Iterator<T> {

  private final long aggTime;
  private final PeekingIterator<T> it;
  private T rec;
  private long maxTime;
  private final ToLongFunction<T> timeExtractor;
  private final BiConsumer<T, T> accumulator;

  public TimeSeriesAggregatingIterator(final Iterable<T> dataStream,
          final ToLongFunction<T> timeExtractor, final BiConsumer<T, T> accumulator, final long aggTime) {
    this.aggTime = aggTime;
    it = Iterators.peekingIterator(dataStream.iterator());
    this.timeExtractor = timeExtractor;
    this.accumulator = accumulator;
    aggNext();
  }

  private void aggNext() {
    if (it.hasNext()) {
      rec = it.next();
      long recTime = timeExtractor.applyAsLong(rec);
      maxTime = recTime + aggTime;
      while (it.hasNext()) {
        T next = it.peek();
        recTime = timeExtractor.applyAsLong(next);
        if (recTime < maxTime) {
          accumulator.accept(rec, next);
          it.next();
        } else {
          break;
        }
      }
    } else {
      rec = null;
    }
  }

  @Override
  public boolean hasNext() {
    return rec != null;
  }

  @Override
  public T next() {
    if (rec == null) {
      throw new NoSuchElementException();
    } else {
      T result = rec;
      aggNext();
      return result;
    }
  }

  @Override
  public String toString() {
    return "TimeSeriesAggregatingIterator{" + "aggTime=" + aggTime + ", it="
            + it + ", rec=" + rec + ", maxTime=" + maxTime + '}';
  }

}
