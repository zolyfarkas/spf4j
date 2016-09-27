
package org.spf4j.io.csv;

/**
 *
 * @author zoly
 */
public interface CsvHandler<T> {

  void startRow();

  /**
   * @param elem - the CharSequence instance is being reused, between invocations. value should be copied or parsed into
   * a new object.
   */
  void element(CharSequence elem);

  void endRow();

  T eof();
}
