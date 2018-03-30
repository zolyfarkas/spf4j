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

import com.google.common.util.concurrent.UncheckedExecutionException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Thread utilities.
 * @author Zoltan Farkas
 */
public final class Threads {

  public static final Thread[] EMPTY_ARRAY = new Thread[0];

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

  private Threads() { }

  public static Thread[] getThreads() {
    try {
      return (Thread[]) GET_THREADS.invokeExact();
    } catch (RuntimeException | Error ex) {
      throw ex;
    } catch (Throwable ex) {
      throw new UncheckedExecutionException(ex);
    }
  }

  public static StackTraceElement[][] getStackTraces(final Thread... threads) {
    StackTraceElement[][] stackDump;
    try {
      stackDump = (StackTraceElement[][]) DUMP_THREADS.invokeExact(threads);
    } catch (RuntimeException | Error ex) {
      throw ex;
    } catch (Throwable ex) {
      throw new RuntimeException(ex);
    }
    return stackDump;
  }

  @SuppressFBWarnings("NOS_NON_OWNED_SYNCHRONIZATION") // jdk printstreams are sync I don't want interleaving.
  public static void dumpToPrintStream(final PrintStream stream) {
    synchronized (stream) {
      Thread[] threads = getThreads();
      StackTraceElement[][] stackTraces = getStackTraces(threads);
      for (int i = 0; i < threads.length; i++) {
        StackTraceElement[] stackTrace = stackTraces[i];
        if (stackTrace != null && stackTrace.length > 0) {
          Thread thread = threads[i];
          stream.println("Thread " + thread.getName());
          try {
            Throwables.writeTo(stackTrace, stream, Throwables.PackageDetail.SHORT, true);
          } catch (IOException ex) {
            throw new UncheckedIOException(ex);
          }
        }
      }
    }
  }

}
