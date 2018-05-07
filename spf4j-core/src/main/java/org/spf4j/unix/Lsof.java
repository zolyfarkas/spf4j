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
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import static org.spf4j.base.Runtime.PID;
import static org.spf4j.base.Runtime.run;
import org.spf4j.base.SysExits;
import org.spf4j.os.OperatingSystem;
import org.spf4j.os.ProcessResponse;
import org.spf4j.os.StdOutLineCountProcessHandler;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings({ "DMI_HARDCODED_ABSOLUTE_FILENAME", "FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY" })
// Circular dependency will be gone when deprecated methods from Runtime will be removed.
public final class Lsof {

  private static final File LSOF;

  static {
    File lsofFile = new File("/usr/sbin/lsof");
    if (!lsofFile.exists() || !lsofFile.canExecute()) {
      lsofFile = new File("/usr/bin/lsof");
      if (!lsofFile.exists() || !lsofFile.canExecute()) {
        lsofFile = new File("/usr/local/bin/lsof");
        if (!lsofFile.exists() || !lsofFile.canExecute()) {
          lsofFile = null;
        }
      }
    }
    if (lsofFile == null) {
      Logger.getLogger(Lsof.class.getName()).warning("lsof unavailable on this system");
    }
    LSOF = lsofFile;
  }

  private static final String[] LSOF_CMD = (LSOF == null) ? null
          : new String[]{LSOF.getAbsolutePath(), "-p", Integer.toString(PID)};


  private Lsof() { }

  public static int getNrOpenFiles() throws IOException, InterruptedException, ExecutionException, TimeoutException {
    if (Lsof.LSOF == null) {
      return -1;
    }
    ProcessResponse<Long, String> resp =
            OperatingSystem.forkExec(LSOF_CMD, new StdOutLineCountProcessHandler(), 60000, 60000);
    if (resp.getResponseExitCode() != SysExits.OK) {
      throw new ExecutionException("Failed to execute " + java.util.Arrays.toString(LSOF_CMD)
              + ", returned" + resp.getResponseCode() + ", stderr = " + resp.getErrOutput(), null);
    }
    return (int) (resp.getOutput() - 1);
  }

  @Nullable
  @CheckReturnValue
  public static CharSequence getLsofOutput() {
    if (Lsof.LSOF == null) {
      return null;
    }
    try {
      return run(Lsof.LSOF_CMD, 60000);
    } catch (IOException | ExecutionException | TimeoutException ex) {
      Logger.getLogger(Lsof.class.getName()).log(Level.WARNING, "Unable to run lsof", ex);
      return null;
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return null;
    }
  }

}
