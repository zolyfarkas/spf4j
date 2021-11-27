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
package org.spf4j.base;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.CheckReturnValue;
import org.spf4j.concurrent.CustomThreadFactory;
import org.spf4j.io.NullOutputStream;

/**
 * A shutdown thread that allows tiered shutdown.
 * hooks in each tier will be executed sequentially, in tier level ascending order.
 * hooks on the same tier will be executed in parallel.
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY")
public final class ShutdownThread extends Thread {

  public static final long WAIT_FOR_SHUTDOWN_NANOS = TimeUnit.MILLISECONDS.toNanos(
          Integer.getInteger("spf4j.waitForShutdownMillis", 30000));

  static {
    preloadClasses();
  }

  private static final ShutdownThread SHUTDOWN_THREAD = new ShutdownThread(
          Boolean.getBoolean("spf4j.dumpNonDaemonThreadInfoOnShutdown"));

  static {
    java.lang.Runtime.getRuntime().addShutdownHook(SHUTDOWN_THREAD);
  }

  /**
   * We dod this to make sure we have these classes loaded when shutdown happens.
   * THis is to help us get some disagnostic info to the process output.
   */
  private static void preloadClasses() {
    try (PrintStream stream = new PrintStream(NullOutputStream.get(), false, "UTF-8")) {
      RuntimeException rex = new RuntimeException("priming");
      Throwables.writeTo(rex, stream, Throwables.PackageDetail.NONE);
      Throwables.containsNonRecoverable(rex);
    } catch (UnsupportedEncodingException ex) {
      throw new ExceptionInInitializerError(ex);
    }
    try {
      Class.forName(Threads.class.getName());
    } catch (ClassNotFoundException ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  private final SortedMap<Integer, Set<Runnable>> rhooks;

  private final boolean dumpNonDaemonThreadInfoOnShutdown;

  private ThreadPoolExecutor shutdownExecutor;

  private volatile boolean isShutdown;

  private ShutdownThread(final boolean dumpNonDaemonThreadInfoOnShutdown) {
    super("spf4j queued shutdown");
    this.rhooks = new TreeMap<>();
    this.dumpNonDaemonThreadInfoOnShutdown = dumpNonDaemonThreadInfoOnShutdown;
    this.shutdownExecutor = null;
    this.isShutdown = false;
  }

  public boolean isDumpNonDaemonThreadInfoOnShutdown() {
    return dumpNonDaemonThreadInfoOnShutdown;
  }

  @SuppressFBWarnings("MS_EXPOSE_REP")
  public static ShutdownThread get() {
    return SHUTDOWN_THREAD;
  }

  @Override
  public void run() {
    this.isShutdown = true;
    long deadlineNanos = TimeSource.nanoTime() + WAIT_FOR_SHUTDOWN_NANOS;
    try {
      doRun(deadlineNanos);
      shutDownClean(deadlineNanos);
    } catch (InterruptedException ex) {
      shutDownClean(deadlineNanos);
    } catch (TimeoutException ex) {
      ErrLog.error("Timeout during shutdown executor cleanup", ex);
      shutdownNowExecutor();
    } catch (Exception e) {
      if (org.spf4j.base.Throwables.containsNonRecoverable(e)) {
        XRuntime.get().goDownWithError(e, SysExits.EX_SOFTWARE);
      }
      ErrLog.error("Failure during shutdown", e);
      shutDownClean(deadlineNanos);
    } finally {
      dumpInfoOnRemainingThreads();
    }
  }

  private void shutDownClean(final long deadlineNanos) {
    try {
      shutdownExecutor(deadlineNanos);
    } catch (TimeoutException ex) {
      ErrLog.error("Timeout during shutdown executor cleanup", ex);
    } catch (RuntimeException ex) {
      ErrLog.error("RuntimeException during shutdown executor cleanup", ex);
    } catch (InterruptedException ex) {
      // just terminate
    }
  }

  private ThreadPoolExecutor getOrCreateExecutor() {
    ThreadPoolExecutor tpe = this.shutdownExecutor;
    if (tpe == null) {
      tpe = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 10, TimeUnit.MILLISECONDS,
              new SynchronousQueue<Runnable>(), new CustomThreadFactory("shutdownExecutor", true));
      this.shutdownExecutor = tpe;
    }
    return tpe;
  }

  private void shutdownExecutor(final long deadlineNanos) throws TimeoutException, InterruptedException {
    ThreadPoolExecutor tpe = this.shutdownExecutor;
    if (tpe != null) {
      tpe.shutdown();
      long timeoutNanos = TimeSource.getTimeToDeadlineStrict(deadlineNanos, TimeUnit.NANOSECONDS);
      tpe.awaitTermination(timeoutNanos, TimeUnit.NANOSECONDS);
      List<Runnable> remaining = tpe.shutdownNow();
      if (remaining.size() > 0) {
        ErrLog.error("Remaining tasks: " + remaining);
      }
    }
  }

  private void shutdownNowExecutor() {
    ThreadPoolExecutor tpe = this.shutdownExecutor;
    if (tpe != null) {
      List<Runnable> remaining = tpe.shutdownNow();
      if (remaining.size() > 0) {
        ErrLog.error("Remaining tasks: " + remaining);
      }
    }
  }

  public void doRun(final long deadlineNanos) throws TimeoutException, InterruptedException, Exception {
    Exception rex = null;
    SortedMap<Integer, Set<Runnable>> hooks;
    synchronized (rhooks) {
      hooks = new TreeMap<>(rhooks);
      for (Map.Entry<Integer, Set<Runnable>> entry : hooks.entrySet()) {
        entry.setValue(new HashSet<>(entry.getValue()));
      }
    }
    for (Map.Entry<Integer, Set<Runnable>> runnables : hooks.entrySet()) {
      final Set<Runnable> values = runnables.getValue();
      List<Future<?>> futures = new ArrayList<>(values.size());
      for (Runnable runnable : values) {
        futures.add(getOrCreateExecutor().submit(runnable));
      }
      for (Future<?> future : futures) {
        try {
          long timeoutNanos = TimeSource.getTimeToDeadlineStrict(deadlineNanos, TimeUnit.NANOSECONDS);
          future.get(timeoutNanos, TimeUnit.NANOSECONDS);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          if (rex != null) {
            ex.addSuppressed(rex);
          }
          throw ex;
        } catch (ExecutionException | RuntimeException ex) {
          if (rex == null) {
            rex = ex;
          } else {
            rex.addSuppressed(ex);
          }
        } catch (TimeoutException ex) {
          if (rex != null) {
            ex.addSuppressed(rex);
          }
          throw ex;
        }
      }
    }
    if (rex != null) {
      throw rex;
    }
  }

  /**
   * java.lang.ApplicationShutdownHooks.runHooks, java.lang.Shutdown.runHooks
   */
  private static boolean isHookShutdownRunner(final Thread t) {
    for (StackTraceElement ste : t.getStackTrace()) {
      if ("runHooks".equals(ste.getMethodName()) && ste.getClassName().contains("Shutdown")) {
        return true;
      }
    }
    return false;
  }

  public void dumpInfoOnRemainingThreads() {
    // print out info on all remaining non daemon threads.
    if (dumpNonDaemonThreadInfoOnShutdown) {
      Thread[] threads = Threads.getThreads();
      Thread current = Thread.currentThread();
      boolean first = true;
      for (Thread thread : threads) {
        if (thread.isAlive() && !thread.isDaemon() && !thread.equals(current)
                && !thread.getName().contains("DestroyJavaVM")
                && !isHookShutdownRunner(thread)) {
          if (first) {
            ErrLog.error("Non daemon threads still running:");
            first = false;
          }
          ErrLog.error("Non daemon thread " + thread + ", stackTrace = "
                  + java.util.Arrays.toString(thread.getStackTrace()));
        }
      }
    }
  }

  @CheckReturnValue
  public boolean queueHookAtBeginning(final Runnable runnable) {
    return queueHook(Integer.MIN_VALUE, runnable);
  }

  @CheckReturnValue
  public boolean queueHookAtEnd(final Runnable runnable) {
    return queueHook(Integer.MAX_VALUE, runnable);
  }

  @CheckReturnValue
  public boolean queueHook(final int priority, final Runnable runnable) {
    if (this.isShutdown) {
      return false;
    }
    synchronized (this.rhooks) {
      if (this.isShutdown) {
        return false;
      }
      Integer pr = priority;
      Set<Runnable> runnables = this.rhooks.get(pr);
      if (runnables == null) {
        runnables = new HashSet<>(4);
        this.rhooks.put(pr, runnables);
      }
      runnables.add(runnable);
    }
    return true;
  }

  public boolean removeQueuedShutdownHook(final Runnable runnable) {
    if (this.equals(Thread.currentThread())) {
      return false;
    }
    synchronized (this.rhooks) {
      for (Set<Runnable> entry : this.rhooks.values()) {
        if (entry.remove(runnable)) {
          return true;
        }
      }
    }
    return false;
  }

}
