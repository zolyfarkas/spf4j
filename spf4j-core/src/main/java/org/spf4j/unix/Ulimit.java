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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.spf4j.base.Arrays;

/**
 * @deprecated see UnixResources for better/faster alternative.
 *
 */
@SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
@Deprecated
public final class Ulimit {

  private static final String[] ULIMIT_CMD;

  static {
    if (org.spf4j.base.Runtime.isWindows()) {
      ULIMIT_CMD = null;
    } else {
      ULIMIT_CMD = findUlimitCmd();
    }
  }

  private Ulimit() {
  }

  @SuppressFBWarnings("PZLA_PREFER_ZERO_LENGTH_ARRAYS")
  @Nullable
  private static String[] findUlimitCmd() {
    final File bash = new File("/bin/bash");
    final File sh = new File("/bin/sh");
    final File uLimit = new File("/usr/bin/ulimit");
    if (uLimit.exists() && uLimit.canExecute()) {
      return new String[]{uLimit.getPath()};
    } else if (bash.exists() && bash.canExecute()) {
      return new String[]{bash.getPath(), "-c", "ulimit"};
    } else if (sh.exists() && sh.canExecute()) {
      return new String[]{sh.getPath(), "-c", "ulimit"};
    } else {
      return null;
    }
  }

  /**
   * @param options
   * @return number of max open files for the current process. If unable to find out System Max Limit this value will be
   * Integer.MAX_VALUE
   */
  public static int runUlimit(final String... options) {
    if (ULIMIT_CMD == null) {
      Logger.getLogger(Ulimit.class.getName()).warning("Ulimit not available, assuming no limits");
      return Integer.MAX_VALUE;
    }
    int mfiles;
    try {
      String[] cmd = Arrays.concat(ULIMIT_CMD, options);
      String result = org.spf4j.base.Runtime.run(cmd, 10000).toString();
      if (result.contains("unlimited")) {
        mfiles = Integer.MAX_VALUE;
      } else {
        try {
          mfiles = Integer.parseInt(result.trim());
        } catch (NumberFormatException ex) {
          Logger.getLogger(Ulimit.class.getName()).log(Level.WARNING,
                  "Error while parsing ulimit output, assuming no limit", ex);
          mfiles = Integer.MAX_VALUE;
        }
      }
    } catch (TimeoutException | IOException | ExecutionException ex) {
      Logger.getLogger(Ulimit.class.getName()).log(Level.WARNING,
              "Error while parsing ulimit output, assuming no limit", ex);
      mfiles = Integer.MAX_VALUE;
    } catch (InterruptedException ex) {
      Logger.getLogger(Ulimit.class.getName()).log(Level.WARNING,
              "Ulimit interrupted, assuming no limit", ex);
      Thread.currentThread().interrupt();
      mfiles = Integer.MAX_VALUE;
    }
    return mfiles;
  }

}
