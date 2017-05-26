
package org.spf4j.base;

/**
 * @author zoly
 */
public class CloneFailedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public CloneFailedException() {
  }

  public CloneFailedException(final String message) {
    super(message);
  }

  public CloneFailedException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public CloneFailedException(final Throwable cause) {
    super(cause);
  }

}
