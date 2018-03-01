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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.Signed;
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
    ExecutionContextFactory<ExecutionContext> factory;
    if (factoryClass == null) {
      factory =  new BasicExecutionContextFactory();
    } else {
      try {
        factory = ((Class<ExecutionContextFactory<ExecutionContext>>) Class.forName(factoryClass)).newInstance();
      } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
        throw new ExceptionInInitializerError(ex);
      }
    }
    String factoryWrapperClass = System.getProperty("spf4j.execContentFactoryWrapperClass");
    if (factoryWrapperClass != null) {
      try {
        factory = (ExecutionContextFactory<ExecutionContext>) Class.forName(factoryWrapperClass)
                .getConstructor(ExecutionContextFactory.class).newInstance(factory);
       } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
               | NoSuchMethodException | InvocationTargetException ex) {
        throw new ExceptionInInitializerError(ex);
      }
    }
    return factory;
  }

  public static ExecutionContextFactory<ExecutionContext> getContextFactory() {
    return CTX_FACTORY;
  }

  @Nullable
  public static ExecutionContext current() {
    return EXEC_CTX.get();
  }

  private static void setCurrent(@Nullable final ExecutionContext current) {
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
    ExecutionContext nCtx;
    if (xCtx == null) {
      nCtx = CTX_FACTORY.startThreadRoot(name, parent,
            deadlineNanos, () -> ExecutionContexts.setCurrent(null));
    } else {
      nCtx = CTX_FACTORY.start(name, parent == null ? xCtx : parent,
            deadlineNanos, () -> ExecutionContexts.setCurrent(xCtx));
    }
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

  @Signed
  public static long getTimeRelativeToDeadline(final TimeUnit unit) {
    return unit.convert(getContextDeadlineNanos() - TimeSource.nanoTime(), TimeUnit.NANOSECONDS);
  }

  @Nonnegative
  public static long getTimeToDeadline(final TimeUnit unit) throws TimeoutException {
    long timeRelativeToDeadline = getTimeRelativeToDeadline(unit);
    if (timeRelativeToDeadline <= 0) {
      throw new TimeoutException("Deadline exceeded by " + (-timeRelativeToDeadline) + ' ' + unit);
    }
    return timeRelativeToDeadline;
  }

  @Nonnegative
  public static long getMillisToDeadline() throws TimeoutException {
    return getTimeToDeadline(TimeUnit.MILLISECONDS);
  }

  @Nonnegative
  public static int getSecondsToDeadline() throws TimeoutException {
    long secondsToDeadline = getTimeToDeadline(TimeUnit.SECONDS);
    if (secondsToDeadline > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    } else {
      return (int) secondsToDeadline;
    }
  }

  public static long computeDeadline(final ExecutionContext current, final TimeUnit unit, final long timeout) {
    long nanoTime = TimeSource.nanoTime();
    long ctxDeadlinenanos = current.getDeadlineNanos();
    long timeoutNanos = unit.toNanos(timeout);
    return (ctxDeadlinenanos - nanoTime < timeoutNanos) ? ctxDeadlinenanos : nanoTime + timeoutNanos;
  }


  public static TimeoutDeadline computeTimeoutDeadline(final ExecutionContext current,
          final TimeUnit unit, final long timeout) throws TimeoutException {
    long nanoTime = TimeSource.nanoTime();
    long ctxDeadlinenanos = current.getDeadlineNanos();
    long timeoutNanos = unit.toNanos(timeout);
    long contextTimeoutNanos = ctxDeadlinenanos - nanoTime;
    return (contextTimeoutNanos < timeoutNanos)
            ? TimeoutDeadline.of(contextTimeoutNanos, ctxDeadlinenanos)
            : TimeoutDeadline.of(timeoutNanos, nanoTime + timeoutNanos);
  }


  private static class BasicExecutionContextFactory implements ExecutionContextFactory<ExecutionContext> {

    @Override
    public ExecutionContext start(final String name, final ExecutionContext parent,
            final long deadlineNanos, final Runnable onClose) {
      return new BasicExecutionContext(name, parent, deadlineNanos, onClose);
    }

  }

  public static <T> Callable<T> propagatingCallable(final Callable<T> callable) {
    ExecutionContext current = current();
    return current == null ? callable : propagatingCallable(callable, current);
  }

  public static <T> Callable<T> propagatingCallable(final Callable<T> callable, final ExecutionContext ctx) {
    return new PropagatingCallable<T>(callable, ctx);
  }

  public static <T> Collection<? extends Callable<T>> propagatingCallables(
          final Collection<? extends Callable<T>> tasks) {
        ExecutionContext current = current();
        return current == null ? tasks : propagatingCallables(tasks, current);
  }

  public static <T> Collection<? extends Callable<T>> propagatingCallables(
          final Collection<? extends Callable<T>> tasks,
          final ExecutionContext ctx) {
    return tasks.stream().map(
              (c) -> new PropagatingCallable<>(c, ctx))
              .collect(Collectors.toCollection(() -> new ArrayList<>(tasks.size())));
  }

  public static <T> Collection<? extends Callable<T>> deadlinedPropagatingCallables(
          final Collection<? extends Callable<T>> tasks,
          final ExecutionContext ctx, final long deadlineNanos) {
    return tasks.stream().map(
              (c) -> new DeadlinedPropagatingCallable<>(c, ctx, deadlineNanos))
              .collect(Collectors.toCollection(() -> new ArrayList<>(tasks.size())));
  }


  public static <T> Callable<T> deadlinedPropagatingCallable(final Callable<T> callable,
          final ExecutionContext ctx, final long deadlineNanos) {
    return new DeadlinedPropagatingCallable<T>(callable, ctx, deadlineNanos);
  }

  public static Runnable propagatingRunnable(final Runnable runnable) {
    ExecutionContext current = current();
    return current == null ? runnable : propagatingRunnable(runnable, current);
  }

  public static Runnable propagatingRunnable(final Runnable runnable, final ExecutionContext ctx) {
    return new PropagatingRunnable(runnable, ctx);
  }

  private static final class PropagatingCallable<T> implements Callable<T> {

    private final Callable<T> task;
    private final ExecutionContext current;

    PropagatingCallable(final Callable<T> task, final ExecutionContext current) {
      this.task = task;
      this.current = current;
    }

    @Override
    public T call() throws Exception {
      try (ExecutionContext ctx = ExecutionContexts.start(task.toString(), current)) {
        return task.call();
      }
    }
  }

  private static final class DeadlinedPropagatingCallable<T> implements Callable<T> {

    private final Callable<T> task;
    private final ExecutionContext current;
    private final long deadlineNanos;

    DeadlinedPropagatingCallable(final Callable<T> task, final ExecutionContext current,
            final long deadlineNanos) {
      this.task = task;
      this.current = current;
      this.deadlineNanos = deadlineNanos;
    }

    @Override
    public T call() throws Exception {
      try (ExecutionContext ctx = ExecutionContexts.start(task.toString(), current, deadlineNanos)) {
        return task.call();
      }
    }
  }


  private static final class PropagatingRunnable implements Runnable {

    private final Runnable task;
    private final ExecutionContext current;

    PropagatingRunnable(final Runnable task, final ExecutionContext current) {
      this.task = task;
      this.current = current;
    }

    @Override
    public void run() {
      try (ExecutionContext ctx = ExecutionContexts.start(task.toString(), current)) {
        task.run();
      }
    }
  }




}
