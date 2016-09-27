package org.spf4j.io.csv;

import java.io.IOException;

/**
 *
 * @author zoly
 */
public interface CsvReader {

  enum TokenType {
    ELEMENT, END_ROW, END_DOCUMENT
  }

  /**
   * read next CSV element, and return its type.
   *
   * @return return CSV element type.
   * @throws IOException exception is something goes wrong.
   */
  TokenType next() throws IOException;

  /**
   * the CSV element string. the underlying instance is reused, so you will need to make a copy of this if planning to
   * use it.
   *
   * @return CharSequence representing a csv cell.
   */
  CharSequence getElement();

}
