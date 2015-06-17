package org.spf4j.concurrent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
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
//CHECKSTYLE:OFF
import sun.misc.Unsafe;
//CHECKSTYLE:ON

/**
 *
 * Lifo scheduled java thread pool, based on talk: http://applicative.acm.org/speaker-BenMaurer.html Also it behaved
 * differently compared with a java Thread pool in that it prefers to add threads instead of queueing tasks.
 *
 * @author zoly
 */
public final class LifoThreadPoolExecutorUS extends AbstractExecutorService {

    private final BlockingQueue<Runnable> taskQueue;

    private final BlockingDeque<QueuedThread> threadQueue;

    private final int maxIdleTimeMillis;

    private final int maxThreadCount;

    private final ExecState state;

    private static final Unsafe USF;

    static {

        USF = AccessController.doPrivileged(new PrivilegedAction<Unsafe>() {
            @Override
            public Unsafe run() {
                try {
                    Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                    theUnsafe.setAccessible(true);
                    return (Unsafe) theUnsafe.get(null);
                } catch (IllegalArgumentException | IllegalAccessException
                        | NoSuchFieldException | SecurityException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

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

        public ExecState(final int thnr) {
            this.shutdown = false;
            this.threadCount = new AtomicInteger(thnr);
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

    public LifoThreadPoolExecutorUS(final int coreSize, final int maxSize, final int maxIdleTimeMillis,
            final BlockingQueue taskQueue) {
        this.maxIdleTimeMillis = maxIdleTimeMillis;
        this.taskQueue = taskQueue;
        this.threadQueue = new LinkedBlockingDeque<>(maxSize);
        state = new ExecState(coreSize);
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
        QueuedThread nqt = threadQueue.pollLast();
        if (nqt != null && nqt.isRunning()) {
            if (!nqt.runNext(command)) {
                newThreadOrQueue(command);
            }
        } else {
            newThreadOrQueue(command);
        }
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
            throw new RejectedExecutionException("cannot queue runnable " + command);
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


    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    private static class QueuedThread extends Thread {

        private static final AtomicInteger COUNT = new AtomicInteger();

        private final BlockingDeque<QueuedThread> threadQueue;

        private final BlockingQueue<Runnable> taskQueue;

        private final int maxIdleTimeMillis;

        private final Runnable runFirst;

        private Runnable toRun;

        private final ExecState state;

        private volatile boolean running;

        private long lastRunNanos;

        public QueuedThread(final BlockingDeque<QueuedThread> threadQueue,
                final BlockingQueue<Runnable> taskQueue, final int maxIdleTimeMillis,
                final Runnable runFirst, final ExecState state) {
            super("DeQueued-Thread" + COUNT.getAndIncrement());
            this.threadQueue = threadQueue;
            this.taskQueue = taskQueue;
            this.maxIdleTimeMillis = maxIdleTimeMillis;
            this.runFirst = runFirst;
            this.toRun = null;
            this.state = state;
            this.running = false;
            this.lastRunNanos =  System.nanoTime();
        }

        @CheckReturnValue
        public boolean runNext(final Runnable runnable) {
            if (toRun != null) {
                USF.unpark(this);
                return false;
            } else {
                toRun = runnable;
                USF.unpark(this);
                return true;
            }
        }

        public void signal() {
            USF.unpark(this);
        }

        public boolean isRunning() {
            return running;
        }

        @Override
        public void run() {
            long maxIdleNanos = TimeUnit.NANOSECONDS.convert(maxIdleTimeMillis, TimeUnit.MILLISECONDS);
            lastRunNanos = System.nanoTime();
            running = true;
            try {
                if (runFirst != null) {
                    run(runFirst);
                }
                Runnable poll = null;
                //CHECKSTYLE:OFF
                while (!state.isShutdown() || (poll = taskQueue.poll()) != null) {
                    //CHECKSTYLE:ON
                    if (poll == null) {
                        threadQueue.addLast(this);
                        long nsSinceLastRun;
                        //CHECKSTYLE:OFF
                        while (!state.isShutdown()
                                && (nsSinceLastRun = System.nanoTime() - lastRunNanos) < maxIdleNanos) {
                            //CHECKSTYLE:ON
                            USF.park(false, maxIdleNanos - nsSinceLastRun);
                            if (toRun != null) {
                                try {
                                    run(toRun);
                                } finally {
                                    toRun = null;
                                    break;
                                }
                            }
                        }
                        if (System.nanoTime() - lastRunNanos >= maxIdleNanos) {
                                break;
                        }

                    } else {
                        run(poll);
                    }
                }

            } finally {
                running = false;
                state.getThreadCount().decrementAndGet();
                synchronized (state) {
                    state.notifyAll();
                }
            }

        }

        @CheckReturnValue
        public void run(final Runnable runnable) {
            try {
                runnable.run();
            } catch (RuntimeException e) {
                this.getUncaughtExceptionHandler().uncaughtException(this, e);
            } catch (Error e) {
                org.spf4j.base.Runtime.goDownWithError(e, 666);
            } finally {
                lastRunNanos = System.nanoTime();
            }
        }

    }

}
