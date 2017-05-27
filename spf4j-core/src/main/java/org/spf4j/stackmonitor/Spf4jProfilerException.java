
package org.spf4j.stackmonitor;

/**
 * @author zoly
 */
public class Spf4jProfilerException extends RuntimeException {

  public Spf4jProfilerException() {
  }

  public Spf4jProfilerException(final String message) {
    super(message);
  }

  public Spf4jProfilerException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public Spf4jProfilerException(final Throwable cause) {
    super(cause);
  }


}
