
package org.spf4j.stackmonitor;

/**
 * @author zoly
 */
public class SamplerException extends RuntimeException {

  public SamplerException() {
  }

  public SamplerException(final String message) {
    super(message);
  }

  public SamplerException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public SamplerException(final Throwable cause) {
    super(cause);
  }


}
