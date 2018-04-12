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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.ThreadSafe;

/**
 * The simplest execution context possible.
 *
 * @author Zoltan Farkas
 */
@ThreadSafe
@ParametersAreNonnullByDefault
public class BasicExecutionContext implements ExecutionContext {

  private final String name;

  private final ExecutionContext parent;

  private final long startTimeNanos;

  private final long deadlineNanos;

  private final Runnable onClose;

  private Map<Object, Object> baggage;

  private boolean isClosed = false;

  @SuppressWarnings("unchecked")
  public BasicExecutionContext(final String name, @Nullable final ExecutionContext parent,
          final long startTimeNanos, final long deadlineNanos, final Runnable onClose) {
    this.isClosed = false;
    this.name = name;
    this.onClose = onClose;
    this.startTimeNanos = startTimeNanos;
    if (parent != null) {
      long parentDeadline = parent.getDeadlineNanos();
      if (parentDeadline < deadlineNanos) {
        this.deadlineNanos = parentDeadline;
      } else {
        this.deadlineNanos = deadlineNanos;
      }
    } else {
      this.deadlineNanos = deadlineNanos;
    }
    this.parent = parent;
    this.baggage = Collections.EMPTY_MAP;
  }

  @Override
  public final String getName() {
    return name;
  }

  @Override
  public final long getDeadlineNanos() {
    return deadlineNanos;
  }

  public final long getStartTimeNanos() {
    return startTimeNanos;
  }

  @Nullable
  @Beta
  @Override
  public final synchronized <T> T put(@Nonnull final Object key, @Nonnull final T data) {
    if (baggage == Collections.EMPTY_MAP) {
      baggage = new HashMap<>(4);
    }
    return (T) baggage.put(key, data);
  }

  @Nullable
  @Beta
  @Override
  public final synchronized Object get(@Nonnull final Object key) {
    Object res = baggage.get(key);
    if (res == null) {
      if (parent != null) {
        return parent.get(key);
      } else {
        return null;
      }
    } else {
      return res;
    }
  }

  @Override
  @Nullable
  public final synchronized <K, V> V compute(@Nonnull final K key, final BiFunction<K, V, V> compute) {
    if (baggage == Collections.EMPTY_MAP) {
      baggage = new HashMap(4);
    }
    return (V) baggage.compute(key, (BiFunction) compute);
  }

  @Override
  public final ExecutionContext getParent() {
    return parent;
  }

  /**
   * Close might be overridable to close any additional stuff added in the extendsd class.
   */
  @Override
  public void close() {
    if (!isClosed) {
      onClose.run();
      isClosed = true;
    }
  }

  /**
   * Overwrite as needed for debug string.
   */
  @Override
  public String toString() {
    return "BasicExecutionContext{" + "name=" + name + ", parent="
            + parent + ", deadline=" + Timing.getCurrentTiming().fromNanoTimeToInstant(deadlineNanos)
            + ", baggage=" + baggage + '}';
  }

}
