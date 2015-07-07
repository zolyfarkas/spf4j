/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
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
 */
package org.spf4j.concurrent;

import com.google.common.base.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.base.Throwables;
import org.spf4j.base.Pair;

/**
 * Executor that will execute Callables with retry. This executor cannot be used inside a Completion service.
 * 
 *
 * @author zoly
 */
public class RetryExecutor<T> extends AbstractExecutorService {

    private final ExecutorService executionService;
    /**
     * can contain: DelayedCallables for execution. (delayed retries) or FailedExecutionResults for results.
     *
     */
    private final DelayQueue<FailedExecutionResult> executionEvents = new DelayQueue<>();
    private final ConcurrentMap<Callable<? extends Object>, Pair<Integer, ExecutionException>> executionAttempts;
    private final int nrImmediateRetries;
    private final int nrTotalRetries;
    private final long delayMillis;
    private volatile RetryManager retryManager;
    private Future<?> retryManagerFuture;
    private final Predicate<Exception> retryException;
    private final BlockingQueue<Future<T>> completionQueue;
    private final Object sync = new Object();

    private void startRetryManager() {
        if (this.retryManager == null) {
            synchronized (sync) {
                if (this.retryManager == null) {
                    this.retryManager = new RetryManager();
                    this.retryManagerFuture = DefaultExecutor.INSTANCE.submit(retryManager);
                }
            }
        }
    }

    private void shutdownRetryManager() {
        synchronized (sync) {
            if (this.retryManager != null) {
                retryManager.shutdown();
                retryManager = null;
            }
        }
    }

    /**
     * this class represents either a execution failure notification or a retry command.
     */
    private static class FailedExecutionResult implements Delayed {

        private final ExecutionException exception;
        private final FutureBean<Object> future;
        private final Callable<Object> callable;
        private final long delay;
        private final boolean isExecution;

        public FailedExecutionResult(final ExecutionException exception, final FutureBean future,
                final Callable callable, final long delay, final boolean isExecution) {
            this.exception = exception;
            this.future = future;
            this.callable = callable;
            this.delay = delay + System.currentTimeMillis();
            this.isExecution = isExecution;
        }

        @Override
        public long getDelay(final TimeUnit unit) {
            return unit.convert(delay - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(final Delayed o) {
            long tDelay = getDelay(TimeUnit.MILLISECONDS);
            long oDelay = o.getDelay(TimeUnit.MILLISECONDS);
            if (tDelay > oDelay) {
                return 1;
            } else if (tDelay < oDelay) {
                return -1;
            } else {
                return 0;
            }
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            } else {
                if (obj instanceof FailedExecutionResult) {
                    return this.compareTo((FailedExecutionResult) obj) == 0;
                } else {
                    return false;
                }
            }
        }

        @Override
        public int hashCode() {
            int hash = 7;
            return 53 * hash + (this.callable != null ? this.callable.hashCode() : 0);
        }

        public ExecutionException getException() {
            return exception;
        }

        public FutureBean<Object> getFuture() {
            return future;
        }

        public Callable<Object> getCallable() {
            return callable;
        }

        public boolean isIsExecution() {
            return isExecution;
        }

    }

    private class RetryableCallable<T> implements Callable<T>, Runnable {

        private final Callable<T> callable;
        private final FutureBean<T> future;

        public RetryableCallable(final Callable<T> callable, final FutureBean<T> future) {
            this.callable = callable;
            this.future = future;
        }

        public RetryableCallable(final Runnable task, final Object result, final FutureBean<T> future) {
            this.callable = new Callable() {

                @Override
                public Object call() throws Exception {
                    task.run();
                    return result;
                }
            };
            this.future = future;
        }

        @Override
        public T call() {
            try {
                T result = callable.call();
                if (future != null) {
                    future.setResult(result);
                }
                return null;
            } catch (Exception e) {
                if (retryException.apply(e)) {
                    startRetryManager();
                    executionEvents.add(
                            new FailedExecutionResult(new ExecutionException(e), future, callable, 0, false));
                } else {
                    future.setExceptionResult(new ExecutionException(e));
                }
                return null;
            }
        }

        @Override
        public void run() {
            call();
        }
    }

    private class RetryManager extends AbstractRunnable {

        public RetryManager() {
            super("RetryManager");
            isRunning = true;
        }

        public void shutdown() {
            isRunning = false;
        }

        private volatile boolean isRunning;

        @Override
        public void doRun() {
            while (isRunning) {
                try {
                    FailedExecutionResult event = executionEvents.take();
                    final Callable<Object> callable = event.getCallable();
                    if (event.isIsExecution()) {
                        executionService.execute(new RetryableCallable<>(callable, event.getFuture()));
                    } else {
                        Pair<Integer, ExecutionException> attemptsInfo = executionAttempts.get(callable);
                        if (attemptsInfo == null) {
                            attemptsInfo = Pair.of(1, event.getException());
                        } else {
                            if (attemptsInfo.getSecond() == null) {
                                attemptsInfo = Pair.of(attemptsInfo.getFirst() + 1, event.getException());
                            } else {
                                attemptsInfo = Pair.of(attemptsInfo.getFirst() + 1,
                                        Throwables.suppress(event.getException(), attemptsInfo.getSecond()));
                            }
                        }
                        int nrAttempts = attemptsInfo.getFirst();
                        if (nrAttempts > nrTotalRetries) {
                            executionAttempts.remove(callable);
                            event.getFuture().setExceptionResult(attemptsInfo.getSecond());
                        } else if (nrAttempts > nrImmediateRetries) {
                            executionAttempts.put(callable, attemptsInfo);
                            executionEvents.put(new FailedExecutionResult(attemptsInfo.getSecond(),
                                    event.getFuture(), callable, delayMillis, true));
                        } else {
                            executionAttempts.put(callable, attemptsInfo);
                            executionService.execute(
                                    new RetryableCallable<>(callable, event.getFuture()));
                        }
                    }
                } catch (InterruptedException ex) {
                    isRunning = false;
                    break;
                }
            }
        }

    }

    public RetryExecutor(final ExecutorService exec, final int nrImmediateRetries,
            final int nrTotalRetries, final long delayMillis, final Predicate<Exception> retryException,
            @Nullable final BlockingQueue<Future<T>> completionQueue) {
        executionService = exec;
        executionAttempts = new ConcurrentHashMap<>();
        this.nrImmediateRetries = nrImmediateRetries;
        this.nrTotalRetries = nrTotalRetries;
        this.delayMillis = delayMillis;
        this.retryException = retryException;
        this.completionQueue = completionQueue;
    }

    @Override
    public final void shutdown() {
        shutdownRetryManager();
        executionService.shutdown();
    }

    @Override
    public final List<Runnable> shutdownNow() {
        shutdownRetryManager();
        return executionService.shutdownNow();
    }

    @Override
    public final boolean isShutdown() {
        return executionService.isShutdown();
    }

    @Override
    public final boolean isTerminated() {
        return executionService.isTerminated();
    }

    @Override
    public final boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        try {
            this.retryManagerFuture.get();
        } catch (ExecutionException ex) {
            throw new RuntimeException(ex);
        }
        return executionService.awaitTermination(timeout, unit);
    }

    private FutureBean<T> createFutureBean() {
        if (completionQueue == null) {
            return new FutureBean<>();
        } else {
            return new FutureBean<T>() {
                @Override
                public void done() {
                    completionQueue.add(this);
                }
            };
        }
    }

    @Override
    public final <A> Future<A> submit(final Callable<A> task) {
        FutureBean<T> result = createFutureBean();
        executionService.execute(new RetryableCallable(task, result));
        return (Future<A>) result;
    }

    @Override
    public final <A> Future<A> submit(final Runnable task, final A result) {
        FutureBean<T> resultFuture = createFutureBean();
        executionService.execute(new RetryableCallable<>(task, result, resultFuture));
        return (Future<A>) resultFuture;
    }

    @Override
    public final Future<?> submit(final Runnable task) {
        FutureBean<?> resultFuture = createFutureBean();
        executionService.execute(new RetryableCallable(task, null, resultFuture));
        return resultFuture;
    }

    @Override
    public final <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        List<Future<T>> result = new ArrayList<>();
        for (Callable task : tasks) {
            result.add(this.submit(task));
        }
        for (Future fut : result) {
            try {
                fut.get();
                // CHECKSTYLE:OFF
            } catch (ExecutionException ex) {
                    //CHECKSTYLE:ON
                // Swallow exception for now, this sexception will be thoriwn when the client will call get again..
            }
        }
        return result;
    }

    @Override
    public final <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks,
            final long timeout, final TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public final <T> T invokeAny(final Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public final <T> T invokeAny(final Collection<? extends Callable<T>> tasks,
            final long timeout, final TimeUnit unit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public final void execute(final Runnable command) {
        executionService.execute(new RetryableCallable(command, null, null));
    }
}
