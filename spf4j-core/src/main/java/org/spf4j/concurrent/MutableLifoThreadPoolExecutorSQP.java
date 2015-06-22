
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
import java.util.HashSet;
import java.util.Queue;
import java.util.List;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.CheckReturnValue;
import org.spf4j.ds.ZArrayDequeue;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.Registry;

/**
 *
 * Lifo scheduled java thread pool, based on talk: http://applicative.acm.org/speaker-BenMaurer.html Also it behaved
 * differently compared with a java Thread pool in that it prefers to add threads instead of queueing tasks.
 *
 * This pool allows changing most parameters on the fly. This comes at the cost of using some volatile vars.
 *
 * There are 3 data structures involved in the transfer of tasks to Threads.
 *
 * 1) Task Queue - a classic FIFO queue. RW controlled by a reentrant lock.
 *    only non-blockng read operations are done on this queue
 * 2) Available Thread Queue - a classic LIFO queue, a thread end up here when there is nothing to process.
 * 3) A "UnitQueue", is a queue with a capacity on 1, which a thread will listen on while in the available thread queue.
 *
 * @author zoly
 */
public final class MutableLifoThreadPoolExecutorSQP extends AbstractExecutorService {

    private final Queue<Runnable> taskQueue;

    private final ZArrayDequeue<QueuedThread> threadQueue;

    private volatile int maxThreadCount;

    private final PoolState state;

    private final ReentrantLock stateLock;

    private volatile int queueSizeLimit;

    private final String poolName;

    private final RejectedExecutionHandler rejectionHandler;


    public MutableLifoThreadPoolExecutorSQP(final String poolName, final int coreSize,
            final int maxSize, final int maxIdleTimeMillis,
            final int queueSize) {
        this(poolName, coreSize, maxSize, maxIdleTimeMillis, queueSize, 1024);
    }

    private static final int LL_THRESHOLD = Integer.getInteger("lifoTp.llQueueSizeThreshold", 64000);

    public MutableLifoThreadPoolExecutorSQP(final String poolName, final int coreSize,
            final int maxSize, final int maxIdleTimeMillis,
            final int queueSizeLimit, final int spinLockCount) {
        this(poolName, coreSize, maxSize, maxIdleTimeMillis,
                new ArrayDeque<Runnable>(Math.min(queueSizeLimit, LL_THRESHOLD)),
                queueSizeLimit, spinLockCount, REJECT_EXCEPTION_EXEC_HANDLER);
    }

    public MutableLifoThreadPoolExecutorSQP(final String poolName, final int coreSize,
            final int maxSize, final int maxIdleTimeMillis, final Queue<Runnable> taskQueue,
            final int queueSizeLimit, final int spinLockCount, final RejectedExecutionHandler rejectionHandler) {
        this.rejectionHandler = rejectionHandler;
        this.poolName = poolName;
        this.taskQueue = taskQueue;
        this.queueSizeLimit = queueSizeLimit;
        this.threadQueue = new ZArrayDequeue<>(maxSize);
        state = new PoolState(coreSize, spinLockCount, new HashSet<QueuedThread>(Math.min(maxSize, 2048)),
                                maxIdleTimeMillis);
        this.stateLock = new ReentrantLock(false);
        for (int i = 0; i < coreSize; i++) {
            QueuedThread qt = new QueuedThread(poolName, threadQueue,
                    taskQueue, null, state, stateLock);
            state.addThread(qt);
            qt.start();
        }
        maxThreadCount = maxSize;
    }

    public void exportJmx() {
        Registry.export(MutableLifoThreadPoolExecutorSQP.class.getName(), poolName, this);
    }

    @Override
    @SuppressFBWarnings(value = {"MDM_WAIT_WITHOUT_TIMEOUT", "MDM_LOCK_ISLOCKED", "UL_UNRELEASED_LOCK_EXCEPTION_PATH" },
            justification = "no blocking is done while holding the lock,"
                    + " lock is released on all paths, findbugs just cannot figure it out...")
    public void execute(final Runnable command) {
        if (state.isShutdown()) {
            throw new UnsupportedOperationException("Executor is shutting down, rejecting" + command);
        }
        stateLock.lock();
        try {
            do {
                QueuedThread nqt = threadQueue.pollLast();
                if (nqt != null) {
                    stateLock.unlock();
                    if (nqt.runNext(command)) {
                        return;
                    } else {
                        stateLock.lock();
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
                    stateLock.unlock();
                    QueuedThread qt = new QueuedThread(poolName, threadQueue, taskQueue, command,
                            state, stateLock);
                    state.addThread(qt);
                    qt.start();
                    return;
                }
            }
            if (taskQueue.size() >= queueSizeLimit) {
                rejectionHandler.rejectedExecution(command, this);
            } else {
                if (!taskQueue.offer(command)) {
                    rejectionHandler.rejectedExecution(command, this);
                }
            }
            stateLock.unlock();
        } catch (Throwable t) {
            if (stateLock.isHeldByCurrentThread()) {
                stateLock.unlock();
            }
            throw t;
        }

    }

    @Override
    @SuppressFBWarnings("MDM_WAIT_WITHOUT_TIMEOUT")
    @JmxExport
    public void shutdown() {
        stateLock.lock();
        try {
            if (!state.isShutdown()) {
                state.setShutdown(true);
                QueuedThread th;
                while ((th = threadQueue.pollLast()) != null) {
                    th.signal();
                }
            }
        } finally {
            stateLock.unlock();
        }
    }

    @JmxExport
    @SuppressFBWarnings("MDM_WAIT_WITHOUT_TIMEOUT")
    public void start() {
        stateLock.lock();
        try {
            state.setShutdown(false);
        } finally {
            stateLock.unlock();
        }
    }

    public void unregisterJmx() {
        Registry.unregister(MutableLifoThreadPoolExecutorSQP.class.getName(), poolName);
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

    @Override
    @JmxExport
    public List<Runnable> shutdownNow() {
        shutdown();
        state.interruptAll();
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
    public void setMaxThreadCount(final int maxThreadCount) {
        this.maxThreadCount = maxThreadCount;
    }

    @JmxExport
    @SuppressFBWarnings(value = "MDM_WAIT_WITHOUT_TIMEOUT",
            justification = "Holders of this lock will not block")
    public int getNrQueuedTasks() {
        stateLock.lock();
        try {
            return taskQueue.size();
        } finally {
            stateLock.unlock();
        }
    }

    @JmxExport
    public int getQueueSizeLimit() {
        return queueSizeLimit;
    }

    @JmxExport
    public void setQueueSizeLimit(final int queueSizeLimit) {
        this.queueSizeLimit = queueSizeLimit;
    }



    private static final Runnable VOID = new Runnable() {

        @Override
        public void run() {
        }

    };

    @SuppressFBWarnings("NO_NOTIFY_NOT_NOTIFYALL")
    private static class QueuedThread extends Thread {

        private static final AtomicInteger COUNT = new AtomicInteger();

        private final ZArrayDequeue<QueuedThread> threadQueue;

        private final Queue<Runnable> taskQueue;

        private Runnable runFirst;

        private final UnitQueuePU<Runnable> toRun;

        private final PoolState state;

        private volatile boolean running;

        private long lastRunNanos;

        private final Object sync;

        private final ReentrantLock submitMonitor;

        public QueuedThread(final String nameBase, final ZArrayDequeue<QueuedThread> threadQueue,
                final Queue<Runnable> taskQueue,
                final Runnable runFirst, final PoolState state, final ReentrantLock submitMonitor) {
            super(nameBase + COUNT.getAndIncrement());
            this.threadQueue = threadQueue;
            this.taskQueue = taskQueue;
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
         *
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
                state.removeThread(this);
                state.notifyAll();
            }
        }

        @SuppressFBWarnings({"MDM_WAIT_WITHOUT_TIMEOUT", "MDM_LOCK_ISLOCKED", "UL_UNRELEASED_LOCK_EXCEPTION_PATH" })
        public void doRun() {
            running = true;
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
                        int ptr = threadQueue.addLastAndGetPtr(this);
                        submitMonitor.unlock();
                        while (true) {
                            Runnable runnable;
                            try {
                                final long wTime = state.getMaxIdleTimeNanos() - (System.nanoTime() - lastRunNanos);
                                if (wTime > 0) {
                                    runnable = toRun.poll(wTime, state.spinlockCount);
                                } else {
                                    running = false;
                                    removeThreadFromQueue(ptr);
                                    break;
                                }
                            } catch (InterruptedException ex) {
                                interrupt();
                                running = false;
                                removeThreadFromQueue(ptr);
                                break;
                            }
                            if (runnable != null) {
                                run(runnable);
                                break;
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

        @SuppressFBWarnings("MDM_WAIT_WITHOUT_TIMEOUT")
        public void removeThreadFromQueue(final int ptr) {
            submitMonitor.lock();
            try {
                threadQueue.delete(ptr, this);
            } finally {
                submitMonitor.unlock();
            }
        }

        public void run(final Runnable runnable) {
            try {
                runnable.run();
            } finally {
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

    private static final class PoolState {

        private volatile int maxIdleTimeMillis;

        private volatile long maxIdleTimeNanos;

        private boolean shutdown;

        private final AtomicInteger threadCount;

        private final int spinlockCount;

        private final int coreThreads;

        private final Set<QueuedThread> allThreads;

        public PoolState(final int thnr, final int spinlockCount,
                final Set<QueuedThread> allThreads, final int maxIdleTimeMillis) {
            this.shutdown = false;
            this.coreThreads = thnr;
            this.threadCount = new AtomicInteger(thnr);
            this.spinlockCount = spinlockCount;
            this.allThreads = allThreads;
            this.maxIdleTimeMillis = maxIdleTimeMillis;
            this.maxIdleTimeNanos = TimeUnit.NANOSECONDS.convert(maxIdleTimeMillis, TimeUnit.MILLISECONDS);
        }

        public int getMaxIdleTimeMillis() {
            return maxIdleTimeMillis;
        }

        public long getMaxIdleTimeNanos() {
            return maxIdleTimeNanos;
        }

        public void setMaxIdleTimeMillis(final int maxIdleTimeMillis) {
            this.maxIdleTimeMillis = maxIdleTimeMillis;
            this.maxIdleTimeNanos = TimeUnit.NANOSECONDS.convert(maxIdleTimeMillis, TimeUnit.MILLISECONDS);
        }



        public void addThread(final QueuedThread thread) {
            synchronized (allThreads) {
                if (!allThreads.add(thread)) {
                    throw new IllegalStateException("Attempting to add a thread twice: " + thread);
                }
            }
        }

        public void removeThread(final QueuedThread thread) {
            synchronized (allThreads) {
                if (!allThreads.remove(thread)) {
                    throw new IllegalStateException("Removing thread failed: " + thread);
                }
            }
        }

        public void interruptAll() {
            synchronized (allThreads) {
                for (Thread thread : allThreads) {
                    thread.interrupt();
                }
            }
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
        return "LifoThreadPoolExecutorSQP{" + "threadQueue=" + threadQueue
                + ", maxThreadCount=" + maxThreadCount + ", state=" + state
                + ", submitMonitor=" + stateLock + ", queueCapacity=" + queueSizeLimit
                + ", poolName=" + poolName + '}';
    }

    public Queue<Runnable> getTaskQueue() {
        return taskQueue;
    }

    @JmxExport
    public int getMaxIdleTimeMillis() {
        return state.getMaxIdleTimeMillis();
    }


    @JmxExport
    public void setMaxIdleTimeMillis(final int maxIdleTimeMillis) {
        this.state.setMaxIdleTimeMillis(maxIdleTimeMillis);
    }

    public String getPoolName() {
        return poolName;
    }

    public interface RejectedExecutionHandler {
        void rejectedExecution(Runnable r, MutableLifoThreadPoolExecutorSQP executor);
    }

    public static final RejectedExecutionHandler REJECT_EXCEPTION_EXEC_HANDLER = new RejectedExecutionHandler() {

        @Override
        public void rejectedExecution(final Runnable r, final MutableLifoThreadPoolExecutorSQP executor) {
            throw new RejectedExecutionException();
        }
    };


}
