
package org.spf4j.io.csv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public final class CsvReader2Iterator implements Iterator<Iterable<String>> {

  private final CsvReader reader;

  private final List<String> row;

  private boolean haveParsedRow;

  public CsvReader2Iterator(final CsvReader preader) {
    this.reader = preader;
    haveParsedRow = false;
    row = new ArrayList<>();
  }

  private CsvReader.TokenType readRow() throws IOException, CsvParseException {
    row.clear();
    CsvReader.TokenType token;
    boolean loop = true;
    do {
      token = reader.next();
      switch (token) {
        case ELEMENT:
          row.add(reader.getElement().toString());
          break;
        case END_DOCUMENT:
        case END_ROW:
          loop = false;
          break;
        default:
          throw new IllegalStateException("Illegal token " + token);
      }
    } while (loop);
    haveParsedRow = !row.isEmpty();
    return token;
  }

  @Override
  public boolean hasNext() {
    if (!haveParsedRow) {
      try {
        CsvReader.TokenType token = readRow();
        if (haveParsedRow) {
          return true;
        } else {
          return token != CsvReader.TokenType.END_DOCUMENT;
        }
      } catch (IOException | CsvParseException ex) {
        throw new RuntimeException(ex);
      }
    } else {
      return true;
    }
  }

  @Override
  public Iterable<String> next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    } else {
      haveParsedRow = false;
      return row;
    }
  }

  @Override
  public String toString() {
    return "CsvReader2Iterator{" + "reader=" + reader + ", row=" + row + ", haveParsedRow=" + haveParsedRow + '}';
  }


}
