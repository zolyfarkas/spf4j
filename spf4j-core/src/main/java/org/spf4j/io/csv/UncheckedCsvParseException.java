package org.spf4j.io.csv;

/**
 * @author zoly
 */
public class UncheckedCsvParseException extends RuntimeException {

  public UncheckedCsvParseException() {
  }

  public UncheckedCsvParseException(final String message) {
    super(message);
  }

  public UncheckedCsvParseException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public UncheckedCsvParseException(final Throwable cause) {
    super(cause);
  }

}
