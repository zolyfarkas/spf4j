package org.spf4j.concurrent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.CheckReturnValue;

/**
 *
 * Lifo scheduled java thread pool, based on talk: http://applicative.acm.org/speaker-BenMaurer.html Also it behaved
 * differently compared with a java Thread pool in that it prefers to add threads instead of queueing tasks.
 *
 * @author zoly
 */
public final class LifoThreadPoolExecutorSQP extends AbstractExecutorService {

    private final BlockingQueue<Runnable> taskQueue;

    private final BlockingDeque<QueuedThread> threadQueue;

    private final int maxIdleTimeMillis;

    private final int maxThreadCount;

    private final ExecState state;

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
    public boolean isShutdown() {
        return state.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return state.getThreadCount().get() == 0;
    }

    private static class ExecState {

        private volatile boolean shutdown;

        private final AtomicInteger threadCount;

        private final int spinlockCount;

        public ExecState(final int thnr, final int spinlockCount) {
            this.shutdown = false;
            this.threadCount = new AtomicInteger(thnr);
            this.spinlockCount = spinlockCount;
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

    }

    public LifoThreadPoolExecutorSQP(final int coreSize, final int maxSize, final int maxIdleTimeMillis,
            final BlockingQueue taskQueue) {
        this(coreSize, maxSize, maxIdleTimeMillis, taskQueue, 1024);
    }

    public LifoThreadPoolExecutorSQP(final int coreSize, final int maxSize, final int maxIdleTimeMillis,
            final BlockingQueue taskQueue, final int spinLockCount) {
        this.maxIdleTimeMillis = maxIdleTimeMillis;
        this.taskQueue = taskQueue;
        this.threadQueue = new LinkedBlockingDeque<>(maxSize);
        state = new ExecState(coreSize, spinLockCount);
        for (int i = 0; i < coreSize; i++) {
            QueuedThread qt = new QueuedThread(threadQueue, taskQueue, Integer.MAX_VALUE, null, state);
            qt.start();
        }
        maxThreadCount = maxSize;
    }

    @Override
    public void execute(final Runnable command) {
        if (state.isShutdown()) {
            throw new UnsupportedOperationException("Executor is shutting down, rejecting" + command);
        }
        do {
            QueuedThread nqt = threadQueue.pollLast();
            if (nqt != null) {
                if (nqt.runNext(command)) {
                    return;
                }
            } else {
                break;
            }
        } while (true);
        newThreadOrQueue(command);

    }

    public void newThreadOrQueue(final Runnable command) {
        AtomicInteger threadCount = state.getThreadCount();
        int tc;
        //CHECKSTYLE:OFF
        while ((tc = threadCount.get()) < maxThreadCount) {
            //CHECKSTYLE:ON
            if (threadCount.compareAndSet(tc, tc + 1)) {
                QueuedThread qt = new QueuedThread(threadQueue, taskQueue, maxIdleTimeMillis, command, state);
                qt.start();
                return;
            }
        }
        if (!taskQueue.offer(command)) {
            throw new RejectedExecutionException();
        }

    }

    public void shutdown() {
        state.setShutdown(true);
        QueuedThread th;
        while ((th = threadQueue.poll()) != null) {
            th.signal();
        }
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

        private final BlockingDeque<QueuedThread> threadQueue;

        private final BlockingQueue<Runnable> taskQueue;

        private final int maxIdleTimeMillis;

        private final Runnable runFirst;

        private final UnitQueuePU<Runnable> toRun;

        private final ExecState state;

        private volatile boolean running;

        private long lastRunNanos;

        private final Object sync;

        public QueuedThread(final BlockingDeque<QueuedThread> threadQueue,
                final BlockingQueue<Runnable> taskQueue, final int maxIdleTimeMillis,
                final Runnable runFirst, final ExecState state) {
            super("DeQueued-Thread" + COUNT.getAndIncrement());
            this.threadQueue = threadQueue;
            this.taskQueue = taskQueue;
            this.maxIdleTimeMillis = maxIdleTimeMillis;
            this.runFirst = runFirst;
            this.state = state;
            this.running = false;
            this.sync = new Object();
            this.lastRunNanos = System.nanoTime();
            this.toRun = new UnitQueuePU<>(this);
        }

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
            long maxIdleNanos = TimeUnit.NANOSECONDS.convert(maxIdleTimeMillis, TimeUnit.MILLISECONDS);
            running = true;
            boolean onQueue = false;
            try {
                if (runFirst != null) {
                    run(runFirst);
                }
                Runnable poll = null;
                //CHECKSTYLE:OFF
                while (!state.isShutdown() || (poll = taskQueue.poll()) != null) {
                    //CHECKSTYLE:ON
                    if (poll == null) {
                        if (!onQueue) {
                            threadQueue.addLast(this);
                            onQueue = true;
                        }
                        Runnable runnable;
                        try {
                            runnable = toRun.poll(maxIdleNanos - (System.nanoTime() - lastRunNanos),
                                    state.spinlockCount);
                        } catch (InterruptedException ex) {
                            break;
                        }
                        if (runnable != null) {
                            try {
                                run(runnable);
                            } finally {
                                onQueue = false;
                            }
                        } else {
                            if (System.nanoTime() - lastRunNanos >= maxIdleNanos) {
                                break;
                            }
                        }
                    } else {
                        run(poll);
                    }
                }

            } finally {
                synchronized (sync) {
                    running = false;
                }
                Runnable runnable = toRun.poll();
                if (runnable != null) {
                    run(runnable);
                }
                state.getThreadCount().decrementAndGet();
                synchronized (state) {
                    state.notifyAll();
                }
            }

        }

        public void run(final Runnable runnable) {
            try {
                runnable.run();
            } catch (RuntimeException e) {
                try {
                    this.getUncaughtExceptionHandler().uncaughtException(this, e);
                } catch (RuntimeException ex) {
                    ex.addSuppressed(e);
                    ex.printStackTrace();
                }
            } catch (Error e) {
                org.spf4j.base.Runtime.goDownWithError(e, 666);
            } finally {
                lastRunNanos = System.nanoTime();
            }
        }

    }

}
