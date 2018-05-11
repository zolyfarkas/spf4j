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

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import java.lang.reflect.Field;
import org.spf4j.base.UncheckedExecutionException;

/**
 * PID is exposed in jdk 9... this will go away.
 * @author Zoltan Farkas
 */
public final class ProcessUtil {

  private ProcessUtil() { }

  public static int getPid(final Process p) {
    if (org.spf4j.base.Runtime.isWindows()) {
      return getWindowsPid(p);
    }
    return getUnixPid(p);
  }

  private static int getUnixPid(final Process p) {
    try {
      Field pidF = p.getClass().getDeclaredField("pid");
      pidF.setAccessible(true);
      return pidF.getInt(p);
    } catch (IllegalAccessException | NoSuchFieldException | SecurityException ex) {
      throw new UncheckedExecutionException("Cannot get PID for " + p, ex);
    }
  }

  private static int getWindowsPid(final Process p) {
    Field f;
    long lHandle;
    try {
      f = p.getClass().getDeclaredField("handle");
      f.setAccessible(true);
      lHandle = f.getLong(p);
    } catch (NoSuchFieldException | SecurityException | IllegalAccessException ex) {
      throw new UncheckedExecutionException("Cannot get PID for " + p, ex);
    }

    Kernel32 kernel = Kernel32.INSTANCE;
    WinNT.HANDLE handle = new WinNT.HANDLE();
    handle.setPointer(Pointer.createConstant(lHandle));
    return kernel.GetProcessId(handle);
  }

}
