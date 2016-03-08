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
package org.spf4j.zel.vm;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import javax.annotation.Nullable;
import org.spf4j.base.Pair;
import org.spf4j.base.Throwables;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("NOS_NON_OWNED_SYNCHRONIZATION")
public final class VMExecutor {

    public static class Lazy {

        static class ZelWorker extends ForkJoinWorkerThread {

            ZelWorker(final ForkJoinPool pool) {
                super(pool);
            }

        }

        static class DefaultForkJoinWorkerThreadFactory
                implements ForkJoinPool.ForkJoinWorkerThreadFactory {

            public ForkJoinWorkerThread newThread(final ForkJoinPool pool) {
                return new ZelWorker(pool);
            }
        }

        private static final ExecutorService DEF_EXEC
                = new ForkJoinPool(Integer.getInteger("zel.pool.maxThreadNr", org.spf4j.base.Runtime.NR_PROCESSORS),
                        new DefaultForkJoinWorkerThreadFactory(), new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(final Thread t, final Throwable e) {
                System.err.println(Throwables.toString(e, Throwables.Detail.STANDARD));
            }
        }, true);
//                 new LifoThreadPoolExecutorSQP("zel-pool",
//                 Integer.getInteger("zel.pool.coreThreadNr", 0),
//                 Integer.getInteger("zel.pool.maxThreadNr", org.spf4j.base.Runtime.NR_PROCESSORS),
//                 Integer.getInteger("zel.pool.maxIdleMillis", 60000),
//                 Integer.getInteger("zel.pool.queueLimit", Integer.MAX_VALUE));

        static {
            org.spf4j.base.Runtime.queueHook(0, new Runnable() {
                @Override
                public void run() {
                    DEF_EXEC.shutdown();
                }
            });
        }
        public static final VMExecutor DEFAULT = new VMExecutor(DEF_EXEC);
    }

    public interface Suspendable<T> extends Callable<T> {

        @Override
        T call() throws SuspendedException, ExecutionException, InterruptedException;

        List<VMFuture<Object>> getSuspendedAt();

    }

    public static <T> Suspendable<T> synchronize(final Suspendable<T> what) {
        return new Suspendable<T>() {

            private volatile boolean isRunning = false;

            @Override
            public T call() throws SuspendedException, ExecutionException, InterruptedException {
                if (!isRunning) {
                    synchronized (this) {
                        if (!isRunning) {
                            isRunning = true;
                            try {
                                return what.call();
                            } catch (SuspendedException e) {
                                isRunning = false;
                                throw e;
                            }
                        }
                    }
                }
                throw ExecAbortException.INSTANCE;
            }

            @Override
            public synchronized List<VMFuture<Object>> getSuspendedAt() {
                return what.getSuspendedAt();
            }
        };
    }

    private final Executor exec;

    public VMExecutor(final Executor exec) {
        this.exec = exec;
    }

    public <T> Future<T> submitNonSuspendable(final Callable<T> callable) {
        FutureTask task = new FutureTask(callable);
        exec.execute(task);
        return task;
    }

    public <T> Future<T> submit(final Suspendable<T> callable) {
        final VMFuture<T> resultFuture = new VMSyncFuture<>();
        submit(callable, resultFuture);
        return resultFuture;
    }

    /**
     * Returns a future that will not get notified when callable completes.
     *
     * @param <T>
     * @param callable
     * @return
     */
    public <T> Future<T> submitInternal(final Suspendable<T> callable) {
        final VMFuture<T> resultFuture = new VMASyncFuture<>();
        submit(callable, resultFuture);
        return resultFuture;
    }

    /**
     * Map from Future -> Suspendables suspended at this futures and their futures.
     */
    private final ConcurrentMap<VMFuture<Object>, List<Pair<Suspendable<Object>, VMFuture<Object>>>> futToSuspMap
            = new ConcurrentHashMap<>();

    @Nullable
    public List<Pair<Suspendable<Object>, VMFuture<Object>>> resumeSuspendables(final VMFuture<Object> future) {
        List<Pair<Suspendable<Object>, VMFuture<Object>>> suspended = futToSuspMap.remove(future);
        if (suspended != null) {
            for (Pair<Suspendable<Object>, VMFuture<Object>> susp : suspended) {
                submit(susp.getFirst(), susp.getSecond());
            }
        }
        return suspended;
    }

    private void addSuspendable(final VMFuture<Object> futureSuspendedFor,
            final Suspendable<Object> suspendedCallable, final VMFuture<Object> suspendedCallableFuture) {

        List<Pair<Suspendable<Object>, VMFuture<Object>>> suspended
                = futToSuspMap.get(futureSuspendedFor);
        if (suspended == null) {
            suspended = new LinkedList<>();
            List<Pair<Suspendable<Object>, VMFuture<Object>>> old
                    = futToSuspMap.putIfAbsent(futureSuspendedFor, suspended);
            if (old != null) {
                suspended = old;
            }
        }
        do {
            List<Pair<Suspendable<Object>, VMFuture<Object>>> newList
                    = new LinkedList<>(suspended);
            newList.add(Pair.of(suspendedCallable, suspendedCallableFuture));
            if (futToSuspMap.replace(futureSuspendedFor, suspended, newList)) {
                break;
            } else {
                suspended = futToSuspMap.get(futureSuspendedFor);
                if (suspended == null) {
                    suspended = new LinkedList<>();
                    List<Pair<Suspendable<Object>, VMFuture<Object>>> old
                            = futToSuspMap.putIfAbsent(futureSuspendedFor, suspended);
                    if (old != null) {
                        suspended = old;
                    }
                }
            }
        } while (true);
        if (futureSuspendedFor.isDone()) {
            resumeSuspendables(futureSuspendedFor);
        }
    }

    private <T> void submit(final Suspendable<T> callable, final VMFuture<T> future) {
        exec.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    T result = callable.call();
                    future.setResult(result);
                    resumeSuspendables((VMFuture<Object>) future);
                } catch (SuspendedException ex) {
                    for (VMFuture<Object> fut : callable.getSuspendedAt()) {
                        addSuspendable(fut,
                                (Suspendable<Object>) callable, (VMFuture<Object>) future);
                    }
                } catch (ExecutionException e) {
                    future.setExceptionResult(e);
                    resumeSuspendables((VMFuture<Object>) future);
                } catch (RuntimeException | InterruptedException e) {
                    future.setExceptionResult(new ExecutionException(e));
                    resumeSuspendables((VMFuture<Object>) future);
                }
            }
        });
    }

    @Override
    public String toString() {
        return "VMExecutor{" + "exec=" + exec + ", futToSuspMap=" + futToSuspMap + '}';
    }

}
