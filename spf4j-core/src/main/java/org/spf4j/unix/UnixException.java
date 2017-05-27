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
