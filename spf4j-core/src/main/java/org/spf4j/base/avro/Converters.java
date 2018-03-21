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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.base.PackageInfo;

/**
 *
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
public final class Converters {

  private Converters() { }

  public static JStackTraceElement convert(final StackTraceElement stackTrace) {
    String className = stackTrace.getClassName();
    JStackTraceElement.Builder builder = JStackTraceElement.newBuilder()
            .setClassName(className)
            .setMethodName(stackTrace.getMethodName());
    String fileName = stackTrace.getFileName();
    if (fileName != null) {
      builder.setLocation(new JFileLocation(fileName, stackTrace.getLineNumber()));
    }
    PackageInfo packageInfo = PackageInfo.getPackageInfo(className);
    if (!packageInfo.equals(PackageInfo.NONE)) {
      builder.setPackageInfo(new JPackageInfo(packageInfo.getUrl(), packageInfo.getVersion()));
    }
    return builder.build();
  }

  public static List<JStackTraceElement> convert(final StackTraceElement[] stackTraces) {
    int l = stackTraces.length;
    if (l == 0) {
      return Collections.EMPTY_LIST;
    }
    List<JStackTraceElement> result = new ArrayList<>(l);
    for (StackTraceElement st : stackTraces) {
      result.add(convert(st));
    }
    return result;
  }

  public static List<JThrowable> convert(final Throwable[] throwables) {
    int l = throwables.length;
    if (l == 0) {
      return Collections.EMPTY_LIST;
    }
    List<JThrowable> result = new ArrayList<>(l);
    for (Throwable t : throwables) {
      result.add(convert(t));
    }
    return result;
  }

  public static JThrowable convert(final Throwable throwable) {
    if (throwable instanceof RemoteException) {
          return JThrowable.newBuilder()
            .setClassName(throwable.getClass().getName())
            .setMessage(throwable.getMessage())
            .setStackTrace(convert(throwable.getStackTrace()))
            .setSuppressed(convert(throwable.getSuppressed()))
            .setCause(((RemoteException) throwable).getRemoteCause())
            .build();
    }
    Throwable cause = throwable.getCause();
    return JThrowable.newBuilder()
            .setClassName(throwable.getClass().getName())
            .setMessage(throwable.getMessage())
            .setCause(cause == null ? null : convert(cause))
            .setStackTrace(convert(throwable.getStackTrace()))
            .setSuppressed(convert(throwable.getSuppressed()))
            .build();
  }

  public static RemoteException convert(final String source, final JThrowable throwable) {
    return new RemoteException(source, throwable);
  }

}
