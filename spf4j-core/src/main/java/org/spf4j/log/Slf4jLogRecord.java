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
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Marker;

/**
 *
 * @author Zoltan Farkas
 */
public interface Slf4jLogRecord {

  @SuppressFBWarnings(value = "EI_EXPOSE_REP")
  @Nonnull
  Object[] getArguments();

  /**
   * @return all extra arguments. (arguments that are not parameters for the message)
   */
  @Nonnull
  Object[] getExtraArgumentsRaw();

  /**
   * @return all non Throwable extra arguments.
   */
  @Nonnull
  Object[] getExtraArguments();

  /**
   * @return Throwable from extra arguments. If multiple, will return first will all others as suppressed.
   */
  @Nullable
  Throwable getExtraThrowable();

  Level getLevel();

  String getLoggerName();

  @Nullable
  Marker getMarker();

  @Nonnull
  String getMessage();

  String getMessageFormat();

  int getNrMessageArguments();

  String getThreadName();

  long getTimeStamp();

  /**
   * Indicates that this log record has been sent to the logging backend to persist.
   * @return
   */
  boolean isLogged();

  void setIsLogged();

  void attach(Object obj);

  Set<Object> getAttachments();

  boolean hasAttachment(Object obj);

  static int compareByTimestamp(Slf4jLogRecord a, Slf4jLogRecord b) {
    long timeDiff = a.getTimeStamp() - b.getTimeStamp();
    if (timeDiff > 0) {
      return 1;
    } else if (timeDiff < 0) {
      return -1;
    } else {
      return 0;
    }
  }

}
