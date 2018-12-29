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
package org.spf4j.base.avro;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import org.slf4j.Marker;
import org.spf4j.log.Slf4jLogRecord;

/**
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
public final class Converters {

  private Converters() { }

  public static StackTraceElement convert(final java.lang.StackTraceElement stackTrace) {
    String className = stackTrace.getClassName();
    StackTraceElement.Builder builder = StackTraceElement.newBuilder()
            .setClassName(className)
            .setMethodName(stackTrace.getMethodName());
    String fileName = stackTrace.getFileName();
    if (fileName != null) {
      builder.setLocation(new FileLocation(fileName, stackTrace.getLineNumber(), -1));
    }
    builder.setPackageInfo(org.spf4j.base.PackageInfo.getPackageInfo(className));
    return builder.build();
  }

  public static List<StackTraceElement> convert(final java.lang.StackTraceElement[] stackTraces) {
    int l = stackTraces.length;
    if (l == 0) {
      return Collections.EMPTY_LIST;
    }
    List<StackTraceElement> result = new ArrayList<>(l);
    for (java.lang.StackTraceElement st : stackTraces) {
      result.add(convert(st));
    }
    return result;
  }

  public static List<Throwable> convert(final java.lang.Throwable[] throwables) {
    int l = throwables.length;
    if (l == 0) {
      return Collections.EMPTY_LIST;
    }
    List<Throwable> result = new ArrayList<>(l);
    for (java.lang.Throwable t : throwables) {
      result.add(convert(t));
    }
    return result;
  }

  public static Throwable convert(final java.lang.Throwable throwable) {
    String message = throwable.getMessage();
    if (throwable instanceof RemoteException) {
          return Throwable.newBuilder()
            .setClassName(throwable.getClass().getName())
            .setMessage(message == null ? "" : message)
            .setStackTrace(convert(throwable.getStackTrace()))
            .setSuppressed(convert(throwable.getSuppressed()))
            .setCause(((RemoteException) throwable).getRemoteCause())
            .build();
    }
    java.lang.Throwable cause = throwable.getCause();
    return Throwable.newBuilder()
            .setClassName(throwable.getClass().getName())
            .setMessage(message == null ? "" : message)
            .setCause(cause == null ? null : convert(cause))
            .setStackTrace(convert(throwable.getStackTrace()))
            .setSuppressed(convert(throwable.getSuppressed()))
            .build();
  }

  public static RemoteException convert(final String source, final Throwable throwable) {
    return new RemoteException(source, throwable);
  }

  public static LogRecord convert(final String origin, final String traceId, final Slf4jLogRecord logRecord) {
    java.lang.Throwable extraThrowable = logRecord.getExtraThrowable();
    Marker marker = logRecord.getMarker();
    Object[] extraArguments = logRecord.getExtraArguments();
    List<Object> xArgs;
    if (marker == null) {
      xArgs = extraArguments.length == 0 ? Collections.EMPTY_LIST : Arrays.asList(logRecord.getExtraArguments());
    } else {
      if (extraArguments.length == 0) {
        xArgs = Collections.singletonList(marker);
      } else {
        xArgs = new ArrayList<>(extraArguments.length + 1);
        xArgs.add(marker);
        for (Object obj : extraArguments) {
          xArgs.add(obj);
        }
      }
    }
    return new LogRecord(origin, traceId,  logRecord.getLevel().getAvroLevel(),
            Instant.ofEpochMilli(logRecord.getTimeStamp()),
    logRecord.getLoggerName(), logRecord.getThreadName(), logRecord.getMessage(),
    extraThrowable == null ? null : convert(extraThrowable), xArgs);
  }

  public static List<LogRecord> convert(final String origin, final String traceId,
          final List<Slf4jLogRecord> logRecords) {
    List<LogRecord> result = new ArrayList<>(logRecords.size());
    for (Slf4jLogRecord log : logRecords) {
      result.add(convert(origin, traceId, log));
    }
    return result;
  }

}
