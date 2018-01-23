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
import gnu.trove.map.hash.THashMap;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @author Zoltan Farkas
 */
@ThreadSafe
public final class ExecutionContext implements AutoCloseable {

  private static final ThreadLocal<ExecutionContext> EXEC_CTX = new ThreadLocal<ExecutionContext>();

  /**
   * The previous context on Thread.
   */
  private final ExecutionContext tParent;

  private final ExecutionContext parent;

  private final long deadlineNanos;

  private Map<Class, Object> baggage;

  @SuppressWarnings("unchecked")
  private ExecutionContext(final ExecutionContext parent, final ExecutionContext tParent, final long deadlineNanos) {
    this.deadlineNanos = deadlineNanos;
    this.tParent = tParent;
    this.parent = parent;
    this.baggage = Collections.EMPTY_MAP;
  }

  public long getDeadlineNanos() {
    return deadlineNanos;
  }

  public long getUnitsToDeadline(final TimeUnit unit) {
    return unit.convert(deadlineNanos - TimeSource.nanoTime(), TimeUnit.NANOSECONDS);
  }

  public ExecutionContext subCtx() {
    return subCtx(deadlineNanos);
  }

  public ExecutionContext subCtx(final long timeout, final TimeUnit tu) {
    return subCtx(TimeSource.getDeadlineNanos(timeout, tu));
  }

  public ExecutionContext subCtx(final long pdeadlineNanos) {
    ExecutionContext xCtx = EXEC_CTX.get();
    ExecutionContext ctx = new ExecutionContext(this, xCtx, pdeadlineNanos);
    EXEC_CTX.set(ctx);
    return ctx;
  }

  @Nullable
  public static ExecutionContext current() {
    return EXEC_CTX.get();
  }

  @Nullable
  @Beta
  public synchronized <T> T put(@Nonnull final T data) {
    if (baggage.isEmpty()) {
      baggage = new THashMap<>(2);
    }
    return (T) baggage.put(data.getClass(), data);
  }

  @Nullable
  @Beta
  public synchronized <T> T get(@Nonnull final Class<T> clasz) {
    return (T) baggage.get(clasz);
  }

  /**
   * start a execution context.
   * @param deadlineNanos the deadline for this context. (System.nanotime)
   * @return the execution context.
   */
  public static ExecutionContext start(final long deadlineNanos) {
    ExecutionContext xCtx = EXEC_CTX.get();
    ExecutionContext ctx = new ExecutionContext(xCtx, xCtx, deadlineNanos);
    EXEC_CTX.set(ctx);
    return ctx;
  }

  /**
   * start a execution context.
   * @param timeout
   * @param tu
   * @return
   */
  public static ExecutionContext start(final long timeout, final TimeUnit tu) {
    return start(TimeSource.getDeadlineNanos(timeout, tu));
  }

  public static long getContextDeadlineNanos() {
    ExecutionContext ec = ExecutionContext.current();
    if (ec == null) {
      return TimeSource.nanoTime() + Long.MAX_VALUE;
    } else {
      return ec.getDeadlineNanos();
    }
  }

  public static long getSecondsToDeadline() {
    return TimeUnit.NANOSECONDS.toSeconds(getContextDeadlineNanos() - TimeSource.nanoTime());
  }

  public static long getMillisToDeadline() {
    return TimeUnit.NANOSECONDS.toMillis(getContextDeadlineNanos() - TimeSource.nanoTime());
  }

  public static long getNanosToDeadline() {
    return getContextDeadlineNanos() - TimeSource.nanoTime();
  }

  public ExecutionContext getParent() {
    return parent;
  }

  @Override
  public void close()  {
    EXEC_CTX.set(this.tParent);
  }

}
