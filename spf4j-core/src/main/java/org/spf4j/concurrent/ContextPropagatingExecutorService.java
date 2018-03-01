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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.TimeoutDeadline;

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
    return wrapped.submit(ExecutionContexts.propagatingCallable(task));
  }

  @Override
  public final <T> Future<T> submit(final Runnable task, final T result) {
    return wrapped.submit(ExecutionContexts.propagatingRunnable(task), result);
  }

  @Override
  public final Future<?> submit(final Runnable task) {
    return wrapped.submit(ExecutionContexts.propagatingRunnable(task));
  }

  @Override
  public final <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks)
          throws InterruptedException {

    return wrapped.invokeAll(ExecutionContexts.propagatingCallables(tasks));
  }

  @Override
  public final <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks,
          final long timeout, final TimeUnit unit)
          throws InterruptedException {
    ExecutionContext current = ExecutionContexts.current();
    if (current == null) {
      return wrapped.invokeAll(tasks, timeout, unit);
    } else {
      TimeoutDeadline td;
      try {
        td = ExecutionContexts.computeTimeoutDeadline(current, unit, timeout);
      } catch (TimeoutException ex) {
        return Futures.timedOutFutures(tasks.size(), ex);
      }
      return wrapped.invokeAll(ExecutionContexts.deadlinedPropagatingCallables(tasks, current, td.getDeadlineNanos()),
              td.getTimeoutNanos(), TimeUnit.NANOSECONDS);
    }
  }

  @Override
  public final <T> T invokeAny(final Collection<? extends Callable<T>> tasks)
          throws InterruptedException, ExecutionException {
    return wrapped.invokeAny(ExecutionContexts.propagatingCallables(tasks));
  }

  @Override
  public final <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit)
          throws InterruptedException, ExecutionException, TimeoutException {
    ExecutionContext current = ExecutionContexts.current();
    if (current == null) {
      return wrapped.invokeAny(tasks, timeout, unit);
    } else {
      TimeoutDeadline td = ExecutionContexts.computeTimeoutDeadline(current, unit, timeout);
      return wrapped.invokeAny(ExecutionContexts.deadlinedPropagatingCallables(tasks, current, td.getDeadlineNanos()),
              td.getTimeoutNanos(), TimeUnit.NANOSECONDS);
    }
  }

  @Override
  public final void execute(final Runnable command) {
    wrapped.execute(ExecutionContexts.propagatingRunnable(command));
  }

  @Override
  public final String toString() {
    return "ContextPropagatingExecutorService{" + "wrapped=" + wrapped + '}';
  }

}
