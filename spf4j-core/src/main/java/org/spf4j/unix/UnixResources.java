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
package org.spf4j.unix;

import com.sun.jna.Native;
import com.sun.jna.platform.unix.Resource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.spf4j.base.Runtime;

/**
 * Possible values of the first parameter to getrlimit()/setrlimit()
 * A combination of com.sun.jna.platform.unix.Resource and MACOSX resource.h
 * this class requires jna-platforn which is a optional dependency.
 * @author zoly
 */
@SuppressFBWarnings("FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY")
// class circularity should go away when deprecated methods from Runtime are removed.
public enum UnixResources {


  /**
   * 0 Per-process CPU limit, in seconds.
   */
  RLIMIT_CPU(0),
  /**
   * 1 Largest file that can be created, in bytes.
   */
  RLIMIT_FSIZE(1),
  /**
   * 2 Maximum size of data segment, in bytes.
   */
  RLIMIT_DATA(2),
  /**
   * 3 Maximum size of stack segment, in bytes.
   */
  RLIMIT_STACK(3),
  /**
   * 4 Largest core file that can be created, in bytes.
   */
  RLIMIT_CORE(4),
  /**
   * 5 Largest resident set size, in bytes. This affects swapping; processes that are exceeding their resident set size
   * will be more likely to have physical memory taken from them.
   */
  RLIMIT_RSS(5),
  /**
   * 6 Number of processes.
   */
  RLIMIT_NPROC(6, 7),
  /**
   * 7 Number of open files.
   */
  RLIMIT_NOFILE(7, 8),
  /**
   * 8 Locked-in-memory address space.
   */
  RLIMIT_MEMLOCK(8, 6),
  /**
   * 9 Address space limit.
   */
  RLIMIT_AS(9, 5),
  /**
   * 10 Maximum number of file locks.
   */
  RLIMIT_LOCKS(10, -1),
  /**
   * 11 Maximum number of pending signals.
   */
  RLIMIT_SIGPENDING(11, -1),
  /**
   * 12 Maximum bytes in POSIX message queues.
   */
  RLIMIT_MSGQUEUE(12, -1),
  /**
   * 13 Maximum nice priority allowed to raise to. Nice levels 19 .. -20 correspond to 0 .. 39 values of this resource
   * limit.
   */
  RLIMIT_NICE(13, -1),
  /**
   * 14
   */
  RLIMIT_RTPRIO(14, -1),
  /**
   * 15 Maximum CPU time in microseconds that a process scheduled under a real-time scheduling policy may consume
   * without making a blocking system call before being forcibly de-scheduled.
   */
  RLIMIT_RTTIME(15, -1),
  /**
   * 16 Number of {@code rlimit} values
   */
  RLIMIT_NLIMITS(16, 9);

  private final int macId;
  private final int gnuId;

  UnixResources(final int gnuId) {
    this.macId = gnuId;
    this.gnuId = gnuId;
  }

  UnixResources(final int gnuId, final int macId) {
    this.macId = macId;
    this.gnuId = gnuId;
  }

  public int getMacId() {
    return macId;
  }

  public int getGnuId() {
    return gnuId;
  }

  public long getSoftLimit() throws UnixException {
    return getRLimit(this).rlim_cur;
  }

  public void setSoftLimit(final long limit) throws UnixException {
    setRLimit(this, limit, getHardLimit());
  }

  public void setLimits(final long softLimit, final long hardlimit) throws UnixException {
    setRLimit(this, softLimit, hardlimit);
  }

  public long getHardLimit() throws UnixException {
    return getRLimit(this).rlim_max;
  }

  private static Resource.Rlimit getRLimit(final UnixResources resourceId) throws UnixException {
    int id = Runtime.isMacOsx() ? resourceId.macId : resourceId.gnuId;
    if (id < 0) {
      throw new UnixException("Unsupported " + id + " limit on " + Runtime.OS_NAME, 0);
    }
    final Resource.Rlimit limit = new Resource.Rlimit();
    int err = CLibrary.INSTANCE.getrlimit(id, limit);
    if (err != 0) {
      int lastError = Native.getLastError();
      throw new UnixException("Error code " + CLibrary.INSTANCE.strerror(lastError)
              + " for getrlimit(" + id + ", " + limit + '\'', lastError);
    }
    return limit;
  }

  private static void setRLimit(final UnixResources resourceId, final long softValue, final long hardValue)
          throws UnixException {
    int id = Runtime.isMacOsx() ? resourceId.macId : resourceId.gnuId;
    if (id < 0) {
      throw new UnixException("Unsupported " + id + " limit on " + Runtime.OS_NAME, 0);
    }
    final Resource.Rlimit limit = new Resource.Rlimit();
    limit.rlim_cur = softValue;
    limit.rlim_max = hardValue;
    int err = CLibrary.INSTANCE.setrlimit(id, limit);
    if (err != 0) {
      int lastError = Native.getLastError();
      throw new UnixException("Error " + CLibrary.INSTANCE.strerror(lastError)
              + " for setrlimit(" + id + ", " + limit + '\'', lastError);
    }
  }

}
