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

import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.sun.management.UnixOperatingSystemMXBean;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nullable;
import org.slf4j.LoggerFactory;
import static org.spf4j.base.Runtime.PID;
import static org.spf4j.base.Runtime.isMacOsx;
import org.spf4j.unix.Lsof;
import org.spf4j.unix.UnixException;
import org.spf4j.unix.UnixResources;

/**
 * Utility to wrap access to JDK specific Operating system Mbean attributes.
 * @author Zoltan Farkas
 */
public final class OperatingSystem {

  private static final class Lazy {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(Lazy.class);
  }

  private static final Path FD_FOLDER = Paths.get("/proc/" + PID + "/fd");

  private static final OperatingSystemMXBean OS_MBEAN;

  private static final com.sun.management.OperatingSystemMXBean SUN_OS_MBEAN;

  private static final com.sun.management.UnixOperatingSystemMXBean UNIX_OS_MBEAN;

  public static final long MAX_NR_OPENFILES;

  static {
    OS_MBEAN = ManagementFactory.getOperatingSystemMXBean();
    if (OS_MBEAN instanceof com.sun.management.OperatingSystemMXBean) {
      SUN_OS_MBEAN = (com.sun.management.OperatingSystemMXBean) OS_MBEAN;
    } else {
      SUN_OS_MBEAN = null;
    }
    if (OS_MBEAN instanceof com.sun.management.UnixOperatingSystemMXBean) {
      UNIX_OS_MBEAN = (com.sun.management.UnixOperatingSystemMXBean) OS_MBEAN;
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

  private OperatingSystem() { }

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


}
