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
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
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
            Logger.getLogger(OperatingSystem.class.getName()).log(Level.WARNING, "Unable to get nr of open files", ex);
            return -1;
          } catch (InterruptedException ex) {
            Logger.getLogger(OperatingSystem.class.getName()).log(Level.WARNING, "Unable to get nr of open files", ex);
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
          Logger.getLogger(OperatingSystem.class.getName()).log(Level.WARNING, "Unable to get nr of open files", ex);
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
          throws InterruptedException, TimeoutException {

    proc.destroy();
    if (proc.waitFor(terminateTimeoutMillis, TimeUnit.MILLISECONDS)) {
      return proc.exitValue();
    } else {
      proc.destroyForcibly();
      if (!proc.waitFor(forceTerminateTimeoutMillis, TimeUnit.MILLISECONDS)) {
        throw new TimeoutException("Cannot terminate " + proc);
      } else {
        return proc.exitValue();
      }
    }
  }

  /**
   * Process execution utility.
   * @param <T> type the stdout is reduced to.
   * @param <E> type stderr is reduced to.
   * @param command the command to execute.
   * @param handler handler for child stdin, stdout and stderr. stdout and stderr handling will be done in 2 threads
   * from the DefaultExecutor thread pool. while stdin handling will execute in the current thread.
   * @param timeoutMillis time to wait for the process to execute.
   * @param terminationTimeoutMillis this is the timeout used when trying to terminate the process gracefully.
   * @return the response (respCode, stdout reduction, stderr reduction)
   * @throws IOException
   * @throws InterruptedException
   * @throws ExecutionException
   * @throws TimeoutException when timeout happens.
   */
  @SuppressFBWarnings({ "COMMAND_INJECTION", "CC_CYCLOMATIC_COMPLEXITY" })
  public static <T, E> ProcessResponse<T, E> forkExec(final String[] command, final ProcessHandler<T, E> handler,
          final long timeoutMillis, final long terminationTimeoutMillis)
          throws IOException, InterruptedException, ExecutionException, TimeoutException {
    final Process proc = java.lang.Runtime.getRuntime().exec(command);
    handler.started(proc);
    try (InputStream pos = proc.getInputStream();
            InputStream pes = proc.getErrorStream();
            OutputStream pis = proc.getOutputStream()) {
      Future<E> esh;
      Future<T> osh;
      final AtomicReference<Throwable> eex = new AtomicReference<>();
      try {
        esh = DefaultExecutor.INSTANCE.submit(() -> {
          try {
            return handler.handleStdErr(pes);
          } catch (Throwable t) {
            eex.set(t);
            throw t;
          }
        });
      } catch (RuntimeException ex) { // Executor might Reject
        int result = killProcess(proc, terminationTimeoutMillis, ABORT_TIMEOUT_MILLIS);
        throw new ExecutionException("Cannot execute stderr handler, killed process returned " + result, ex);
      }
      try {
        osh = DefaultExecutor.INSTANCE.submit(() -> {
          try {
          return handler.handleStdOut(pos);
          } catch (Throwable t) {
            eex.set(t);
            throw t;
          }
        });
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
      } catch (RuntimeException | IOException ex) {
        RuntimeException cex = Futures.cancelAll(true, esh, osh);
        if (cex != null) {
          ex.addSuppressed(cex);
        }
        int result = killProcess(proc, terminationTimeoutMillis, ABORT_TIMEOUT_MILLIS);
        throw new ExecutionException("Failure executing stdin handler, killed process returned " + result, ex);
      }
      try {
        int result = waitFor(eex, proc, timeoutMillis, TimeUnit.MILLISECONDS);
        Pair<Map<Future, Object>, Exception> results = Futures.getAllWithDeadlineNanos(deadlineNanos, osh, esh);
        Exception hex = results.getSecond();
        Map<Future, Object> asyncRes = results.getFirst();
        if (hex == null) {
          return new ProcessResponse<>(result, (T) asyncRes.get(osh), (E) asyncRes.get(esh));
        } else {
          Throwables.throwException(hex);
          throw new IllegalStateException();
        }
      } catch (TimeoutException | ExecutionException | InterruptedException ex) {
        killProcess(proc, terminationTimeoutMillis, ABORT_TIMEOUT_MILLIS);
        RuntimeException cex = Futures.cancelAll(true, osh, esh);
        if (cex != null) {
          ex.addSuppressed(cex);
        }
        throw ex;
      }
    }
  }

  @SuppressFBWarnings("MDM_THREAD_YIELD") // best I can come up with so far. (same as JDK)
  private static int waitFor(final AtomicReference<Throwable> exr,
          final Process process,
          final long timeout, final TimeUnit unit)
          throws InterruptedException, ExecutionException, TimeoutException {
    long startTime = TimeSource.nanoTime();
    long rem = unit.toNanos(timeout);
    do {
      try {
        return process.exitValue();
      } catch (IllegalThreadStateException ex) {
        if (rem > 0) {
          Thread.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1, 100));
        }
      }
      rem = unit.toNanos(timeout) - (TimeSource.nanoTime() - startTime);
      Throwable get = exr.get();
      if (get != null) {
        throw new ExecutionException(get);
      }
    } while (rem > 0);
    throw new TimeoutException("Process " + process + " timed out after " + timeout + " " + unit);
  }


  @CheckReturnValue
  public static String forkExec(final String[] command,
          final long timeoutMillis) throws IOException, InterruptedException, ExecutionException, TimeoutException {
    ProcessResponse<String, String> resp
            = forkExec(command, new StdOutToStringProcessHandler(), timeoutMillis, 60000);
    if (resp.getResponseExitCode() != SysExits.OK) {
      throw new ExecutionException("Failed to execute " + Arrays.toString(command)
              + ", exitCode = " + resp.getResponseCode() + ", stderr = " + resp.getErrOutput(), null);
    }
    return resp.getOutput();
  }

  public static void forkExecLog(final String[] command,
          final long timeoutMillis) throws IOException, InterruptedException, ExecutionException, TimeoutException {
    ProcessResponse<Void, Void> resp
            = forkExec(command,
                    new LoggingProcessHandler(Logger.getLogger("fork." + command[0])),
                    timeoutMillis, 60000);
    if (resp.getResponseExitCode() != SysExits.OK) {
      throw new ExecutionException("Failed to execute " + java.util.Arrays.toString(command)
              + ", exitCode = " + resp.getResponseCode() + ", stderr = " + resp.getErrOutput(), null);
    }
  }

}
