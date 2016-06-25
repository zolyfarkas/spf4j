
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
import gnu.trove.set.hash.THashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;
import java.util.List;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.GuardedBy;
import static org.spf4j.concurrent.RejectedExecutionHandler.REJECT_EXCEPTION_EXEC_HANDLER;
import org.spf4j.ds.ZArrayDequeue;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.Registry;

/**
 *
 * Lifo scheduled java thread pool, based on talk: http://applicative.acm.org/speaker-BenMaurer.html This implementation
 * behaves differently compared with a java Thread pool in that it prefers to spawn a thread if possible instead of
 * queueing tasks.
 *
 * See LifoThreadPoolBuilder for conveniently constructing pools
 *
 * There are 3 data structures involved in the transfer of tasks to Threads.
 *
 * 1) Task Queue - a classic FIFO queue. RW controlled by a reentrant lock. only non-blockng read operations are done on
 * this queue 2) Available Thread Queue - a classic LIFO queue, a thread end up here when there is nothing to process.
 * 3) A "UnitQueue", is a queue with a capacity on 1, which a thread will listen on while in the available thread queue.
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
@SuppressFBWarnings({ "MDM_THREAD_PRIORITIES", "MDM_WAIT_WITHOUT_TIMEOUT" })
public final class LifoThreadPoolExecutorSQP extends AbstractExecutorService implements LifoThreadPool {

  /**
   * when a thread survives due core size, this the minimum wait time that core threads will wait for. worker threads
   * have a maximum time they are idle, after which they are retired... in case a user configures a thread pool with
   * idle times less than min wait, the core threads will have to have a minimum wait time to avoid spinning and
   * hogging the CPU. this value is used only when the max idle time of the pool is smaller, and it interferes with
   * thread retirement in that case... I do not see that case as a useful pooling case to be worth trying
   * to optimize it...
   */
  static final long CORE_MINWAIT_NANOS = Long.getLong("spf4j.lifoTp.coreMaxWaitNanos", 1000000000);

  @GuardedBy("stateLock")
  private final Queue<Runnable> taskQueue;

  @GuardedBy("stateLock")
  private final ZArrayDequeue<QueuedThread> threadQueue;

  private final int maxIdleTimeMillis;

  private final int maxThreadCount;

  @GuardedBy("stateLock")
  private final PoolState state;

  private final ReentrantLock stateLock;

  private final Condition stateCondition;

  private final int queueSizeLimit;

  private final String poolName;

  private final RejectedExecutionHandler rejectionHandler;

  private final boolean daemonThreads;

  private final int threadPriority;

  public LifoThreadPoolExecutorSQP(final int maxNrThreads, final String name) {
    this(name, 0, maxNrThreads, 5000, 0);
  }

  public LifoThreadPoolExecutorSQP(final String poolName, final int coreSize,
          final int maxSize, final int maxIdleTimeMillis,
          final int queueSize) {
    this(poolName, coreSize, maxSize, maxIdleTimeMillis, queueSize, 1024);
  }

  public LifoThreadPoolExecutorSQP(final String poolName, final int coreSize,
          final int maxSize, final int maxIdleTimeMillis,
          final int queueSize, final boolean daemonThreads, final int spinLockCount) {
    this(poolName, coreSize, maxSize, maxIdleTimeMillis,
            new ArrayDeque<Runnable>(Math.min(queueSize, LL_THRESHOLD)),
            queueSize, daemonThreads, spinLockCount, REJECT_EXCEPTION_EXEC_HANDLER);
  }

  public LifoThreadPoolExecutorSQP(final String poolName, final int coreSize,
          final int maxSize, final int maxIdleTimeMillis,
          final int queueSize, final boolean daemonThreads) {
    this(poolName, coreSize, maxSize, maxIdleTimeMillis,
            new ArrayDeque<Runnable>(Math.min(queueSize, LL_THRESHOLD)),
            queueSize, daemonThreads, 1024, REJECT_EXCEPTION_EXEC_HANDLER);
  }

  static final int LL_THRESHOLD = Integer.getInteger("spf4j.lifoTp.llQueueSizeThreshold", 64000);

  public LifoThreadPoolExecutorSQP(final String poolName, final int coreSize,
          final int maxSize, final int maxIdleTimeMillis,
          final int queueSizeLimit, final int spinLockCount) {
    this(poolName, coreSize, maxSize, maxIdleTimeMillis,
            new ArrayDeque<Runnable>(Math.min(queueSizeLimit, LL_THRESHOLD)),
            queueSizeLimit, false, spinLockCount, REJECT_EXCEPTION_EXEC_HANDLER);
  }

  public LifoThreadPoolExecutorSQP(final String poolName, final int coreSize,
          final int maxSize, final int maxIdleTimeMillis, final Queue<Runnable> taskQueue,
          final int queueSizeLimit, final boolean daemonThreads,
          final int spinLockCount, final RejectedExecutionHandler rejectionHandler) {
    this(poolName, coreSize, maxSize, maxIdleTimeMillis, taskQueue, queueSizeLimit, daemonThreads,
            spinLockCount, rejectionHandler, Thread.NORM_PRIORITY);
  }

  public LifoThreadPoolExecutorSQP(final String poolName, final int coreSize,
          final int maxSize, final int maxIdleTimeMillis, final Queue<Runnable> taskQueue,
          final int queueSizeLimit, final boolean daemonThreads,
          final int spinLockCount, final RejectedExecutionHandler rejectionHandler,
          final int threadPriority) {
    if (coreSize > maxSize) {
      throw new IllegalArgumentException("Core size must be smaller than max size " + coreSize
              + " < " + maxSize);
    }
    if (coreSize < 0 || maxSize < 0 || spinLockCount < 0 || maxIdleTimeMillis < 0 || queueSizeLimit < 0) {
      throw new IllegalArgumentException("All numberic TP configs must be positive values: "
              + coreSize + ", " + maxSize + ", " + maxIdleTimeMillis + ", " + spinLockCount
              + ", " + queueSizeLimit);
    }
    this.rejectionHandler = rejectionHandler;
    this.poolName = poolName;
    this.maxIdleTimeMillis = maxIdleTimeMillis;
    this.taskQueue = taskQueue;
    this.queueSizeLimit = queueSizeLimit;
    this.threadQueue = new ZArrayDequeue<>(Math.min(1024, maxSize));
    this.threadPriority = threadPriority;
    state = new PoolState(coreSize, spinLockCount, new THashSet<QueuedThread>(Math.min(maxSize, 2048)));
    this.stateLock = new ReentrantLock(false);
    this.stateCondition = stateLock.newCondition();
    this.daemonThreads = daemonThreads;
    for (int i = 0; i < coreSize; i++) {
      QueuedThread qt = new QueuedThread(poolName, threadQueue,
              taskQueue, maxIdleTimeMillis, null, state, stateLock, stateCondition);
      qt.setDaemon(daemonThreads);
      qt.setPriority(threadPriority);
      state.addThread(qt);
      qt.start();
    }
    maxThreadCount = maxSize;
  }

  @Override
  public void exportJmx() {
    Registry.export(LifoThreadPoolExecutorSQP.class.getName(), poolName, this);
  }

  @Override
  @SuppressFBWarnings(value = {"MDM_WAIT_WITHOUT_TIMEOUT", "MDM_LOCK_ISLOCKED", "UL_UNRELEASED_LOCK_EXCEPTION_PATH"},
          justification = "no blocking is done while holding the lock,"
          + " lock is released on all paths, findbugs just cannot figure it out...")
  public void execute(final Runnable command) {
    stateLock.lock();
    try {
      if (state.isShutdown()) {
        // if shutting down, reject
        stateLock.unlock();
        this.rejectionHandler.rejectedExecution(command, this);
        return;
      }
      do { // See if we have Threads available to run the task and do so if any
        QueuedThread nqt = threadQueue.pollLast();
        if (nqt != null) {
          stateLock.unlock();
          if (nqt.runNext(command)) {
            return; // job successfully submitted
          } else {
            stateLock.lock();
          }
        } else {
          break;
        }
      } while (true);
      int tc = state.getThreadCount();
      // was not able to submit to an existing available thread, will attempt to create a new thread.
      //CHECKSTYLE:OFF
      if (tc < maxThreadCount) {
        QueuedThread qt = new QueuedThread(poolName, threadQueue, taskQueue, maxIdleTimeMillis, command,
                state, stateLock, stateCondition);
        qt.setDaemon(daemonThreads);
        qt.setPriority(threadPriority);
        state.addThread(qt);
        stateLock.unlock();
        qt.start();
        return;
      }
      // was not able to submit to an existing available thread, reached the maxThread limit.
      // will attempt to queue the task, and reject if unable to
      if (taskQueue.size() >= queueSizeLimit) {
        stateLock.unlock();
        rejectionHandler.rejectedExecution(command, this);
      } else if (!taskQueue.offer(command)) {
        stateLock.unlock();
        rejectionHandler.rejectedExecution(command, this);
      } else {
        stateLock.unlock();
      }
    } catch (Throwable t) {
      if (stateLock.isHeldByCurrentThread()) {
        stateLock.unlock();
      }
      throw t;
    }

  }

  @Override
  @SuppressFBWarnings("MDM_WAIT_WITHOUT_TIMEOUT")
  public void shutdown() {
    stateLock.lock();
    try {
      if (!state.isShutdown()) {
        state.setShutdown(true); // set the shutdown flag, to reject new submissions.
        QueuedThread th;
        while ((th = threadQueue.pollLast()) != null) {
          th.signal(); // signal all waiting threads, so thay can start going down.
        }
      }
    } finally {
      stateLock.unlock();
    }
  }

  @Override
  public boolean awaitTermination(final long millis, final TimeUnit unit) throws InterruptedException {
    long deadlinenanos = System.nanoTime() + TimeUnit.NANOSECONDS.convert(millis, unit);
    int threadCount;
    stateLock.lock();
    try {
      if (!state.isShutdown()) {
        throw new IllegalStateException("Threadpool is not is shutdown mode " + this);
      }
      threadCount = state.getThreadCount();
      long timeoutNs = deadlinenanos - System.nanoTime();
      while (threadCount > 0) {
        if (timeoutNs > 0) {
          timeoutNs = stateCondition.awaitNanos(timeoutNs);
        } else {
          break;
        }
        threadCount = state.getThreadCount();
      }
    } finally {
      stateLock.unlock();
    }
    return threadCount == 0;
  }

  @Override
  public List<Runnable> shutdownNow() {
    shutdown(); // shutdown
    stateLock.lock();
    try {
      state.interruptAll(); // interrupt all running threads.
      return new ArrayList<>(taskQueue);
    } finally {
      stateLock.unlock();
    }
  }

  @Override
  @JmxExport
  public boolean isShutdown() {
    return state.isShutdown();
  }

  @JmxExport
  @Override
  public boolean isDaemonThreads() {
    return daemonThreads;
  }

  @Override
  @JmxExport
  public boolean isTerminated() {
    stateLock.lock();
    try {
      return state.isShutdown() && state.getThreadCount() == 0;
    } finally {
      stateLock.unlock();
    }
  }

  @JmxExport
  @Override
  public int getThreadCount() {
    stateLock.lock();
    try {
      return state.getThreadCount();
    } finally {
      stateLock.unlock();
    }
  }

  @JmxExport
  @Override
  public int getMaxThreadCount() {
    return maxThreadCount;
  }

  @Override
  public ReentrantLock getStateLock() {
    return stateLock;
  }

  @JmxExport
  @SuppressFBWarnings(value = "MDM_WAIT_WITHOUT_TIMEOUT",
          justification = "Holders of this lock will not block")
  @Override
  public int getNrQueuedTasks() {
    stateLock.lock();
    try {
      return taskQueue.size();
    } finally {
      stateLock.unlock();
    }
  }

  @JmxExport
  @Override
  public int getQueueSizeLimit() {
    return queueSizeLimit;
  }

  private static final Runnable VOID = new Runnable() {

    @Override
    public void run() {
    }

  };

  @Override
  public void unregisterJmx() {
    Registry.unregister(LifoThreadPoolExecutorSQP.class.getName(), poolName);
  }

  @SuppressFBWarnings("NO_NOTIFY_NOT_NOTIFYALL")
  private static final class QueuedThread extends Thread {

    private static final AtomicInteger COUNT = new AtomicInteger();

    private final ZArrayDequeue<QueuedThread> threadQueue;

    private final Queue<Runnable> taskQueue;

    private final int maxIdleTimeMillis;

    private Runnable runFirst;

    private final UnitQueuePU<Runnable> toRun;

    private final PoolState state;

    private long lastRunNanos;

    private final Object sync;

    private final ReentrantLock submitMonitor;

    private final Condition submitCondition;

    private volatile boolean running;

    QueuedThread(final String nameBase, final ZArrayDequeue<QueuedThread> threadQueue,
            final Queue<Runnable> taskQueue, final int maxIdleTimeMillis,
            @Nullable final Runnable runFirst, final PoolState state,
            final ReentrantLock submitMonitor, final Condition submitCondition) {
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
      this.submitCondition = submitCondition;
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
      final long origNanosWait = TimeUnit.NANOSECONDS.convert(maxIdleTimeMillis, TimeUnit.MILLISECONDS);
      long maxIdleNanos = origNanosWait;
      boolean shouldRun = true;
      do {
        try {
          doRun(maxIdleNanos);
        } catch (RuntimeException e) {
          final UncaughtExceptionHandler uexh = this.getUncaughtExceptionHandler();
          try {
            uexh.uncaughtException(this, e);
          } catch (RuntimeException ex) {
            ex.addSuppressed(e);
            throw new Error("Uncaught exception handler blew up: " + uexh, ex);
          }
        } catch (Error e) {
          org.spf4j.base.Runtime.goDownWithError(e, 666);
        }
        submitMonitor.lock();
        try {
          final int tc = state.getThreadCount();
          if (state.isShutdown() || tc - 1 >= state.getCoreThreads()) {
            state.removeThread(this);
            shouldRun = false;
            submitCondition.signalAll();
            break;
          } else {
            this.lastRunNanos = System.nanoTime(); // update last Run time to avoid core thread spinning.
            // there must be a minimal wait time for a core thread to avoid spinning.
            maxIdleNanos = Math.max(origNanosWait, CORE_MINWAIT_NANOS);
          }
        } finally {
          submitMonitor.unlock();
        }
      } while (shouldRun && !state.isShutdown());
    }

    @SuppressFBWarnings({"MDM_WAIT_WITHOUT_TIMEOUT", "MDM_LOCK_ISLOCKED", "UL_UNRELEASED_LOCK_EXCEPTION_PATH"})
    public void doRun(final long maxIdleNanos) {
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
                final long wTime = maxIdleNanos - (System.nanoTime() - lastRunNanos);
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
      return "QueuedThread{name = " + getName() + ", running=" + running + ", lastRunNanos="
              + lastRunNanos + ", stack =" + Arrays.toString(this.getStackTrace())
              + ", toRun = " + toRun + '}';
    }

  }

  private static final class PoolState {

    private boolean shutdown;

    private int threadCount;

    private final int spinlockCount;

    private final int coreThreads;

    private final Set<QueuedThread> allThreads;

    PoolState(final int thnr, final int spinlockCount, final Set<QueuedThread> allThreads) {
      this.shutdown = false;
      this.coreThreads = thnr;
      this.threadCount = 0;
      this.spinlockCount = spinlockCount;
      this.allThreads = allThreads;
    }

    public void addThread(final QueuedThread thread) {
      if (!allThreads.add(thread)) {
        throw new IllegalStateException("Attempting to add a thread twice: " + thread);
      }
      threadCount++;
    }

    public void removeThread(final QueuedThread thread) {
      if (!allThreads.remove(thread)) {
        throw new IllegalStateException("Removing thread failed: " + thread);
      }
      threadCount--;
    }

    public void interruptAll() {
      for (Thread thread : allThreads) {
        thread.interrupt();
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

    public int getThreadCount() {
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
            + ", submitMonitor=" + stateLock + ", queueCapacity=" + queueSizeLimit
            + ", poolName=" + poolName + '}';
  }

  @Override
  public Queue<Runnable> getTaskQueue() {
    return taskQueue;
  }

  @JmxExport
  @Override
  public int getMaxIdleTimeMillis() {
    return maxIdleTimeMillis;
  }

  @JmxExport
  @Override
  public String getPoolName() {
    return poolName;
  }

  @JmxExport
  @Override
  public int getThreadPriority() {
    return threadPriority;
  }

}
