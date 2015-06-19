
/*
 * Copyright (c) 2015, Zoltan Farkas All Rights Reserved.
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.CheckReturnValue;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.Registry;

/**
 *
 * Lifo scheduled java thread pool, based on talk: http://applicative.acm.org/speaker-BenMaurer.html Also it behaved
 * differently compared with a java Thread pool in that it prefers to add threads instead of queueing tasks.
 *
 * @author zoly
 */
public final class LifoThreadPoolExecutorSQP extends AbstractExecutorService {

    private final Queue<Runnable> taskQueue;

    private final Deque<QueuedThread> threadQueue;

    private final int maxIdleTimeMillis;

    private final int maxThreadCount;

    private final ExecState state;

    private final ReentrantLock submitMonitor;

    private final int queueCapacity;

    private final String poolName;

    /**
     * todo: need to interrupt running things...
     *
     * @return
     */
    @Override
    public List<Runnable> shutdownNow() {
        shutdown();
        return new ArrayList<>(taskQueue);
    }

    @Override
    @JmxExport
    public boolean isShutdown() {
        return state.isShutdown();
    }

    @Override
    @JmxExport
    public boolean isTerminated() {
        return state.isShutdown() && state.getThreadCount().get() == 0;
    }

    @JmxExport
    public int getThreadCount() {
        return state.getThreadCount().get();
    }

    @JmxExport
    public int getMaxThreadCount() {
        return maxThreadCount;
    }

    @JmxExport
    @SuppressFBWarnings("MDM_WAIT_WITHOUT_TIMEOUT")
    public int getNrQueuedTasks() {
        submitMonitor.lock();
        try {
            return taskQueue.size();
        } finally {
            submitMonitor.unlock();
        }
    }

    @JmxExport
    public int getQueueCapacity() {
        return queueCapacity;
    }

    public LifoThreadPoolExecutorSQP(final String poolName, final int coreSize,
            final int maxSize, final int maxIdleTimeMillis,
            final int queueSize) {
        this(poolName, coreSize, maxSize, maxIdleTimeMillis, queueSize, 1024);
    }

    private static final int LL_THRESHOLD = Integer.getInteger("lifoTp.llQueueSizeThreshold", 64000);

    public LifoThreadPoolExecutorSQP(final String poolName, final int coreSize,
            final int maxSize, final int maxIdleTimeMillis,
            final int queueSize, final int spinLockCount) {
        this.poolName = poolName;
        this.maxIdleTimeMillis = maxIdleTimeMillis;
        if (queueSize > LL_THRESHOLD) {
            taskQueue = new LinkedList<>();
        } else {
            this.taskQueue = new ArrayDeque<>(queueSize);
        }
        this.queueCapacity = queueSize;
        this.threadQueue = new ArrayDeque<>(maxSize);
        state = new ExecState(coreSize, spinLockCount);
        this.submitMonitor = new ReentrantLock(false);
        for (int i = 0; i < coreSize; i++) {
            QueuedThread qt = new QueuedThread(poolName, threadQueue,
                    taskQueue, maxIdleTimeMillis, null, state, submitMonitor);
            qt.start();
        }
        maxThreadCount = maxSize;
    }

    public void exportJmx() {
        Registry.export(LifoThreadPoolExecutorSQP.class.getName(), poolName, this);
    }

    @Override
    @SuppressFBWarnings({"MDM_WAIT_WITHOUT_TIMEOUT", "MDM_LOCK_ISLOCKED", "UL_UNRELEASED_LOCK_EXCEPTION_PATH" })
    public void execute(final Runnable command) {
        if (state.isShutdown()) {
            throw new UnsupportedOperationException("Executor is shutting down, rejecting" + command);
        }
        submitMonitor.lock();
        try {
            do {
                QueuedThread nqt = threadQueue.pollLast();
                if (nqt != null) {
                    submitMonitor.unlock();
                    if (nqt.runNext(command)) {
                        return;
                    } else {
                        submitMonitor.lock();
                    }
                } else {
                    break;
                }
            } while (true);
            AtomicInteger threadCount = state.getThreadCount();
            int tc;
            //CHECKSTYLE:OFF
            while ((tc = threadCount.get()) < maxThreadCount) {
                //CHECKSTYLE:ON
                if (threadCount.compareAndSet(tc, tc + 1)) {
                    submitMonitor.unlock();
                    QueuedThread qt = new QueuedThread(poolName, threadQueue, taskQueue, maxIdleTimeMillis, command,
                            state, submitMonitor);
                    qt.start();
                    return;
                }
            }
            if (taskQueue.size() >= queueCapacity) {
                throw new RejectedExecutionException();
            } else {
                if (!taskQueue.offer(command)) {
                    throw new RejectedExecutionException();
                }
            }
            submitMonitor.unlock();
        } catch (Throwable t) {
            if (submitMonitor.isHeldByCurrentThread()) {
                submitMonitor.unlock();
            }
            throw t;
        }

    }

    @Override
    @SuppressFBWarnings("MDM_WAIT_WITHOUT_TIMEOUT")
    public void shutdown() {
        submitMonitor.lock();
        try {
            if (!state.isShutdown()) {
                state.setShutdown(true);
                QueuedThread th;
                while ((th = threadQueue.poll()) != null) {
                    th.signal();
                }
            }
        } finally {
            submitMonitor.unlock();
        }
        Registry.unregister(LifoThreadPoolExecutorSQP.class.getName(), poolName);
    }

    @Override
    public boolean awaitTermination(final long millis, final TimeUnit unit) throws InterruptedException {
        long deadlinenanos = System.nanoTime() + TimeUnit.NANOSECONDS.convert(millis, unit);
        AtomicInteger threadCount = state.getThreadCount();
        synchronized (state) {
            while (threadCount.get() > 0) {
                final long timeoutNs = deadlinenanos - System.nanoTime();
                final long timeoutMs = TimeUnit.MILLISECONDS.convert(timeoutNs, TimeUnit.NANOSECONDS);
                if (timeoutMs > 0) {
                    state.wait(timeoutMs);
                } else {
                    break;
                }
            }
        }
        return threadCount.get() == 0;
    }

    private static final Runnable VOID = new Runnable() {

        @Override
        public void run() {
        }

    };

    @SuppressFBWarnings("NO_NOTIFY_NOT_NOTIFYALL")
    private static class QueuedThread extends Thread {

        private static final AtomicInteger COUNT = new AtomicInteger();

        private final Deque<QueuedThread> threadQueue;

        private final Queue<Runnable> taskQueue;

        private final int maxIdleTimeMillis;

        private Runnable runFirst;

        private final UnitQueuePU<Runnable> toRun;

        private final ExecState state;

        private volatile boolean running;

        private long lastRunNanos;

        private final Object sync;

        private final ReentrantLock submitMonitor;


        public QueuedThread(final String nameBase, final Deque<QueuedThread> threadQueue,
                final Queue<Runnable> taskQueue, final int maxIdleTimeMillis,
                final Runnable runFirst, final ExecState state, final ReentrantLock submitMonitor) {
            super(nameBase + COUNT.getAndIncrement());
            this.threadQueue = threadQueue;
            this.taskQueue = taskQueue;
            this.maxIdleTimeMillis = maxIdleTimeMillis;
            this.runFirst = runFirst;
            this.state = state;
            this.running = false;
            this.sync = new Object();
            this.lastRunNanos = System.nanoTime();
            this.submitMonitor = submitMonitor;
            this.toRun = new UnitQueuePU<>(this);
        }


        /**
         * will return false when this thread is not running anymore...
         * @param runnable
         * @return
         */
        @CheckReturnValue
        public boolean runNext(final Runnable runnable) {
            synchronized (sync) {
                if (!running) {
                    return false;
                } else {
                    return toRun.offer(runnable);
                }
            }
        }

        @SuppressFBWarnings
        public void signal() {
            toRun.offer(VOID);
        }

        public boolean isRunning() {
            return running;
        }

        @Override
        public void run() {
            boolean shouldRun = true;
            do {
                try {
                    doRun();
                } catch (RuntimeException e) {
                    try {
                        this.getUncaughtExceptionHandler().uncaughtException(this, e);
                    } catch (RuntimeException ex) {
                        ex.addSuppressed(e);
                        ex.printStackTrace();
                    }
                } catch (Error e) {
                    org.spf4j.base.Runtime.goDownWithError(e, 666);
                }

                final AtomicInteger tc = state.getThreadCount();
                int count = tc.decrementAndGet();
                while (!state.isShutdown()) {
                    if (count >= state.getCoreThreads()) {
                        shouldRun = false;
                        break;
                    } else if (tc.compareAndSet(count, count + 1)) {
                        break;
                    } else {
                        count = tc.get();
                    }
                }
            } while (shouldRun && !state.isShutdown());

            synchronized (state) {
                state.notifyAll();
            }
        }

        @SuppressFBWarnings({"MDM_WAIT_WITHOUT_TIMEOUT", "MDM_LOCK_ISLOCKED", "UL_UNRELEASED_LOCK_EXCEPTION_PATH" })
        public void doRun() {
            running = true;
            long maxIdleNanos = TimeUnit.NANOSECONDS.convert(maxIdleTimeMillis, TimeUnit.MILLISECONDS);
            try {
                if (runFirst != null) {
                    try {
                        run(runFirst);
                    } finally {
                        runFirst = null;
                    }
                }
                while (running) {
                    submitMonitor.lock();
                    Runnable poll = taskQueue.poll();
                    if (poll == null) {
                        if (state.isShutdown()) {
                            submitMonitor.unlock();
                            running = false;
                            break;
                        }
                        threadQueue.addLast(this);
                        submitMonitor.unlock();
                        while (true) {
                            Runnable runnable;
                            try {
                                runnable = toRun.poll(maxIdleNanos - (System.nanoTime() - lastRunNanos),
                                        state.spinlockCount);
                            } catch (InterruptedException ex) {
                                interrupt();
                                running = false;
                                break;
                            }
                            if (runnable != null) {
                                run(runnable);
                                break;
                            } else {
                                if ((System.nanoTime() - lastRunNanos) > maxIdleNanos) {
                                    running = false;
                                    break;
                                }
                            }
                        }
                    } else {
                        submitMonitor.unlock();
                        run(poll);
                    }
                }

            } catch (Throwable t) {
                running = false;
                if (submitMonitor.isHeldByCurrentThread()) {
                    submitMonitor.unlock();
                }
                throw t;
            } finally {
                if (!interrupted()) {
                    synchronized (sync) {
                        Runnable runnable = toRun.poll();
                        if (runnable != null) {
                            run(runnable);
                        }
                    }
                }
            }

        }

        public void run(final Runnable runnable) {
            try {
                runnable.run();
            }  finally {
                lastRunNanos = System.nanoTime();
            }
        }

        @Override
        public String toString() {
            return "QueuedThread{running=" + running + ", lastRunNanos="
                    + lastRunNanos + ", stack =" + Arrays.toString(this.getStackTrace())
                    + ", toRun = " + toRun + '}';
        }

    }

    private static final class ExecState {

        private boolean shutdown;

        private final AtomicInteger threadCount;

        private final int spinlockCount;

        private final int coreThreads;

        public ExecState(final int thnr, final int spinlockCount) {
            this.shutdown = false;
            this.coreThreads = thnr;
            this.threadCount = new AtomicInteger(thnr);
            this.spinlockCount = spinlockCount;
        }

        public int getCoreThreads() {
            return coreThreads;
        }

        public int getSpinlockCount() {
            return spinlockCount;
        }

        public boolean isShutdown() {
            return shutdown;
        }

        public void setShutdown(final boolean shutdown) {
            this.shutdown = shutdown;
        }

        public AtomicInteger getThreadCount() {
            return threadCount;
        }

        @Override
        public String toString() {
            return "ExecState{" + "shutdown=" + shutdown + ", threadCount="
                    + threadCount + ", spinlockCount=" + spinlockCount + '}';
        }



    }

    @Override
    public String toString() {
        return "LifoThreadPoolExecutorSQP{" + "threadQueue=" + threadQueue + ", maxIdleTimeMillis="
                + maxIdleTimeMillis + ", maxThreadCount=" + maxThreadCount + ", state=" + state
                + ", submitMonitor=" + submitMonitor + ", queueCapacity=" + queueCapacity
                + ", poolName=" + poolName + '}';
    }




}
