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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utility to wrap access to JDK specific Operating system Mbean attributes.
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY")
public final class OperatingSystem {

  private static final boolean IS_MAC_OSX;
  private static final boolean IS_WINDOWS;
  private static final String OS_NAME;

  static {
    final String osName = System.getProperty("os.name");
    OS_NAME = osName;
    IS_MAC_OSX = "Mac OS X".equals(osName);
    IS_WINDOWS = osName.startsWith("Windows");

  }

  private OperatingSystem() {
  }

  public static boolean isMacOsx() {
    return IS_MAC_OSX;
  }

  public static boolean isWindows() {
    return IS_WINDOWS;
  }

  public static String getOsName() {
    return OS_NAME;
  }


}
