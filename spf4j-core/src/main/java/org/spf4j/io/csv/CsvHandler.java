
package org.spf4j.io.csv;

/**
 *
 * @author zoly
 */
public interface CsvHandler<T> {

  default void startRow(int rowNr) {
    startRow();
  }

  default void startRow() {
    // do nothing by default.
  }

  /**
   * @param elem - the CharSequence instance is being reused, between invocations. value should be copied or parsed into
   * a new object.
   */
  void element(CharSequence elem) throws CsvParseException;

  default void endRow() throws CsvParseException {
    // do nothing by default.
  }

  T eof();
}
