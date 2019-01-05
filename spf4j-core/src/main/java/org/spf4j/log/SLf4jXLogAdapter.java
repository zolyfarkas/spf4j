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

public final class SLf4jXLogAdapter implements XLog {

  private final Logger wrapped;

  @SuppressFBWarnings("LO_SUSPECT_LOG_PARAMETER")
  public SLf4jXLogAdapter(final Logger wrapped) {
    this.wrapped = wrapped;
  }

  @Override
  @SuppressFBWarnings({"SA_LOCAL_SELF_COMPARISON", "SF_SWITCH_FALLTHROUGH"})
  public void logUpgrade(@Nullable final Marker marker, final Level level, final String format,
          final Object... pargs) {
    LogUtils.logUpgrade(wrapped, marker, level, format, pargs);
  }

  @Override
  public void log(@Nullable final Marker marker, final Level level, final String format, final Object... args) {
    switch (level) {
      case TRACE:
        if (marker != null) {
          wrapped.trace(marker, format, args);
        } else {
          wrapped.trace(format, args);
        }
        break;
      case DEBUG:
        if (marker != null) {
          wrapped.debug(marker, format, args);
        } else {
          wrapped.debug(format, args);
        }
        break;
      case INFO:
        if (marker != null) {
          wrapped.info(marker, format, args);
        } else {
          wrapped.info(format, args);
        }
        break;
      case WARN:
        if (marker != null) {
          wrapped.warn(marker, format, args);
        } else {
          wrapped.warn(format, args);
        }
        break;
      case ERROR:
        if (marker != null) {
          wrapped.error(marker, format, args);
        } else {
          wrapped.error(format, args);
        }
        break;
      default:
        throw new UnsupportedOperationException("Unsupported " + level);
    }
  }

  @Override
  public boolean isEnabled(final Level level, @Nullable final Marker marker) {
    switch (level) {
      case TRACE:
        if (marker == null) {
          return wrapped.isTraceEnabled();
        } else {
          return wrapped.isTraceEnabled(marker);
        }
      case DEBUG:
        if (marker == null) {
          return wrapped.isDebugEnabled();
        } else {
          return wrapped.isDebugEnabled(marker);
        }
      case INFO:
        if (marker == null) {
          return wrapped.isInfoEnabled();
        } else {
          return wrapped.isInfoEnabled(marker);
        }
      case WARN:
        if (marker == null) {
          return wrapped.isWarnEnabled();
        } else {
          return wrapped.isWarnEnabled(marker);
        }
      case ERROR:
        if (marker == null) {
          return wrapped.isErrorEnabled();
        } else {
          return wrapped.isErrorEnabled(null);
        }
      default:
        throw new UnsupportedOperationException("Unsupported " + level);
    }
  }

  @Override
  public Logger getWrapped() {
    return wrapped;
  }

  @Override
  public String toString() {
    return "SLf4jXLogAdapter{" + "wrapped=" + wrapped + '}';
  }

}
