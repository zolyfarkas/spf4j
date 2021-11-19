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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * a new cleaned up version of Runtime.
 *
 * @author Zoltan Farkas
 */
public final class XRuntime {

  private static final XRuntime INSTANCE = new XRuntime();

  private final java.lang.Runtime runtime;

  private XRuntime() {
    runtime = java.lang.Runtime.getRuntime();
  }

  public static XRuntime get() {
    return INSTANCE;
  }

  public void goDownWithError(final SysExits exitCode) {
    goDownWithError(null, exitCode.exitCode());
  }

  public void goDownWithError(@Nullable final Throwable t, final SysExits exitCode) {
    goDownWithError(t, exitCode.exitCode());
  }

  // Calling Halt is the only sensible thing to do when the JVM is hosed.
  @SuppressFBWarnings("MDM_RUNTIME_EXIT_OR_HALT")
  public void goDownWithError(@Nullable final Throwable t, final int exitCode) {
    try {
      if (t != null) {
        Throwables.writeTo(t, System.err, Throwables.PackageDetail.NONE); //High probability attempt to log first
        ErrLog.error("Error, going down with exit code " + exitCode, t);
        //Now we are pushing it...
        Logger logger = Logger.getLogger(Runtime.class.getName());
        logger.log(Level.SEVERE, "Error, going down with exit code {0}", exitCode);
        logger.log(Level.SEVERE, "Exception detail", t);
      } else {
        ErrLog.error("Error, going down with exit code " + exitCode);
        Logger.getLogger(Runtime.class.getName())
                .log(Level.SEVERE, "Error, going down with exit code {0}", exitCode);
      }
    } finally {
      runtime.halt(exitCode);
    }
  }

  @Override
  public String toString() {
    return "XRuntime{" + "runtime=" + runtime + '}';
  }

}
