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
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread utilities.
 *
 * @author Zoltan Farkas
 */
public final class Threads {

  public static final Thread[] EMPTY_ARRAY = new Thread[0];

  private static final ThreadInfoSupplier TI_SUPP;

  static {
    ThreadInfoSupplier supp;
    try {
      supp = new OracleJdkThreadInfoSupplier();
    } catch (ExceptionInInitializerError ex) {
      Logger logger = Logger.getLogger(Threads.class.getName());
      logger.warning("Optimized stack trace access not available,"
              + " profiling overhead will be higher");
      logger.log(Level.FINE, "Exception detail", ex);
      supp = new SlowThreadInfoSupplierImpl();
    }
    TI_SUPP = supp;
  }

  interface ThreadInfoSupplier {

    Thread[] getThreads();

    StackTraceElement[][] getStackTraces(Thread... threads);
  }

  private static class OracleJdkThreadInfoSupplier implements ThreadInfoSupplier {

    private static final MethodHandle GET_THREADS;
    private static final MethodHandle DUMP_THREADS;

    static {
      final java.lang.reflect.Method getThreads;
      final java.lang.reflect.Method dumpThreads;
      try {

        getThreads = Thread.class.getDeclaredMethod("getThreads");
        dumpThreads = Thread.class.getDeclaredMethod("dumpThreads", Thread[].class);
      } catch (NoSuchMethodException ex) {
        throw new ExceptionInInitializerError(ex);
      }
      AccessController.doPrivileged((PrivilegedAction) () -> {
        getThreads.setAccessible(true);
        dumpThreads.setAccessible(true);
        return null; // nothing to return
      });
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      try {
        GET_THREADS = lookup.unreflect(getThreads);
        DUMP_THREADS = lookup.unreflect(dumpThreads);
      } catch (IllegalAccessException ex) {
        throw new ExceptionInInitializerError(ex);
      }

    }

    @Override
    @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
    public Thread[] getThreads() {
      try {
        return (Thread[]) GET_THREADS.invokeExact();
      } catch (RuntimeException | Error ex) {
        throw ex;
      } catch (Throwable ex) {
        throw new UncheckedExecutionException(ex);
      }
    }

    @Override
    @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
    public StackTraceElement[][] getStackTraces(final Thread... threads) {
      StackTraceElement[][] stackDump;
      try {
        stackDump = (StackTraceElement[][]) DUMP_THREADS.invokeExact(threads);
      } catch (RuntimeException | Error ex) {
        throw ex;
      } catch (Throwable ex) {
        throw new UncheckedExecutionException(ex);
      }
      return stackDump;
    }

  }

  private Threads() {
  }

  public static Thread[] getThreads() {
    return TI_SUPP.getThreads();
  }

  /**
   * get a random selection of nr Threads from the array, the first nr location in the array will
   * contain the random set,m the rest will be null.
   * @param nr number of threads to randomly select.
   * @param threads the array of threads to select from
   */
  @SuppressFBWarnings("PREDICTABLE_RANDOM")
  public static int randomFirst(final int nr, final Thread[] threads) {
    int length = threads.length;
    if (nr >= length) {
      return length;
    }
    ThreadLocalRandom rnd = ThreadLocalRandom.current();
    for (int i = 0; i < nr; i++) {
      int nextInt = rnd.nextInt(i, length);
      if (nextInt != i) {
        Thread t = threads[i];
        threads[i] = threads[nextInt];
        threads[nextInt] = t;
      }
    }
    Arrays.fill(threads, nr, length, null);
    return nr;
  }

  public static StackTraceElement[][] getStackTraces(final Thread... threads) {
    return TI_SUPP.getStackTraces(threads);
  }

  public static void dumpToPrintStream(final PrintStream stream) {
    StringBuilder sb = new StringBuilder(1024);
    Thread[] threads = getThreads();
    StackTraceElement[][] stackTraces = getStackTraces(threads);
    for (int i = 0; i < threads.length; i++) {
      StackTraceElement[] stackTrace = stackTraces[i];
      if (stackTrace != null && stackTrace.length > 0) {
        Thread thread = threads[i];
        sb.append("Thread ").append(thread.getName()).append('\n');
        try {
          Throwables.writeTo(stackTrace, sb, Throwables.PackageDetail.SHORT, true);
        } catch (IOException ex) {
          throw new UncheckedIOException(ex);
        }
      }
    }
    stream.append(sb);
  }

  private static class SlowThreadInfoSupplierImpl implements ThreadInfoSupplier {

    @Override
    public Thread[] getThreads() {
      Set<Thread> keySet = Thread.getAllStackTraces().keySet();
      return keySet.toArray(new Thread[keySet.size()]);
    }

    @Override
    public StackTraceElement[][] getStackTraces(final Thread... threads) {
      Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
      StackTraceElement[][] result = new StackTraceElement[threads.length][];
      for (int i = 0; i < threads.length; i++) {
        result[i] = allStackTraces.get(threads[i]);
      }
      return result;
    }
  }

}
