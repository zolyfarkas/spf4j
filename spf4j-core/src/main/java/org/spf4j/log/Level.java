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
import org.spf4j.base.avro.LogLevel;


/**
 * An enum for log levels
 * @author Zoltan Farkas
 */
public enum Level {
  TRACE(java.util.logging.Level.FINEST, 'T'),
  DEBUG(java.util.logging.Level.FINE, 'D'),
  INFO(java.util.logging.Level.INFO, 'I'),
  WARN(java.util.logging.Level.WARNING, 'W'),
  ERROR(java.util.logging.Level.SEVERE, 'E'),
  OFF(java.util.logging.Level.OFF, 'O');


  static final class Lazy {
    private static final Map<Level, LogLevel> CONVERT_MAP = new EnumMap<>(Level.class);
    static {
      CONVERT_MAP.put(TRACE, LogLevel.TRACE);
      CONVERT_MAP.put(DEBUG, LogLevel.DEBUG);
      CONVERT_MAP.put(INFO, LogLevel.INFO);
      CONVERT_MAP.put(WARN, LogLevel.WARN);
      CONVERT_MAP.put(ERROR, LogLevel.ERROR);
      CONVERT_MAP.put(OFF, LogLevel.UNKNOWN);
    }
  }

  private final java.util.logging.Level julLevel;

  private final char charRepresentation;

  Level(final java.util.logging.Level julLevel, final char charRepresentation) {
    this.julLevel = julLevel;
    this.charRepresentation = charRepresentation;
  }

  public int getIntValue() {
    return julLevel.intValue();
  }

  public java.util.logging.Level getJulLevel() {
    return julLevel;
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

