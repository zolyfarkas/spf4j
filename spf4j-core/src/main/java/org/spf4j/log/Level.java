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

import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.spf4j.base.avro.LogLevel;

/**
 * An enum for log levels
 *
 * @author Zoltan Farkas
 */
public enum Level {
  TRACE(java.util.logging.Level.FINEST, org.slf4j.event.Level.TRACE, 'T'),
  DEBUG(java.util.logging.Level.FINE, org.slf4j.event.Level.DEBUG, 'D'),
  INFO(java.util.logging.Level.INFO, org.slf4j.event.Level.INFO, 'I'),
  WARN(java.util.logging.Level.WARNING, org.slf4j.event.Level.WARN, 'W'),
  ERROR(java.util.logging.Level.SEVERE, org.slf4j.event.Level.ERROR, 'E'),
  OFF(java.util.logging.Level.OFF, null, 'O');

  static final class Lazy {

    private static final Map<Level, LogLevel> CONVERT_MAP = new EnumMap<>(Level.class);
    private static final Map<org.slf4j.event.Level, Level> CONVERT2_MAP = new EnumMap<>(org.slf4j.event.Level.class);

    static {
      CONVERT_MAP.put(TRACE, LogLevel.TRACE);
      CONVERT_MAP.put(DEBUG, LogLevel.DEBUG);
      CONVERT_MAP.put(INFO, LogLevel.INFO);
      CONVERT_MAP.put(WARN, LogLevel.WARN);
      CONVERT_MAP.put(ERROR, LogLevel.ERROR);
      CONVERT_MAP.put(OFF, LogLevel.UNKNOWN);
      for (Level level : Level.values()) {
        org.slf4j.event.Level slf4jLevel = level.getSlf4jLevel();
        if (slf4jLevel != null) {
          CONVERT2_MAP.put(slf4jLevel, level);
        }
      }
    }
  }

  public static Level valueOf(@Nullable final org.slf4j.event.Level level) {
    if (level == null) {
      return Level.OFF;
    }
    return Lazy.CONVERT2_MAP.get(level);
  }

  private final java.util.logging.Level julLevel;

  private final org.slf4j.event.Level slf4jLevel;

  private final char charRepresentation;

  Level(final java.util.logging.Level julLevel, final org.slf4j.event.Level slf4jLevel,
          final char charRepresentation) {
    this.julLevel = julLevel;
    this.slf4jLevel = slf4jLevel;
    this.charRepresentation = charRepresentation;
  }

  public int getIntValue() {
    return julLevel.intValue();
  }

  public java.util.logging.Level getJulLevel() {
    return julLevel;
  }

  @Nullable
  public org.slf4j.event.Level getSlf4jLevel() {
    return slf4jLevel;
  }

  public LogLevel getAvroLevel() {
    return Lazy.CONVERT_MAP.get(this);
  }

  public static Level fromAvroLevel(final LogLevel level) {
    switch (level) {
      case UNKNOWN:
      case DEBUG:
        return Level.DEBUG;
      case ERROR:
        return Level.ERROR;
      case INFO:
        return Level.INFO;
      case TRACE:
        return Level.TRACE;
      case WARN:
        return Level.WARN;
      default:
        throw new IllegalArgumentException("Unsupported LogLevel " + level);
    }
  }

  public static Level fromJulLevel(final int severity) {
    if (severity <= TRACE.getIntValue()) {
      return TRACE;
    } else if (severity <= DEBUG.getIntValue()) {
      return DEBUG;
    } else if (severity <= INFO.getIntValue()) {
      return INFO;
    } else if (severity <= WARN.getIntValue()) {
      return WARN;
    } else if (severity <= ERROR.getIntValue()) {
      return ERROR;
    } else {
      return OFF;
    }
  }

  public char toCharRepresentation() {
    return charRepresentation;
  }

  public static Level valueOf(final char c) {
    switch (c) {
      case 'E':
        return Level.ERROR;
      case 'W':
        return Level.WARN;
      case 'I':
        return Level.INFO;
      case 'D':
        return Level.DEBUG;
      case 'T':
        return Level.TRACE;
      case 'O':
        return Level.OFF;
      default:
        throw new IllegalArgumentException("Unsupported Log Level Character: " + c);
    }
  }

}
