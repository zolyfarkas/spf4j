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

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.StringArray;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.platform.unix.LibCAPI;
import com.sun.jna.ptr.IntByReference;

/**
 * GNU C library.
 */
public interface CLibrary extends LibCAPI, Library {

  CLibrary INSTANCE = (CLibrary) Native.loadLibrary("c", CLibrary.class);

   // obtained from Linux. Needs to be checked if these values are portable.
  int F_GETFD = 1;

  int F_SETFD = 2;

  int FD_CLOEXEC = 1;

  int fork();

  /**
   * returns signal name from a signal number.
   * @param sigNumber the signal number.
   * @return the signal name.
   */
  String strsignal(int sigNumber);

  int kill(int pid, int signum);
  
  int setsid();

  int setuid(short newuid);

  int setgid(short newgid);

  int umask(int mask);

  /**
   * Get current process id.
   * https://www.systutorials.com/docs/linux/man/2-getppid/
   * @return current process id.
   */
  int getpid();

  /**
   * get parent process id.
   * https://www.systutorials.com/docs/linux/man/2-getppid/
   * @return parent process id.
   */
  int getppid();

  int chdir(String dir);

  int execv(String file, StringArray args);

  int execvp(String file, StringArray args);

  int setenv(String name, String value);

  void perror(String msg);

  String strerror(int errno);

  // this is listed in http://developer.apple.com/DOCUMENTATION/Darwin/Reference/ManPages/man3/sysctlbyname.3.html
  // but not in http://www.gnu.org/software/libc/manual/html_node/System-Parameters.html#index-sysctl-3493
  // perhaps it is only supported on BSD?
  int sysctlbyname(String name, Pointer oldp, IntByReference oldlenp, Pointer newp, IntByReference newlen);

  int sysctl(int[] mib, int nameLen, Pointer oldp, IntByReference oldlenp, Pointer newp, IntByReference newlen);

  int sysctlnametomib(String name, Pointer mibp, IntByReference size);

  class FILE extends PointerType {

    public FILE() {
    }

    public FILE(final Pointer pointer) {
      super(pointer);
    }
  }

  FILE fopen(String fileName, String mode);

  //FILE *freopen(const char *filename, const char *mode, FILE *stream)
  FILE freopen(String fileName, String mode, FILE stream);

  int fseek(FILE file, long offset, int whence);

  long ftell(FILE file);

  int fread(Pointer buf, int size, int count, FILE file);

  int fclose(FILE file);

  int getdtablesize();

  int fcntl(int fd, int command);

  int fcntl(int fd, int command, int flags);

  /**
   * Read a symlink. The name will be copied into the specified memory, and returns the number of bytes copied. The
   * string is not null-terminated.
   *
   * @return if the return value equals size, the caller needs to retry with a bigger buffer. If -1, error.
   */
  int readlink(String filename, Memory buffer, NativeLong size);

}
