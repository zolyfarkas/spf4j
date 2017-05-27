package org.spf4j.concurrent;

/**
 * @author zoly
 */
public class LockRuntimeException  extends RuntimeException {

  public LockRuntimeException() {
  }

  public LockRuntimeException(final String message) {
    super(message);
  }

  public LockRuntimeException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public LockRuntimeException(final Throwable cause) {
    super(cause);
  }

}
