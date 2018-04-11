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
package org.spf4j.os;

import com.google.common.base.Joiner;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.sun.management.UnixOperatingSystemMXBean;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Pair;
import org.spf4j.base.Runtime;
import org.spf4j.base.Throwables;
import static org.spf4j.base.Runtime.PID;
import static org.spf4j.base.Runtime.isMacOsx;
import org.spf4j.base.SysExits;
import org.spf4j.base.TimeSource;
import org.spf4j.concurrent.DefaultExecutor;
import org.spf4j.concurrent.Futures;
import org.spf4j.unix.Lsof;
import org.spf4j.unix.UnixException;
import org.spf4j.unix.UnixResources;

/**
 * Utility to wrap access to JDK specific Operating system Mbean attributes.
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY")
public final class OperatingSystem {

  private static final long ABORT_TIMEOUT_MILLIS = Long.getLong("spf4j.os.abortTimeoutMillis", 5000);

  private static final Path FD_FOLDER = Paths.get("/proc/" + PID + "/fd");

  private static final OperatingSystemMXBean OS_MBEAN;

  private static final com.sun.management.OperatingSystemMXBean SUN_OS_MBEAN;

  private static final UnixOperatingSystemMXBean UNIX_OS_MBEAN;

  public static final long MAX_NR_OPENFILES;

  static {
    OS_MBEAN = ManagementFactory.getOperatingSystemMXBean();
    if (OS_MBEAN instanceof com.sun.management.OperatingSystemMXBean) {
      SUN_OS_MBEAN = (com.sun.management.OperatingSystemMXBean) OS_MBEAN;
    } else {
      SUN_OS_MBEAN = null;
    }
    if (OS_MBEAN instanceof UnixOperatingSystemMXBean) {
      UNIX_OS_MBEAN = (UnixOperatingSystemMXBean) OS_MBEAN;
      MAX_NR_OPENFILES = UNIX_OS_MBEAN.getMaxFileDescriptorCount();
    } else {
      UNIX_OS_MBEAN = null;
      if (Runtime.isWindows()) {
        MAX_NR_OPENFILES = Integer.MAX_VALUE;
      } else if (Runtime.haveJnaPlatformClib()) {
        try {
          MAX_NR_OPENFILES = UnixResources.RLIMIT_NOFILE.getSoftLimit();
        } catch (UnixException ex) {
          throw new ExceptionInInitializerError(ex);
        }
      } else {
        MAX_NR_OPENFILES = Integer.MAX_VALUE;
      }
    }
  }

  private static final class Lazy {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(Lazy.class);
  }

  private OperatingSystem() {
  }

  public static OperatingSystemMXBean getOSMbean() {
    return OS_MBEAN;
  }

  @Nullable
  public static com.sun.management.OperatingSystemMXBean getSunJdkOSMBean() {
    return SUN_OS_MBEAN;
  }

  @Nullable
  public static UnixOperatingSystemMXBean getUnixOsMBean() {
    return UNIX_OS_MBEAN;
  }

  public static long getOpenFileDescriptorCount() {
    if (UNIX_OS_MBEAN != null) {
      return UNIX_OS_MBEAN.getOpenFileDescriptorCount();
    } else {
      try {
        if (isMacOsx()) {
          try {
            return Lsof.getNrOpenFiles();
          } catch (ExecutionException | TimeoutException ex) {
            Lazy.LOG.warn("Unable to get nr of open files", ex);
            return -1;
          } catch (InterruptedException ex) {
            Lazy.LOG.warn("Unable to get nr of open files", ex);
            Thread.currentThread().interrupt();
            return -1;
          }
        } else {
          if (Files.isDirectory(FD_FOLDER)) {
            int result = 0;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(FD_FOLDER)) {
              Iterator<Path> iterator = stream.iterator();
              while (iterator.hasNext()) {
                iterator.next();
                result++;
              }
            }
            return result;
          } else {
            return -1;
          }
        }
      } catch (IOException ex) {
        String msg = ex.getMessage();
        if (msg != null && msg.contains("Too many open files")) {
          return getMaxFileDescriptorCount();
        } else {
          Lazy.LOG.warn("Unable to get nr of open files", ex);
          return -1;
        }
      }
    }
  }

  public static long getMaxFileDescriptorCount() {
    return MAX_NR_OPENFILES;
  }

  public static int killProcess(final Process proc, final long terminateTimeoutMillis,
          final long forceTerminateTimeoutMillis)
          throws InterruptedException {

    proc.destroy();
    if (proc.waitFor(terminateTimeoutMillis, TimeUnit.MILLISECONDS)) {
      return proc.exitValue();
    } else {
      proc.destroyForcibly();
      if (!proc.waitFor(forceTerminateTimeoutMillis, TimeUnit.MILLISECONDS)) {
        throw new UncheckedTimeoutException("Cannot terminate " + proc);
      } else {
        return proc.exitValue();
      }
    }
  }

  @SuppressFBWarnings({ "COMMAND_INJECTION", "CC_CYCLOMATIC_COMPLEXITY" })
  public static <T, E> ProcessResponse<T, E> forkExec(final String[] command, final ProcessHandler<T, E> handler,
          final long timeoutMillis, final long terminationTimeoutMillis)
          throws IOException, InterruptedException, ExecutionException, TimeoutException {
    final Process proc = java.lang.Runtime.getRuntime().exec(command);
    try (InputStream pos = proc.getInputStream();
            InputStream pes = proc.getErrorStream();
            OutputStream pis = proc.getOutputStream()) {
      Future<E> esh;
      Future<T> osh;
      try {
        esh = DefaultExecutor.INSTANCE.submit(() -> handler.handleStdErr(pes));
      } catch (RuntimeException ex) { // Executor might Reject
        int result = killProcess(proc, terminationTimeoutMillis, 5000);
        throw new ExecutionException("Cannot exec stderr handler, killed process returned " + result, ex);
      }
      try {
        osh = DefaultExecutor.INSTANCE.submit(() -> handler.handleStdOut(pos));
      } catch (RuntimeException ex) { // Executor might Reject
        RuntimeException cex = Futures.cancelAll(true, esh);
        if (cex != null) {
          ex.addSuppressed(cex);
        }
        int result = killProcess(proc, terminationTimeoutMillis, ABORT_TIMEOUT_MILLIS);
        throw new ExecutionException("Cannot execute stdout handler, killed process returned " + result, ex);
      }
      long deadlineNanos = TimeSource.nanoTime() + TimeUnit.NANOSECONDS.convert(timeoutMillis, TimeUnit.MILLISECONDS);
      try {
        handler.writeStdIn(pis);
      } catch (RuntimeException ex) {
        RuntimeException cex = Futures.cancelAll(true, esh, osh);
        if (cex != null) {
          ex.addSuppressed(cex);
        }
        int result = killProcess(proc, terminationTimeoutMillis, ABORT_TIMEOUT_MILLIS);
        throw new ExecutionException("Filure executing stdin handler, killed process returned " + result, ex);
      }
      boolean isProcessFinished;
      try {
        isProcessFinished = proc.waitFor(timeoutMillis, TimeUnit.MILLISECONDS);
      } catch (InterruptedException ex) {
        RuntimeException cex = Futures.cancelAll(true, osh, esh);
        if (cex != null) {
          ex.addSuppressed(cex);
        }
        OperatingSystem.killProcess(proc, terminationTimeoutMillis, ABORT_TIMEOUT_MILLIS);
        throw ex;
      }
      if (isProcessFinished) {
        Pair<Map<Future, Object>, Exception> results = Futures.getAllWithDeadlineNanos(deadlineNanos, osh, esh);
        Exception hex = results.getSecond();
        Map<Future, Object> asyncRes = results.getFirst();
        if (hex == null) {
          return new ProcessResponse<>(proc.exitValue(), (T) asyncRes.get(osh), (E) asyncRes.get(esh));
        } else {
          Throwables.throwException(hex);
          throw new IllegalStateException();
        }
      } else {
        int result = killProcess(proc, terminationTimeoutMillis, ABORT_TIMEOUT_MILLIS);
        RuntimeException cex = Futures.cancelAll(true, osh, esh);
        TimeoutException tex = new TimeoutException("Timed out while executing: " + java.util.Arrays.toString(command)
                + ";\nprocess returned " + result + ";\nhandler:\n" + handler);
        if (cex != null) {
          tex.addSuppressed(cex);
        }
        throw tex;
      }
    }
  }

  @CheckReturnValue
  public static String forkExec(final String[] command,
          final long timeoutMillis) throws IOException, InterruptedException, ExecutionException, TimeoutException {
    ProcessResponse<String, String> resp
            = forkExec(command, new StdOutToStringProcessHandler(), timeoutMillis, 60000);
    if (resp.getResponseExitCode() != SysExits.OK) {
      throw new ExecutionException("Failed to execute " + java.util.Arrays.toString(command)
              + ", returned" + resp.getResponseCode() + ", stderr = " + resp.getErrOutput(), null);
    }
    return resp.getOutput();
  }

  public static void forkExecLog(final String[] command,
          final long timeoutMillis) throws IOException, InterruptedException, ExecutionException, TimeoutException {
    ProcessResponse<Void, Void> resp
            = forkExec(command,
                    new LoggingProcessHandler(LoggerFactory.getLogger("fork." + Joiner.on('.').join(command))),
                    timeoutMillis, 60000);
    if (resp.getResponseExitCode() != SysExits.OK) {
      throw new ExecutionException("Failed to execute " + java.util.Arrays.toString(command)
              + ", returned" + resp.getResponseCode() + ", stderr = " + resp.getErrOutput(), null);
    }
  }

}
