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
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.Wrapper;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("LO_SUSPECT_LOG_PARAMETER")
public final class ExecContextLogger implements Logger, Wrapper<Logger> {


  interface Log {

   void log(@Nullable Marker marker, Level level, String format, Object... args);

  }

  private final Logger wrapped;

  private final Log traceLogger;

  public ExecContextLogger(final Logger wrapped) {
    this(wrapped, new Log() {
      @Override
      @SuppressFBWarnings({"SA_LOCAL_SELF_COMPARISON", "SF_SWITCH_FALLTHROUGH"})
      public void log(@Nullable final  Marker marker, final Level level, final String format, final Object... pargs) {
        LogUtils.logUpgrade(wrapped, marker, level, format, pargs);
      }
    });
  }

  public ExecContextLogger(final Logger wrapped, final Log traceLogger) {
    this.wrapped = wrapped;
    this.traceLogger = traceLogger;
  }

  @Override
  public Logger getWrapped() {
    return wrapped;
  }

  @Override
  public String getName() {
    return wrapped.getName();
  }

  @Override
  public boolean isTraceEnabled() {
    ExecutionContext ctx = ExecutionContexts.current();
    if (ctx ==  null) {
      return wrapped.isTraceEnabled();
    }
    String name = wrapped.getName();
    Level backendOverwrite = ctx.getBackendMinLogLevel(name);
    if (backendOverwrite == null) {
      return wrapped.isTraceEnabled() || Level.TRACE.ordinal() >= ctx.getContextMinLogLevel(name).ordinal();
    } else {
      return wrapped.isTraceEnabled()
              || Level.TRACE.ordinal()
                  >= Math.min(ctx.getContextMinLogLevel(name).ordinal(), backendOverwrite.ordinal());
    }
  }

  @Override
  public void trace(final String msg) {
    ExecutionContext ctx = ExecutionContexts.current();
    if (ctx ==  null) {
      wrapped.trace(msg);
      return;
    }
    String name = wrapped.getName();
    boolean logged;
    if (wrapped.isTraceEnabled()) {
      wrapped.trace(msg, LogAttribute.traceId(ctx.getId()));
      logged = true;
    } else {
      Level backendOverwrite = ctx.getBackendMinLogLevel(name);
      if (backendOverwrite == null) {
        logged = false;
      } else if (backendOverwrite.ordinal() <= Level.TRACE.ordinal()) {
        traceLogger.log(null, Level.TRACE, msg, LogAttribute.traceId(ctx.getId()));
        logged = true;
      } else {
        logged = false;
      }
    }
    if (ctx.getContextMinLogLevel(name).ordinal() <= Level.TRACE.ordinal()) {
      ctx.addLog(new Slf4jLogRecordImpl(logged, name, Level.TRACE, (Marker) null, msg));
    }
  }

  @Override
  public void trace(final String format, final Object arg) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void trace(final String format, final Object arg1, final Object arg2) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void trace(final String format, final Object... arguments) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void trace(final String msg, final Throwable t) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean isTraceEnabled(final Marker marker) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void trace(final Marker marker, final String msg) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void trace(final Marker marker, final String format, final Object arg) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void trace(final Marker marker, final String format, final Object arg1, final Object arg2) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void trace(final Marker marker, final String format, final Object... argArray) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void trace(final Marker marker, final String msg, final Throwable t) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean isDebugEnabled() {
    ExecutionContext ctx = ExecutionContexts.current();
    if (ctx ==  null) {
      return wrapped.isDebugEnabled();
    }
    String name = wrapped.getName();
    Level backendOverwrite = ctx.getBackendMinLogLevel(name);
    if (backendOverwrite == null) {
      return wrapped.isDebugEnabled() || Level.DEBUG.ordinal() >= ctx.getContextMinLogLevel(name).ordinal();
    } else {
      return wrapped.isDebugEnabled()
              || Level.DEBUG.ordinal()
                  >= Math.min(ctx.getContextMinLogLevel(name).ordinal(), backendOverwrite.ordinal());
    }
  }

  @Override
  public void debug(final String msg) {
    ExecutionContext ctx = ExecutionContexts.current();
    if (ctx ==  null) {
      wrapped.debug(msg);
      return;
    }
    String name = wrapped.getName();
    boolean logged;
    if (wrapped.isDebugEnabled()) {
      wrapped.debug(msg, LogAttribute.traceId(ctx.getId()));
      logged = true;
    } else {
      Level backendOverwrite = ctx.getBackendMinLogLevel(name);
      if (backendOverwrite == null) {
        logged = false;
      } else if (backendOverwrite.ordinal() <= Level.DEBUG.ordinal()) {
        traceLogger.log(null, Level.DEBUG, msg, LogAttribute.traceId(ctx.getId()));
        logged = true;
      } else {
        logged = false;
      }
    }
    if (ctx.getContextMinLogLevel(name).ordinal() <= Level.DEBUG.ordinal()) {
      ctx.addLog(new Slf4jLogRecordImpl(logged, name, Level.DEBUG, (Marker) null, msg));
    }
  }

  @Override
  public void debug(final String format, final Object arg) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void debug(final String format, final Object arg1, final Object arg2) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void debug(final String format, final Object... arguments) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void debug(final String msg, final Throwable t) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean isDebugEnabled(final Marker marker) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void debug(final Marker marker, final String msg) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void debug(final Marker marker, final String format, final Object arg) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void debug(final Marker marker, final String format, final Object arg1, final Object arg2) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void debug(final Marker marker, final String format, final Object... arguments) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void debug(final Marker marker, final String msg, final Throwable t) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean isInfoEnabled() {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void info(final String msg) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void info(final String format, final Object arg) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void info(final String format, final Object arg1, final Object arg2) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void info(final String format, final Object... arguments) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void info(final String msg, final Throwable t) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean isInfoEnabled(final Marker marker) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void info(final Marker marker, final String msg) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void info(final Marker marker, final String format, final Object arg) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void info(final Marker marker, final String format, final Object arg1, final Object arg2) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void info(final Marker marker, final String format, final Object... arguments) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void info(final Marker marker, final String msg, final Throwable t) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean isWarnEnabled() {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void warn(final String msg) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void warn(final String format, final Object arg) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void warn(final String format, final Object... arguments) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void warn(final String format, final Object arg1, final Object arg2) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void warn(final String msg, final Throwable t) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean isWarnEnabled(final Marker marker) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void warn(final Marker marker, final String msg) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void warn(final Marker marker, final String format, final Object arg) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void warn(final Marker marker, final String format, final Object arg1, final Object arg2) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void warn(final Marker marker, final String format, final Object... arguments) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void warn(final Marker marker, final String msg, final Throwable t) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean isErrorEnabled() {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void error(final String msg) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void error(final String format, final Object arg) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void error(final String format, final Object arg1, final Object arg2) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void error(final String format, final Object... arguments) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void error(final String msg, final Throwable t) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean isErrorEnabled(final Marker marker) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void error(final Marker marker, final String msg) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void error(final Marker marker, final String format, final Object arg) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void error(final Marker marker, final String format, final Object arg1, final Object arg2) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void error(final Marker marker, final String format, final Object... arguments) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void error(final Marker marker, final String msg, final Throwable t) {
    throw new UnsupportedOperationException("Not supported yet.");
    //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public String toString() {
    return "ExecContextLogger{" + "wrapped=" + wrapped + '}';
  }

}
