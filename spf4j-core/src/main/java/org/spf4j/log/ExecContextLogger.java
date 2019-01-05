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
package org.spf4j.log;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.spf4j.base.Arrays;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.Wrapper;

/**
 * A Execution context aware logger.
 *
 * does the following:
 *
 * <li>
 * 1) if Execution context is present, it logs the context id. (relies on Logging back-ends supporting overflow args)
 * </li>
 * <li>
 * 2) if Execution context is present, it allows for context aware log level, and upgrades log
 * messages to be logged by backend.
 * </li>
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("LO_SUSPECT_LOG_PARAMETER")
public final class ExecContextLogger implements Logger, Wrapper<Logger> {

  private final XLog logger;


  public static ExecContextLogger from(final Logger wrapped) {
    if (wrapped instanceof ExecContextLogger) {
      return (ExecContextLogger) wrapped;
    } else {
      return new ExecContextLogger(wrapped);
    }
  }

  public ExecContextLogger(final Logger wrapped) {
    this(new SLf4jXLogAdapter(wrapped));
  }

  public ExecContextLogger(final XLog traceLogger) {
    this.logger = traceLogger;
  }

  @Override
  public Logger getWrapped() {
    return this.logger.getWrapped();
  }

  @Override
  public String getName() {
    return this.logger.getWrapped().getName();
  }


  public boolean isEnabled(final Level level, @Nullable final Marker marker) {
    ExecutionContext ctx = ExecutionContexts.current();
    if (ctx ==  null) {
      return logger.isEnabled(level, marker);
    }
    String name = getName();
    Level backendOverwrite = ctx.getBackendMinLogLevel(name);
    if (backendOverwrite == null) {
      return  logger.isEnabled(level, marker) || level.ordinal() >= ctx.getContextMinLogLevel(name).ordinal();
    } else {
      return  logger.isEnabled(level, marker)
              || level.ordinal()
              >= Math.min(ctx.getContextMinLogLevel(name).ordinal(), backendOverwrite.ordinal());
    }
  }

  public void log(@Nullable final Marker marker, final Level level, final String msg, final Object... args) {
    ExecutionContext ctx = ExecutionContexts.current();
    if (ctx ==  null) {
      logger.log(marker, level, msg, args);
      return;
    }
    String name = getName();
    boolean logged;
    if (logger.isEnabled(level, marker)) {
      logger.log(null, level, msg, Arrays.append(args, LogAttribute.traceId(ctx.getId())));
      logged = true;
    } else {
      Level backendOverwrite = ctx.getBackendMinLogLevel(name);
      if (backendOverwrite == null) {
        logged = false;
      } else if (backendOverwrite.ordinal() <= level.ordinal()) {
        logger.logUpgrade(null, level, msg, Arrays.append(args, LogAttribute.traceId(ctx.getId())));
        logged = true;
      } else {
        logged = false;
      }
    }
    if (ctx.getContextMinLogLevel(name).ordinal() <= level.ordinal()) {
      ctx.addLog(new Slf4jLogRecordImpl(logged, name, level, (Marker) null, msg));
    }
  }

  @Override
  public boolean isTraceEnabled() {
    return isEnabled(Level.TRACE, null);
  }

  @Override
  public void trace(final String msg) {
    log(null, Level.TRACE, msg);
  }

  @Override
  public void trace(final String format, final Object arg) {
    log(null, Level.TRACE, format, arg);
  }

  @Override
  public void trace(final String format, final Object arg1, final Object arg2) {
    log(null, Level.TRACE, format, arg1, arg2);
  }

  @Override
  public void trace(final String format, final Object... arguments) {
    log(null, Level.TRACE, format, arguments);
  }

  @Override
  public void trace(final String msg, final Throwable t) {
    log(null, Level.TRACE, msg, t);
  }

  @Override
  public boolean isTraceEnabled(final Marker marker) {
    return isEnabled(Level.TRACE, marker);
  }

  @Override
  public void trace(final Marker marker, final String msg) {
    log(marker, Level.TRACE, msg);
  }

  @Override
  public void trace(final Marker marker, final String format, final Object arg) {
    log(marker, Level.TRACE, format, arg);
  }

  @Override
  public void trace(final Marker marker, final String format, final Object arg1, final Object arg2) {
    log(marker, Level.TRACE, format, arg1, arg2);
  }

  @Override
  public void trace(final Marker marker, final String format, final Object... argArray) {
    log(marker, Level.TRACE, format, argArray);
  }

  @Override
  public void trace(final Marker marker, final String msg, final Throwable t) {
    log(marker, Level.TRACE, msg, t);
  }

  @Override
  public boolean isDebugEnabled() {
   return isEnabled(Level.DEBUG, null);
  }

  @Override
  public void debug(final String msg) {
    log(null, Level.DEBUG, msg);
  }

  @Override
  public void debug(final String format, final Object arg) {
    log(null, Level.DEBUG, format, arg);
  }

  @Override
  public void debug(final String format, final Object arg1, final Object arg2) {
    log(null, Level.DEBUG, format, arg1, arg2);
  }

  @Override
  public void debug(final String format, final Object... arguments) {
    log(null, Level.DEBUG, format, arguments);
  }

  @Override
  public void debug(final String msg, final Throwable t) {
    log(null, Level.DEBUG, msg, t);
  }

  @Override
  public boolean isDebugEnabled(final Marker marker) {
    return isEnabled(Level.DEBUG, marker);
  }

  @Override
  public void debug(final Marker marker, final String msg) {
    log(marker, Level.DEBUG, msg);
  }

  @Override
  public void debug(final Marker marker, final String format, final Object arg) {
    log(marker, Level.DEBUG, format, arg);
  }

  @Override
  public void debug(final Marker marker, final String format, final Object arg1, final Object arg2) {
    log(marker, Level.DEBUG, format, arg1, arg2);
  }

  @Override
  public void debug(final Marker marker, final String format, final Object... arguments) {
    log(marker, Level.DEBUG, format, arguments);
  }

  @Override
  public void debug(final Marker marker, final String msg, final Throwable t) {
    log(marker, Level.DEBUG, msg, t);
  }

  @Override
  public boolean isInfoEnabled() {
    return isEnabled(Level.INFO, null);
  }

  @Override
  public void info(final String msg) {
    log(null, Level.INFO, msg);
  }

  @Override
  public void info(final String format, final Object arg) {
    log(null, Level.INFO, format, arg);
  }

  @Override
  public void info(final String format, final Object arg1, final Object arg2) {
    log(null, Level.INFO, format, arg1, arg2);
  }

  @Override
  public void info(final String format, final Object... arguments) {
    log(null, Level.INFO, format, arguments);
  }

  @Override
  public void info(final String msg, final Throwable t) {
    log(null, Level.INFO, msg, t);
  }

  @Override
  public boolean isInfoEnabled(final Marker marker) {
    return isEnabled(Level.INFO, marker);
  }

  @Override
  public void info(final Marker marker, final String msg) {
    log(marker, Level.INFO, msg);
  }

  @Override
  public void info(final Marker marker, final String format, final Object arg) {
    log(marker, Level.INFO, format, arg);
  }

  @Override
  public void info(final Marker marker, final String format, final Object arg1, final Object arg2) {
    log(marker, Level.INFO, format, arg1, arg2);
  }

  @Override
  public void info(final Marker marker, final String format, final Object... arguments) {
    log(marker, Level.INFO, format, arguments);
  }

  @Override
  public void info(final Marker marker, final String msg, final Throwable t) {
    log(marker, Level.INFO, msg, t);
  }

  @Override
  public boolean isWarnEnabled() {
    return isEnabled(Level.WARN, null);
  }

  @Override
  public void warn(final String msg) {
    log(null, Level.WARN, msg);
  }

  @Override
  public void warn(final String format, final Object arg) {
    log(null, Level.WARN, format, arg);
  }

  @Override
  public void warn(final String format, final Object... arguments) {
    log(null, Level.WARN, format, arguments);
  }

  @Override
  public void warn(final String format, final Object arg1, final Object arg2) {
    log(null, Level.WARN, format, arg1, arg2);
  }

  @Override
  public void warn(final String msg, final Throwable t) {
    log(null, Level.WARN, msg, t);
  }

  @Override
  public boolean isWarnEnabled(final Marker marker) {
    return isEnabled(Level.WARN, marker);
  }

  @Override
  public void warn(final Marker marker, final String msg) {
    log(marker, Level.WARN, msg);
  }

  @Override
  public void warn(final Marker marker, final String format, final Object arg) {
    log(marker, Level.WARN, format, arg);
  }

  @Override
  public void warn(final Marker marker, final String format, final Object arg1, final Object arg2) {
    log(marker, Level.WARN, format, arg1, arg2);
  }

  @Override
  public void warn(final Marker marker, final String format, final Object... arguments) {
    log(marker, Level.WARN, format, arguments);
  }

  @Override
  public void warn(final Marker marker, final String msg, final Throwable t) {
    log(marker, Level.WARN, msg, t);
  }

  @Override
  public boolean isErrorEnabled() {
    return isEnabled(Level.ERROR, null);
  }

  @Override
  public void error(final String msg) {
    log(null, Level.ERROR, msg);
  }

  @Override
  public void error(final String format, final Object arg) {
    log(null, Level.ERROR, format, arg);
  }

  @Override
  public void error(final String format, final Object arg1, final Object arg2) {
    log(null, Level.ERROR, format, arg1, arg2);
  }

  @Override
  public void error(final String format, final Object... arguments) {
    log(null, Level.ERROR, format, arguments);
  }

  @Override
  public void error(final String msg, final Throwable t) {
    log(null, Level.ERROR, msg, t);
  }

  @Override
  public boolean isErrorEnabled(final Marker marker) {
    return isEnabled(Level.ERROR, marker);
  }

  @Override
  public void error(final Marker marker, final String msg) {
    log(marker, Level.ERROR, msg);
  }

  @Override
  public void error(final Marker marker, final String format, final Object arg) {
    log(marker, Level.ERROR, format, arg);
  }

  @Override
  public void error(final Marker marker, final String format, final Object arg1, final Object arg2) {
    log(marker, Level.ERROR, format, arg1, arg2);
  }

  @Override
  public void error(final Marker marker, final String format, final Object... arguments) {
    log(marker, Level.ERROR, format, arguments);
  }

  @Override
  public void error(final Marker marker, final String msg, final Throwable t) {
    log(marker, Level.ERROR, msg, t);
  }

  @Override
  public String toString() {
    return "ExecContextLogger{" + "traceLogger=" + this.logger + '}';
  }

}
