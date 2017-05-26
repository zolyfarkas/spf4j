package org.spf4j.concurrent;

/**
 * @author zoly
 */
public class LockIOException  extends RuntimeException {

  public LockIOException() {
  }

  public LockIOException(final String message) {
    super(message);
  }

  public LockIOException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public LockIOException(final Throwable cause) {
    super(cause);
  }

}
