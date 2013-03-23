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

package org.spf4j.base;

import com.google.common.base.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import java.util.concurrent.TimeoutException;
import javax.annotation.Nullable;

/**
 * Executor that will execute Callables with retry.
 * This executor cannot be used inside a Completion service.
 * as such it allow
 * 
 * @author zoly
 */
public class RetryExecutor<T> implements ExecutorService{

    private final ExecutorService executionService;
    /**
     * can contain: DelayedCallables for execution. (delayed retries) or
     * FailedExecutionResults for results.
     *
     */
    private final DelayQueue<FailedExecutionResult> executionEvents = new DelayQueue<FailedExecutionResult>();
    private final ConcurrentMap<Callable<? extends Object>, Pair<Integer, ExecutionException>> executionAttempts;
    private final ExecutorService exec;
    private final int nrImmediateRetries;
    private final int nrTotalRetries;
    private final long delayMillis;
    private volatile RetryManager retryManager;
    private final Predicate<Exception> retryException;
    private final BlockingQueue<Future<T>> completionQueue;

    private void startRetryManager() {
        if (this.retryManager == null) {
            synchronized(this) {
                if (this.retryManager == null) {
                    this.retryManager = new RetryManager();
                    this.retryManager.start();
                }
            }
        }
    }

    private void shutdownRetryManager()  {
        synchronized(this) {
            if (this.retryManager != null) {
                this.retryManager.interrupt();
                try {
                    this.retryManager.join();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    /**
     * this class represents either a execution failure notification or a retry
     * command.
     */
    private static class FailedExecutionResult implements Delayed {

        private final ExecutionException exception;
        private final FutureBean<Object> future;
        private final Callable<Object> callable;
        private final long delay;
        private final boolean isExecution;

        public FailedExecutionResult(ExecutionException exception, FutureBean future, 
                Callable callable, long delay, boolean isExecution) {
            this.exception = exception;
            this.future = future;
            this.callable = callable;
            this.delay = delay + System.currentTimeMillis();
            this.isExecution = isExecution;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(delay - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
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
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            } else {
                if (obj instanceof Delayed) {
                    return this.compareTo((Delayed)obj) == 0;
                } else {
                    return false;
                }
            }
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + (this.callable != null ? this.callable.hashCode() : 0);
            return hash;
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
    }

    private class RetryableCallable<T> implements Callable<T>, Runnable {

        private final Callable callable;
        private final FutureBean<T> future;

        public RetryableCallable(Callable<T> callable, FutureBean<T> future) {
            this.callable = callable;
            this.future = future;
        }
        
        public RetryableCallable(final Runnable task, final Object result, FutureBean<T> future) {
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
                Object result = callable.call();
                if (future != null) {
                    future.setResult(result);
                }
                return null;
            } catch (Exception e) {
                if (retryException.apply(e)) {
                    startRetryManager();
                    executionEvents.add(new FailedExecutionResult(new ExecutionException(e), future, callable, 0, false));
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

    private class RetryManager extends Thread {

        public RetryManager() {
            super("RetryManager");
        }

        public void shutdown() {
            this.interrupt();
            try {
                this.join();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void run() {
            Thread managerThread = Thread.currentThread();
            while (!managerThread.isInterrupted()) {
                try {
                    FailedExecutionResult event = executionEvents.take();
                    if (event.isExecution) {
                        executionService.execute(new RetryableCallable<Object>(event.getCallable(), event.getFuture()));
                    } else {
                        Pair<Integer, ExecutionException> attemptsInfo = executionAttempts.get(event.getCallable());
                        if (attemptsInfo == null) {
                            attemptsInfo = Pair.of(1, event.getException());
                        } else {
                            attemptsInfo = Pair.of(attemptsInfo.getFirst() + 1,
                                    Exceptions.chain(event.getException(), attemptsInfo.getSecond()));
                        }
                        int nrAttempts = attemptsInfo.getFirst();
                        if (nrAttempts > nrTotalRetries) {
                            executionAttempts.remove(event.getCallable());
                            event.getFuture().setExceptionResult(attemptsInfo.getSecond());
                        } else if (nrAttempts > nrImmediateRetries) {
                            executionAttempts.put(event.getCallable(), attemptsInfo);
                            executionEvents.put(new FailedExecutionResult(attemptsInfo.getSecond(), event.getFuture(),
                                    event.getCallable(), delayMillis, true));
                        } else {
                            executionAttempts.put(event.getCallable(), attemptsInfo);
                            executionService. execute(new RetryableCallable<Object>(event.getCallable(), event.getFuture()));
                        }
                    }


                } catch (InterruptedException ex) {
                    managerThread.interrupt();
                    break;
                }
            }
        }
    }

    public RetryExecutor(ExecutorService exec, int nrImmediateRetries,
            int nrTotalRetries, long delayMillis,  Predicate<Exception> retryException, 
            @Nullable BlockingQueue<Future<T>> completionQueue) {
        executionService = exec;
        executionAttempts = new ConcurrentHashMap<Callable<? extends Object>, Pair<Integer, ExecutionException>>();
        this.nrImmediateRetries = nrImmediateRetries;
        this.nrTotalRetries = nrTotalRetries;
        this.delayMillis = delayMillis;
        this.exec = exec;
        this.retryException = retryException;
        this.completionQueue = completionQueue;
    }

    @Override
    public void shutdown() {
        shutdownRetryManager();
        exec.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdownRetryManager();
        return exec.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return exec.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return exec.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return exec.awaitTermination(timeout, unit);
    }

    private  FutureBean<T> createFutureBean() {
        if (completionQueue == null) {
            return new FutureBean<T>();
        } else {
            return new FutureBean<T>(){
                @Override
                public void done() {
                    completionQueue.add(this);
                }
            };
        }
    }
    
    
    @Override
    public <A> Future<A> submit(Callable<A> task) {
        FutureBean<T> result = createFutureBean();
        executionService.execute(new RetryableCallable(task, result));
        return (Future<A>) result;
    }
   

    @Override
    public <A> Future<A> submit(Runnable task, A result) {
        FutureBean<T> resultFuture = createFutureBean();
        executionService.execute(new RetryableCallable<T>(task, result, resultFuture));
        return (Future<A>) resultFuture;
    }

    @Override
    public Future<?> submit(Runnable task) {
        FutureBean<?> resultFuture = createFutureBean();
        executionService.execute(new RetryableCallable(task, null, resultFuture));
        return resultFuture;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        List<Future<T>> result = new ArrayList<Future<T>>();
        for (Callable task: tasks) {
            result.add(this.submit(task));
        }
        for(Future fut: result) {
            try {
                fut.get();
            } catch (ExecutionException ex) {
                // Swallow exception for now, this sexception will be thoriwn when the client will call get again..
            }
        }
        return result;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void execute(Runnable command) {
        executionService.execute(new RetryableCallable(command, null, null));
    }
}
