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

import com.google.common.annotations.Beta;
import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.Signed;

/**
 * @author Zoltan Farkas
 */
@CleanupObligation
@ParametersAreNonnullByDefault
public interface ExecutionContext extends AutoCloseable {

  @DischargesObligation
  void close();

  @Nonnull
  String getName();

  long getStartTimeNanos();

  long getDeadlineNanos();

  @Nullable
  ExecutionContext getParent();

  @Nonnegative
  default long getTimeToDeadline(final TimeUnit unit) throws TimeoutException {
     long result = getTimeRelativeToDeadline(unit);
     if (result <= 0) {
       throw new TimeoutException("Deadline exceeded by " + (-result) + ' ' + unit);
     }
     return result;
  }

  @Nonnegative
  default long getUncheckedTimeToDeadline(final TimeUnit unit) {
     long result = getTimeRelativeToDeadline(unit);
     if (result <= 0) {
       throw new UncheckedTimeoutException("Deadline exceeded by " + (-result) + ' ' + unit);
     }
     return result;
  }

  @Signed
  default long getTimeRelativeToDeadline(final TimeUnit unit)  {
     return unit.convert(getDeadlineNanos() - TimeSource.nanoTime(), TimeUnit.NANOSECONDS);
  }

  @Nonnegative
  default long getMillisToDeadline() throws TimeoutException {
    return getTimeToDeadline(TimeUnit.MILLISECONDS);
  }

  @Nonnegative
  default int getSecondsToDeadline() throws TimeoutException {
    long secondsToDeadline = getTimeToDeadline(TimeUnit.SECONDS);
    if (secondsToDeadline > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    } else {
      return (int) secondsToDeadline;
    }
  }


  /**
   * Method to get context associated data.
   * if current context does not have baggage, the parent context is queried.
   * @param <T> type of baggage.
   * @param key key of baggage.
   * @param clasz class of baggage value.
   * @return the baggage
   */
  @Nullable
  @Beta
  default <T> T get(Object key, Class<T> clasz) {
    return (T) get(key);
  }


  /**
   * Method to get context associated data.
   * if current context does not have baggage, the parent context is queried.
   * @param <T> type of data.
   * @param key key of data.
   * @return the data
   */
  @Nullable
  @Beta
  Object get(Object key);


  /**
   * Method to put context associated data.
   * @param <T> type of data.
   * @param key the key of data.
   * @param data the data.
   * @return existing data if there.
   */
  @Nullable
  @Beta
  <T> T put(Object key, T data);


  /**
   * Method to put context associated data to the root context.
   * @param <T> type of data.
   * @param key the key of data.
   * @param data the data.
   * @return existing data if there.
   */
  @Nullable
  @Beta
  default <T> T putToRoot(final Object key, final T data) {
    ExecutionContext curr = this;
    ExecutionContext parent;
    while ((parent = curr.getParent()) != null) {
      curr = parent;
    }
    return curr.put(key, data);
  }

  /**
   * Compute context associated data.
   * @param <K>
   * @param <V>
   * @param key
   * @param compute
   * @return
   */
  @Beta
  @Nullable
  <K, V> V compute(K key, BiFunction<K, V, V> compute);

}
