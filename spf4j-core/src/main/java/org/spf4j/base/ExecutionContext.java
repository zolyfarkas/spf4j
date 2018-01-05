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

import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

/**
 * @author Zoltan Farkas
 */
public final class ExecutionContext implements AutoCloseable {

  private static final ThreadLocal<ExecutionContext> DEADLINE = new ThreadLocal<ExecutionContext>();

  private final ExecutionContext parent;

  private long deadlineNanos;

  private ExecutionContext(final ExecutionContext parent, final long deadlineNanos) {
    this.deadlineNanos = deadlineNanos;
    this.parent = parent;
  }

  public long getDeadlineNanos() {
    return deadlineNanos;
  }

  public void setDeadlineNanos(final long deadlineNanos) {
    this.deadlineNanos = deadlineNanos;
  }

  @Nullable
  public static ExecutionContext current() {
    return DEADLINE.get();
  }

  public static ExecutionContext start(final long deadlineNanos) {
    ExecutionContext ctx = new ExecutionContext(DEADLINE.get(), deadlineNanos);
    DEADLINE.set(ctx);
    return ctx;
  }

  public static long getContextDeadlineNanos() {
    ExecutionContext ec = ExecutionContext.current();
    if (ec == null) {
      return System.nanoTime() + Long.MAX_VALUE;
    } else {
      return ec.getDeadlineNanos();
    }
  }

  public static long getSecondsToDeadline() {
    return TimeUnit.NANOSECONDS.toSeconds(getContextDeadlineNanos() - System.nanoTime());
  }

  public static long getMillisToDeadline() {
    return TimeUnit.NANOSECONDS.toSeconds(getContextDeadlineNanos() - System.nanoTime());
  }

  public static long getNanosToDeadline() {
    return getContextDeadlineNanos() - System.nanoTime();
  }

  public ExecutionContext getParent() {
    return parent;
  }

  @Override
  public void close()  {
    DEADLINE.set(this.parent);
  }

}
