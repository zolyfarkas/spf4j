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
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @author Zoltan Farkas
 */
@ThreadSafe
@ParametersAreNonnullByDefault
public final class ExecutionContexts {

  public static final long DEFAULT_TIMEOUT_NANOS =
          Long.getLong("spf4j.execContext.defaultTimeoutNanos", TimeUnit.HOURS.toNanos(8));

  private ExecutionContexts() {
  }

  private static final ThreadLocal<ExecutionContext> EXEC_CTX = new ThreadLocal<ExecutionContext>();

  private static final ExecutionContextFactory<ExecutionContext> CTX_FACTORY = initFactory();

  private static ExecutionContextFactory<ExecutionContext> initFactory() {

    String factoryClass = System.getProperty("spf4j.execContentFactoryClass");
    if (factoryClass == null) {

      return new ExecutionContextFactory<ExecutionContext>() {

        @Override
        public ExecutionContext start(final String name, final ExecutionContext parent,
                 final long deadlineNanos, final Runnable onClose) {
          return new BasicExecutionContext(name, parent, deadlineNanos) {
            @Override
            public void close() {
              onClose.run();
            }
          };
        }
      };
    } else {
      try {
        return ((Class<ExecutionContextFactory<ExecutionContext>>) Class.forName(factoryClass)).newInstance();
      } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
        throw new ExceptionInInitializerError(ex);
      }
    }
  }

  @Nullable
  public static ExecutionContext current() {
    return EXEC_CTX.get();
  }

  static void setCurrent(final ExecutionContext current) {
    EXEC_CTX.set(current);
  }

  /**
   * start a execution context.
   *
   * @param deadlineNanos the deadline for this context. (System.nanotime)
   * @return the execution context.
   */
  public static ExecutionContext start(final long deadlineNanos) {
    return start("anon", null, deadlineNanos);
  }

  /**
   * start a execution context.
   *
   * @param timeout
   * @param tu
   * @return
   */
  public static ExecutionContext start(final long timeout, final TimeUnit tu) {
    return start(TimeSource.getDeadlineNanos(timeout, tu));
  }

  public static ExecutionContext start(@Nullable final ExecutionContext parent, final long timeout, final TimeUnit tu) {
    return start(parent, TimeSource.getDeadlineNanos(timeout, tu));
  }

  public static ExecutionContext start(@Nullable final ExecutionContext parent) {
    return start(parent, parent != null ? parent.getDeadlineNanos() : TimeSource.nanoTime() + DEFAULT_TIMEOUT_NANOS);
  }

  public static ExecutionContext start(@Nullable final ExecutionContext parent, final long deadlineNanos) {
    return start("anon", parent, deadlineNanos);
  }

  public static ExecutionContext start(final String name, final long deadlineNanos) {
    return start(name, null, deadlineNanos);
  }

  public static ExecutionContext start(final String name,
          @Nullable final ExecutionContext parent) {
    return start(name, parent, parent != null ? parent.getDeadlineNanos()
            : TimeSource.nanoTime() + DEFAULT_TIMEOUT_NANOS);
  }

  public static ExecutionContext start(final String name,
          @Nullable final ExecutionContext parent, final long deadlineNanos) {
    ExecutionContext xCtx = EXEC_CTX.get();
    ExecutionContext nCtx = CTX_FACTORY.start(name, parent == null ? xCtx : parent,
            deadlineNanos, () -> ExecutionContexts.setCurrent(xCtx));
    EXEC_CTX.set(nCtx);
    return nCtx;
  }

  public static long getContextDeadlineNanos() {
    ExecutionContext ec = ExecutionContexts.current();
    if (ec == null) {
      return TimeSource.nanoTime() + DEFAULT_TIMEOUT_NANOS;
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

}
