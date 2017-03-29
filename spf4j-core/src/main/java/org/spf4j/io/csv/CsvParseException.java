
package org.spf4j.io.csv;

/**
 * @author zoly
 */
public class CsvParseException extends RuntimeException {

  public CsvParseException() {
  }

  public CsvParseException(final String message) {
    super(message);
  }

  public CsvParseException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public CsvParseException(final Throwable cause) {
    super(cause);
  }


}
