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
 * @author zoly
 */
@SuppressFBWarnings("ACEM_ABSTRACT_CLASS_EMPTY_METHODS")
public abstract class AbstractRunnable implements Runnable {

  @Deprecated
  public static final int ERROR_EXIT_CODE = SysExits.EX_SOFTWARE.exitCode();


  public static final Runnable NOP = () -> { };

  private final boolean lenient;

  private final String threadName;

  /**
   * Create runnable lenient or not with a specific thread name during its execution.
   *
   * @param lenient - If lenient is true, it means that nobody is waiting for this runnable's result(finish) so To not
   * loose the exception, the runnable will LOG it as an error, and not retrow it
   * @param threadName - the thread name during the execution of this runnable.
   */
  public AbstractRunnable(final boolean lenient, @Nullable final String threadName) {
    this.lenient = lenient;
    this.threadName = threadName;
  }

  /**
   * create runnable.
   *
   * @param lenient - If lenient is true, it means that nobody is waiting for this runnable's result(finish) so To not
   * loose the exception, the runnable will LOG it as an error, and not retrow it
   */
  public AbstractRunnable(final boolean lenient) {
    this(lenient, null);
  }

  public AbstractRunnable() {
    this(false, null);
  }

  public AbstractRunnable(final String threadName) {
    this(false, threadName);
  }

  @Override
  public final void run() {
    Thread thread = null;
    String origName = null;
    if (threadName != null) {
      thread = Thread.currentThread();
      origName = thread.getName();
      thread.setName(threadName);
    }

    try {
      doRun();
    } catch (Exception ex) {
      if (org.spf4j.base.Throwables.containsNonRecoverable(ex)) {
        Runtime.goDownWithError(ex, SysExits.EX_SOFTWARE);
      }
      if (lenient) {
        Logger.getLogger(AbstractRunnable.class.getName()).log(Level.SEVERE, "Exception in runnable: ", ex);
      } else {
        throw new UncheckedExecutionException(ex);
      }
    } catch (Throwable ex) {
      if (org.spf4j.base.Throwables.containsNonRecoverable(ex)) {
        Runtime.goDownWithError(ex, SysExits.EX_SOFTWARE);
      }
      throw ex;
    } finally {
      if (thread != null) {
        thread.setName(origName);
      }
    }
  }

  public abstract void doRun() throws Exception;

}
