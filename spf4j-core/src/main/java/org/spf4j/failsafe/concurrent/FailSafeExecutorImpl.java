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
package org.spf4j.failsafe.concurrent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.failsafe.RetryPredicate;
import org.spf4j.base.UncheckedExecutionException;
import org.spf4j.concurrent.DefaultExecutor;

/**
 * Executor that will call Callables with retry. This executor cannot be used inside a Completion service.
 *
 *
 * @author zoly
 */
public final class FailSafeExecutorImpl implements FailSafeExecutor {

  private static final Logger LOG = LoggerFactory.getLogger(FailSafeExecutorImpl.class);

  private static final Future<?> SHUTDOWN = new Future() {
    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDone() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object get()  {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object get(final long timeout, final TimeUnit unit) {
      throw new UnsupportedOperationException();
    }
  };

  private final ExecutorService executionService;
  private final DelayQueue<DelayedTask<RetryFutureTask<?>>> executionEvents = new DelayQueue<>();
  private volatile Future<?> retryManagerFuture;
  private final Object sync = new Object();

  private void startRetryManager() {
    Future<?> rm = retryManagerFuture;
    if (rm == null) {
      synchronized (sync) {
        rm = retryManagerFuture;
        if (rm == null) {
          rm = DefaultExecutor.INSTANCE.submit(new RetryManager());
          this.retryManagerFuture = rm;
          LOG.debug("Retry manager started {}", rm);
        }
      }
    }
  }

  private void shutdownRetryManager() {
    synchronized (sync) {
      Future<?> rmf = this.retryManagerFuture;
      if (rmf != null && rmf != SHUTDOWN) {
        rmf.cancel(true);
        retryManagerFuture = SHUTDOWN;
      }
    }
  }

  private class RetryManager extends AbstractRunnable {

    RetryManager() {
      super("RetryManager");
    }

    @Override
    public void doRun() {
      Thread thread = Thread.currentThread();
      while (!thread.isInterrupted()) {
        try {
          DelayedTask<RetryFutureTask<?>>  event = executionEvents.poll(1, TimeUnit.MINUTES);
          if (event != null) {
            RetryFutureTask<?> runnable = event.getRunnable();
            executionService.execute(runnable);
          }
        } catch (InterruptedException ex) {
          break;
        }
      }
    }

  }

  public FailSafeExecutorImpl(final ExecutorService exec) {
    executionService = exec;
  }

  @Override
  public void close() throws InterruptedException {
    synchronized (sync) {
      shutdownRetryManager();
      Future<?> rmf = this.retryManagerFuture;
      if (rmf != null && rmf != SHUTDOWN) {
        try {
          rmf.get();
        } catch (ExecutionException ex) {
          throw new UncheckedExecutionException(ex);
        }
      }
    }
  }

  public void initiateClose() {
    shutdownRetryManager();
  }


  @Override
  public <A> Future<A> submit(final Callable<? extends A> task,
          final RetryPredicate<A, ? extends Callable<? extends A>> predicate) {
    RetryFutureTask<A> result =
            new RetryFutureTask(task, (RetryPredicate<A, Callable<? extends A>>) predicate, executionEvents,
              this::startRetryManager);

    executionService.execute(result);
    return (Future<A>) result;
  }

  @Override
  public <A> CompletableFuture<A> submitRx(final Callable<? extends A> task,
          final RetryPredicate<A, ? extends Callable<? extends A>> predicate) {
    InterruptibleCompletableFuture<A> result = new InterruptibleCompletableFuture<>();
    ConsumableRetryFutureTask<A> rft =
            new ConsumableRetryFutureTask<>(f -> {
              try {
                A r = f.get();
                result.complete(r);
              } catch (Exception ex)  {
                result.completeExceptionally(ex);
              }
            }, (Callable<A>) task,
                    (RetryPredicate<A, Callable<? extends A>>) predicate, executionEvents,
              this::startRetryManager);
    result.setToCancel(rft);
    executionService.execute(rft);
    return result;
  }

  @Override
  public <A> Future<A> submit(final Callable<? extends A> task,
          final RetryPredicate<A, ? extends Callable<? extends A>> predicate,
          final int nrHedges, final long hedgeDelay, final TimeUnit unit) {
    if (nrHedges <= 0) {
      return submit(task, predicate);
    }
    int nrFut = nrHedges + 1;
    final Future[] futures = new Future[nrFut];
    ArrayBlockingQueue<Future<A>> queue = new ArrayBlockingQueue<>(1);
    FirstFuture<A> result = new FirstFuture<A>(futures, queue);
    ConsumableRetryFutureTask<A> future =  new ConsumableRetryFutureTask(result, task,
            (RetryPredicate<A, Callable<? extends A>>) predicate, executionEvents, this::startRetryManager);
    startRetryManager();
    futures[0] = future;
    Runnable[] submits = new Runnable[nrFut];
    submits[0] = () -> executionService.execute(future);
    for (int i = 1; i < nrFut; i++) {
      ConsumableRetryFutureTask<A> f = new ConsumableRetryFutureTask(
                result, task, (RetryPredicate) predicate, executionEvents,
                this::startRetryManager);
      futures[i] = f;
      if (hedgeDelay > 0) {
        DelayedTask<RetryFutureTask<?>>  delayedExecution = new DelayedTask<RetryFutureTask<?>>(
                f, unit.toNanos(hedgeDelay));
        f.setExec(delayedExecution);
        submits[i] = () -> executionEvents.add(delayedExecution);
      } else {
        submits[i] = () -> executionService.execute(f);
      }
    }
    for (Runnable submit : submits) {
      submit.run();
    }
    return result;
  }

  @Override
  public <A> CompletableFuture<A> submitRx(final Callable<? extends A> task,
          final RetryPredicate<A, ? extends Callable<? extends A>> predicate,
          final int nrHedges, final long hedgeDelay, final TimeUnit unit) {
    if (nrHedges <= 0) {
      return submitRx(task, predicate);
    }
    InterruptibleCompletableFuture<A> result = new InterruptibleCompletableFuture<>();
    int nrFut = nrHedges + 1;
    final Future<A>[] futures = new Future[nrFut];
    ArrayBlockingQueue<Future<A>> queue = new ArrayBlockingQueue<>(1);
    FirstFuture<A> resultX = new FirstFuture<A>(futures, queue) {
      @Override
      @SuppressFBWarnings("NOS_NON_OWNED_SYNCHRONIZATION")
      public void accept(final Future<A> finished) {
        super.accept(finished);
        synchronized (this) {
          if (!result.isDone()) {
            try {
              A r = finished.get();
              result.complete(r);
            } catch (Exception ex) {
              result.completeExceptionally(ex);
            }
          }
        }
      }
    };
    result.setToCancel(resultX);
    ConsumableRetryFutureTask<A> future =  new ConsumableRetryFutureTask(resultX, task,
            (RetryPredicate<A, Callable<? extends A>>) predicate, executionEvents, this::startRetryManager);
    startRetryManager();
    futures[0] = future;
    Runnable[] submits = new Runnable[nrFut];
    submits[0] = () -> executionService.execute(future);
    for (int i = 1; i < nrFut; i++) {
      ConsumableRetryFutureTask<A> f = new ConsumableRetryFutureTask(
                resultX, task, (RetryPredicate) predicate, executionEvents,
                this::startRetryManager);
      futures[i] = f;
      if (hedgeDelay > 0) {
        DelayedTask<RetryFutureTask<?>>  delayedExecution = new DelayedTask<RetryFutureTask<?>>(
                f, unit.toNanos(hedgeDelay));
        f.setExec(delayedExecution);
        submits[i] = () -> executionEvents.add(delayedExecution);
      } else {
        submits[i] = () -> executionService.execute(f);
      }
    }
    for (Runnable submit : submits) {
      submit.run();
    }
    return result;
  }



  @Override
  public <A> void execute(final Callable<? extends A> task,
          final RetryPredicate<A, ? extends Callable<? extends A>> predicate) {
    RetryFutureTask<A> result = new RetryFutureTask(task, predicate, executionEvents, this::startRetryManager);
    executionService.execute(result);
  }


  @Override
  public String toString() {
    return "RetryExecutor{" + "executionService=" + executionService + ", executionEvents=" + executionEvents
            + ", retryManagerFuture=" + retryManagerFuture
            + ", sync=" + sync + '}';
  }

  private static class FirstFuture<T> implements Future<T>, Consumer<Future<T>> {

    private final Future<T>[] futures;
    private final BlockingQueue<Future<T>> queue;
    private boolean first = true;
    private final int nrHedges;

    FirstFuture(final Future<T>[] futures,
            final BlockingQueue<Future<T>> queue) {
      this.futures = futures;
      this.queue = queue;
      this.nrHedges = futures.length - 1;
    }

    @Override
    @SuppressFBWarnings("NOS_NON_OWNED_SYNCHRONIZATION") // Actually I own it...
    public void accept(final Future<T> finished) {
      synchronized (this) {
        if (first) {
          first = false;
          queue.add(finished);
          for (int i = 0;  i < nrHedges; i++) {
            Future f = futures[i];
            if (f != finished) {
              f.cancel(true);
            }
          }
        } else {
          return;
        }
      }
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
      boolean result = true;
      for (int i = 0, l =  futures.length; i < l; i++) {
        Future f  = futures[i];
        if (!f.cancel(mayInterruptIfRunning)) {
          result =  false;
        }
      }
      return result;
    }

    @Override
    public boolean isCancelled() {
      boolean result = true;
      for (int i = 0, l =  futures.length; i < l; i++) {
        Future f  = futures[i];
        if (!f.isCancelled()) {
          result =  false;
        }
      }
      return result;
    }

    @Override
    public boolean isDone() {
      boolean result = true;
      for (int i = 0, l =  futures.length; i < l; i++) {
        Future f  = futures[i];
        if (!f.isDone()) {
          result =  false;
        }
      }
      return result;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
      return queue.take().get();
    }

    @Override
    public T get(final long timeout, final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
      Future<T> poll = queue.poll(timeout, unit);
      if (poll == null) {
        throw new TimeoutException("Timed out after " + timeout + " " + unit);
      } else {
        return poll.get();
      }
    }
  }

  private static class ConsumableRetryFutureTask<T> extends RetryFutureTask<T> {

    private final Consumer<Future<T>> consumer;

    ConsumableRetryFutureTask(final Consumer<Future<T>> consumer, final Callable<T> callable,
            final RetryPredicate<T, Callable<? extends T>> retryPredicate,
            final DelayQueue<DelayedTask<RetryFutureTask<?>>> delayedTasks,
            final Runnable onRetry) {
      super(callable, retryPredicate, delayedTasks, onRetry);
      this.consumer = consumer;
    }

    @Override
    public void done() {
      consumer.accept(this);
    }
  }

  private static class InterruptibleCompletableFuture<A> extends CompletableFuture<A> {

    private volatile Future<A> toCancel;

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
      boolean result = super.cancel(mayInterruptIfRunning);
      Future<A> tc = toCancel;
      if (tc != null) {
        result = tc.cancel(mayInterruptIfRunning);
      }
      return result;
    }

    public void setToCancel(final Future<A> toCancel) {
      this.toCancel = toCancel;
    }

    @Override
    public String toString() {
      return "InterruptibleCompletableFuture{" + "toCancel=" + toCancel + '}';
    }

  }

}
