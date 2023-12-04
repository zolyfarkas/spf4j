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
import org.slf4j.spi.LoggingEventBuilder;

/**
 *
 * @author Zoltan Farkas
 */
public final class LogUtils {

  private LogUtils() {
  }
  
  @Nullable
  public static org.slf4j.event.Level getLowestEnabledLevel(final Logger log, @Nullable final Marker marker) {
    if (marker == null) {
      if (log.isTraceEnabled()) {
        return org.slf4j.event.Level.TRACE;
      }
      if (log.isDebugEnabled()) {
        return org.slf4j.event.Level.DEBUG;
      }
      if (log.isInfoEnabled()) {
        return org.slf4j.event.Level.INFO;
      }
      if (log.isWarnEnabled()) {
        return org.slf4j.event.Level.WARN;
      }
      if (log.isErrorEnabled()) {
        return org.slf4j.event.Level.ERROR;
      }
    } else {
      if (log.isTraceEnabled(marker)) {
        return org.slf4j.event.Level.TRACE;
      }
      if (log.isDebugEnabled(marker)) {
        return org.slf4j.event.Level.DEBUG;
      }
      if (log.isInfoEnabled(marker)) {
        return org.slf4j.event.Level.INFO;
      }
      if (log.isWarnEnabled(marker)) {
        return org.slf4j.event.Level.WARN;
      }
      if (log.isErrorEnabled(marker)) {
        return org.slf4j.event.Level.ERROR;
      }
    }
    return null;
  }

  @SuppressFBWarnings({"SA_LOCAL_SELF_COMPARISON", "SF_SWITCH_FALLTHROUGH"})
  public static void logUpgrade(final Logger log, @Nullable final Marker marker, final Level level,
          final String format, final Object... pargs) {
    org.slf4j.event.Level slf4jLevel = level.getSlf4jLevel();
    if (slf4jLevel == null) {
      return;
    }
    org.slf4j.event.Level lowestEnabledLevel = getLowestEnabledLevel(log, marker);
    if (lowestEnabledLevel == null) {
      return;
    }
    org.slf4j.event.Level logAt =  lowestEnabledLevel.toInt() > slf4jLevel.toInt() ? lowestEnabledLevel : slf4jLevel;
    LoggingEventBuilder builder = log.atLevel(logAt);
    if (marker != null) {
      builder = builder.addMarker(marker);
    }
    if (logAt != slf4jLevel) {
      builder = builder.addKeyValue("origLevel", slf4jLevel);
    }
    builder.log(format, pargs);
  }

  @SuppressFBWarnings({"SA_LOCAL_SELF_COMPARISON", "SF_SWITCH_FALLTHROUGH"})
  public static void logUpgrade(final java.util.logging.Logger log, final Level plevel,
          final String format, final Object... pargs) {
    Level[] values = Level.values();
    Level level = plevel;
    while (true) {
      java.util.logging.Level julLevel = level.getJulLevel();
      if (log.isLoggable(julLevel)) {
        log.log(julLevel, format, pargs);
        return;
      }
      if (level == Level.ERROR) {
        throw new IllegalStateException();
      }
      level = values[level.ordinal() + 1];
    }
  }
}
