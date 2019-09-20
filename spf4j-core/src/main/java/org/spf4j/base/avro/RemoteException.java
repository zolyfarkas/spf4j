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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.io.csv.CharSeparatedValues;

/**
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
public class RemoteException extends RuntimeException {

  private final String source;

  private final Throwable remote;

  public RemoteException(final String source, final Throwable remote) {
    super(remote.getClassName() + '@' + source + ": " + remote.getMessage(),
            remote.getCause() == null ? null : new RemoteException(source, remote.getCause()));
    this.remote = remote;
    this.source = source;
    for (Throwable suppressed : remote.getSuppressed()) {
      addSuppressed(new RemoteException(source, suppressed));
    }
    List<StackTraceElement> stackTrace = remote.getStackTrace();
    java.lang.StackTraceElement[] jste = new java.lang.StackTraceElement[stackTrace.size()];
    int i = 0;
    for (StackTraceElement ste : stackTrace) {
      PackageInfo packageInfo = ste.getPackageInfo();
      FileLocation location = ste.getLocation();
      Method method = ste.getMethod();
      if (location == null) {
       jste[i] = new java.lang.StackTraceElement(method.getDeclaringClass(), method.getName(),
               encodeRemotePackageName(packageInfo, "N/A"), -1);
      } else  {
       jste[i] = new java.lang.StackTraceElement(method.getDeclaringClass(), method.getName(),
               encodeRemotePackageName(packageInfo, location.getFileName()), location.getLineNumber());
      }
      i++;
    }
    this.setStackTrace(jste);
  }

  private static final  CharSeparatedValues CSV = new CharSeparatedValues(':');

  public static final String encodeRemotePackageName(@Nullable final PackageInfo pInfo, final String fileName) {
    StringBuilder result = new StringBuilder(80);
    try {
      if (pInfo == null) {
        CSV.writeCsvRow(result, "remote", "N/A", "N/A", fileName);
      } else {
        CSV.writeCsvRow(result, "remote", pInfo.getUrl(), pInfo.getVersion(), fileName);
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return result.toString();
  }




  public final Throwable getRemoteCause() {
    return remote;
  }

  public final String getSource() {
    return source;
  }

  /**
   * Override to make up the remote stack trace.
   * @return
   */
  @Override
  public synchronized java.lang.Throwable fillInStackTrace() {
    // no need to do anything we will overwrite this.
    return this;
  }



}
