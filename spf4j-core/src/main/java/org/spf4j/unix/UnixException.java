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
package org.spf4j.unix;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import javax.annotation.Nonnull;

/**
 * Internal exception thrown by native methods when error detected.
 */
public final class UnixException extends Exception {

  private static final long serialVersionUID = 1L;

  private final int errno;
  private final String msg;

  public UnixException(final String msg, final int errNo) {
    this.errno = errNo;
    this.msg = msg;
  }

  public int errno() {
    return errno;
  }

  public String errorString() {
    if (msg != null) {
      return msg;
    } else {
      return "No message available, strerror invocation not implemented yet";
    }
  }

  @Override
  public String getMessage() {
    return errorString();
  }

  /**
   * Map well known errors to specific exceptions where possible; otherwise return more general FileSystemException.
   */
  @Nonnull
  public IOException translateToIOException(final String file, final String other) {
    // created with message rather than errno
    if (msg != null) {
      return new IOException(msg);
    }

    // handle specific cases
    if (errno() == UnixConstants.EACCES) {
      return new AccessDeniedException(file, other, null);
    }
    if (errno() == UnixConstants.ENOENT) {
      return new NoSuchFileException(file, other, null);
    }
    if (errno() == UnixConstants.EEXIST) {
      return new FileAlreadyExistsException(file, other, null);
    }

    // fallback to the more general exception
    return new FileSystemException(file, other, errorString());
  }

  public void rethrowAsIOException(final String file) throws IOException {
    IOException x = translateToIOException(file, null);
    throw x;
  }

}
