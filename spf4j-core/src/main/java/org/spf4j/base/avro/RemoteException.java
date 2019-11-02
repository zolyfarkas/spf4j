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

import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
public class RemoteException extends org.spf4j.base.RemoteException {

  public RemoteException(final String source, final Throwable remote) {
    super(remote.getMessage(),
            source, remote, remote.getCause() == null ? null : new RemoteException(source, remote.getCause()));
    for (Throwable suppressed : remote.getSuppressed()) {
      addSuppressed(new RemoteException(source, suppressed));
    }
    List<StackTraceElement> stackTrace = remote.getStackTrace();
    java.lang.StackTraceElement[] jste = new java.lang.StackTraceElement[stackTrace.size()];
    int i = 0;
    for (StackTraceElement ste : stackTrace) {
      FileLocation location = ste.getLocation();
      Method method = ste.getMethod();
      String declaringClass = method.getDeclaringClass();
      if (!declaringClass.startsWith("remote.")) { // add this to protect from reflectiion use.
        declaringClass = "remote." + declaringClass;
      }
      if (location == null) {
       jste[i] = new java.lang.StackTraceElement(declaringClass, method.getName(),
               "N/A", -1);
      } else  {
       jste[i] = new java.lang.StackTraceElement(declaringClass, method.getName(),
               location.getFileName(), location.getLineNumber());
      }
      i++;
    }
    this.setStackTrace(jste);
  }

  public final Throwable getRemoteCause() {
    return (Throwable) getRemoteDetail();
  }

  public final String getSource() {
    return getOrigin();
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

  public String toString() {
    String s = "RemoteException:" + this.getRemoteCause().getClassName();
    String message = getLocalizedMessage();
    return (message != null) ? (s + ": " + message) : s;
  }

}
