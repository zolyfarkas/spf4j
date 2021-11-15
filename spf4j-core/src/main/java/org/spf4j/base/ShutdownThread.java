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
import java.util.concurrent.TimeUnit;
import org.spf4j.concurrent.DefaultExecutor;
import org.spf4j.io.ByteArrayBuilder;

/**
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("WOC_WRITE_ONLY_COLLECTION_FIELD")
public final class ShutdownThread extends Thread {


  public static final long WAIT_FOR_SHUTDOWN_NANOS = TimeUnit.MILLISECONDS.toNanos(
          Integer.getInteger("spf4j.waitForShutdownMillis", 30000));


  private static final ShutdownThread SHUTDOWN_THREAD = new ShutdownThread(
    Boolean.getBoolean("spf4j.dumpNonDaemonThreadInfoOnShutdown"));


  private static final List<Class<?>> PRELOADED = new ArrayList<>(2);

  static {
    try (PrintStream stream = new PrintStream(new ByteArrayBuilder(), false, "UTF-8")) {
      RuntimeException rex = new RuntimeException("priming");
      Throwables.writeTo(rex, stream, Throwables.PackageDetail.NONE);
      Throwables.containsNonRecoverable(rex);
    } catch (UnsupportedEncodingException ex) {
      throw new ExceptionInInitializerError(ex);
    }
    if (SHUTDOWN_THREAD.isDumpNonDaemonThreadInfoOnShutdown()) { // prime class...
      PRELOADED.add(Threads.class);
    }
    java.lang.Runtime.getRuntime().addShutdownHook(SHUTDOWN_THREAD);
  }

  private final SortedMap<Integer, Set<Runnable>> rhooks;

  private final boolean dumpNonDaemonThreadInfoOnShutdown;

  private ShutdownThread(final boolean dumpNonDaemonThreadInfoOnShutdown) {
    super("spf4j queued shutdown");
    this.rhooks = new TreeMap<>();
    this.dumpNonDaemonThreadInfoOnShutdown = dumpNonDaemonThreadInfoOnShutdown;
  }

  public boolean isDumpNonDaemonThreadInfoOnShutdown() {
    return dumpNonDaemonThreadInfoOnShutdown;
  }

  @SuppressFBWarnings("MS_EXPOSE_REP")
  public static ShutdownThread getInstance() {
    return SHUTDOWN_THREAD;
  }

  @Override
  public void run() {
    try {
      doRun();
    } catch (Exception e) {
      ErrLog.error("Failure during sutdown", e);
    }
  }

  public void doRun() throws Exception {
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
      if (values.size() <= 1) {
        for (Runnable runnable : values) {
          try {
            runnable.run();
          } catch (RuntimeException ex) {
            if (rex == null) {
              rex = ex;
            } else {
              rex.addSuppressed(ex);
            }
          }
        }
      } else if (((int) runnables.getKey()) >= Integer.MAX_VALUE) {
        Thread[] threads = new Thread[values.size()];
        int i = 0;
        for (Runnable runnable : values) {
          Thread thread = new Thread(runnable);
          thread.start();
          threads[i++] = thread;
        }
        long deadline = TimeSource.nanoTime() + WAIT_FOR_SHUTDOWN_NANOS;
        for (Thread thread : threads) {
          try {
            thread.join(TimeUnit.NANOSECONDS.toMillis(deadline - TimeSource.nanoTime()));
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            if (rex == null) {
              rex = ex;
            } else {
              rex.addSuppressed(ex);
            }
            break;
          }
        }
      } else {
        List<Future<?>> futures = new ArrayList<>(values.size());
        for (Runnable runnable : values) {
          futures.add(DefaultExecutor.INSTANCE.submit(runnable));
        }
        for (Future<?> future : futures) {
          try {
            future.get();
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            if (rex == null) {
              rex = ex;
            } else {
              rex.addSuppressed(ex);
            }
            break;
          } catch (ExecutionException | RuntimeException ex) {
            if (rex == null) {
              rex = ex;
            } else {
              rex.addSuppressed(ex);
            }
          }
        }
      }
    }
    // print out info on all remaining non daemon threads.
    if (dumpNonDaemonThreadInfoOnShutdown) {
      Thread[] threads = Threads.getThreads();
      Thread current = Thread.currentThread();
      boolean first = true;
      for (Thread thread : threads) {
        if (thread.isAlive() && !thread.isDaemon() && !thread.equals(current)
                && !thread.getName().contains("DestroyJavaVM")) {
          if (first) {
            ErrLog.error("Non daemon threads still running:");
            first = false;
          }
          ErrLog.error("Non daemon thread " + thread + ", stackTrace = "
                  + java.util.Arrays.toString(thread.getStackTrace()));
        }
      }
    }
    if (rex != null) {
      throw rex;
    }
  }

 public void queueHookAtBeginning(final Runnable runnable) {
      queueHook(Integer.MIN_VALUE, runnable);
  }

  public void queueHookAtEnd(final Runnable runnable) {
    queueHook(Integer.MAX_VALUE, runnable);
  }

  public void queueHook(final int priority, final Runnable runnable) {
    synchronized (this.rhooks) {
      Integer pr = priority;
      Set<Runnable> runnables = this.rhooks.get(pr);
      if (runnables == null) {
        runnables = new HashSet<>(4);
        this.rhooks.put(pr, runnables);
      }
      runnables.add(runnable);
    }
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
