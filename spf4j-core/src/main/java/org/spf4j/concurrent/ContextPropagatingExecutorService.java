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
package org.spf4j.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.TimeSource;

/**
 * @author Zoltan Farkas
 */
public class ContextPropagatingExecutorService implements ExecutorService {

  private final ExecutorService wrapped;

  public ContextPropagatingExecutorService(final ExecutorService wrapped) {
    this.wrapped = wrapped;
  }

  @Override
  public final void shutdown() {
    wrapped.shutdown();
  }

  @Override
  public final List<Runnable> shutdownNow() {
    return wrapped.shutdownNow();
  }

  @Override
  public final boolean isShutdown() {
    return wrapped.isShutdown();
  }

  @Override
  public final boolean isTerminated() {
    return wrapped.isTerminated();
  }

  @Override
  public final boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
    return wrapped.awaitTermination(timeout, unit);
  }

  @Override
  public final <T> Future<T> submit(final Callable<T> task) {
    ExecutionContext current = ExecutionContexts.current();
    if (current == null) {
      return wrapped.submit(task);
    } else {
      return wrapped.submit(new PropagatingCallable(task, current));
    }
  }

  @Override
  public final <T> Future<T> submit(final Runnable task, final T result) {
    ExecutionContext current = ExecutionContexts.current();
    if (current == null) {
      return wrapped.submit(task, result);
    } else {
      return wrapped.submit(new PropagatingRunnable(task, current), result);
    }

  }

  @Override
  public final Future<?> submit(final Runnable task) {
    ExecutionContext current = ExecutionContexts.current();
    if (current == null) {
      return wrapped.submit(task);
    } else {
      return wrapped.submit(new PropagatingRunnable(task, current));
    }
  }

  @Override
  public final <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks)
          throws InterruptedException {
    ExecutionContext current = ExecutionContexts.current();
    if (current == null) {
      return wrapped.invokeAll(tasks);
    } else {
      List<? extends Callable<T>> propagating = tasks.stream().map(
              (c) -> new PropagatingCallable<>(c, current))
              .collect(Collectors.toCollection(() -> new ArrayList<>(tasks.size())));
      return wrapped.invokeAll(propagating);
    }
  }

  @Override
  public final <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks,
          final long timeout, final TimeUnit unit)
          throws InterruptedException {
    ExecutionContext current = ExecutionContexts.current();
    if (current == null) {
      return wrapped.invokeAll(tasks, timeout, unit);
    } else {
      long deadlineNanos = computeDeadline(current, unit, timeout);
      List<? extends Callable<T>> propagating = tasks.stream().map(
              (c) -> new DeadlinedPropagatingCallable<>(c, current, deadlineNanos))
              .collect(Collectors.toCollection(() -> new ArrayList<>(tasks.size())));
      return wrapped.invokeAll(propagating, timeout, unit);
    }
  }

  @Override
  public final <T> T invokeAny(final Collection<? extends Callable<T>> tasks)
          throws InterruptedException, ExecutionException {
    ExecutionContext current = ExecutionContexts.current();
    if (current == null) {
      return wrapped.invokeAny(tasks);
    } else {
      List<? extends Callable<T>> propagating = tasks.stream()
              .map((c) -> new PropagatingCallable<>(c, current))
              .collect(Collectors.toCollection(() -> new ArrayList<>(tasks.size())));
      return wrapped.invokeAny(propagating);
    }
  }

  @Override
  public final <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit)
          throws InterruptedException, ExecutionException, TimeoutException {
    ExecutionContext current = ExecutionContexts.current();
    if (current == null) {
      return wrapped.invokeAny(tasks, timeout, unit);
    } else {
      long deadlineNanos = computeDeadline(current, unit, timeout);
      List<? extends Callable<T>> propagating = tasks.stream()
              .map((c) -> new DeadlinedPropagatingCallable<>(c, current, deadlineNanos))
              .collect(Collectors.toCollection(() -> new ArrayList<>(tasks.size())));
      return wrapped.invokeAny(propagating, timeout, unit);
    }
  }

  private static long computeDeadline(final ExecutionContext current, final TimeUnit unit, final long timeout) {
    long nanoTime = TimeSource.nanoTime();
    long ctxDeadlinenanos = current.getDeadlineNanos();
    long timeoutNanos = unit.toNanos(timeout);
    return (ctxDeadlinenanos - nanoTime < timeoutNanos) ? ctxDeadlinenanos :  nanoTime + timeoutNanos;
  }

  @Override
  public final void execute(final Runnable command) {
    ExecutionContext current = ExecutionContexts.current();
    if (current == null) {
      wrapped.execute(command);
    } else {
      wrapped.execute(new PropagatingRunnable(command, current));
    }
  }

  @Override
  public final String toString() {
    return "ContextPropagatingExecutorService{" + "wrapped=" + wrapped + '}';
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
