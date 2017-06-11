package org.spf4j.io.csv;

/**
 * @author zoly
 */
public class CsvRuntimeException extends RuntimeException {

  public CsvRuntimeException() {
  }

  public CsvRuntimeException(final String message) {
    super(message);
  }

  public CsvRuntimeException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public CsvRuntimeException(final Throwable cause) {
    super(cause);
  }

}
