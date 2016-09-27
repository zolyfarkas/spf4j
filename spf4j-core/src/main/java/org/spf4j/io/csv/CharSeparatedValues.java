/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.spf4j.io.csv;

import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gnu.trove.map.hash.THashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.base.Strings;
import org.spf4j.io.PushbackReader;

/**
 * Supports Character Separated values format as described at: https://en.wikipedia.org/wiki/Comma-separated_values.
 * either of \n \r or \r\n generalized to custom separator character.
 * are valid end of line delimiters
 *
 * why another implementation? because I need one that is as fast as possible, and as flexible as possible.
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
@SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE") // FB gets it wrong here
public final class CharSeparatedValues {

  private final char separator;
  private final char[] toEscape;

  public CharSeparatedValues(final char separator) {
    Preconditions.checkArgument(separator != '\n' && separator != '\r' && separator != '"',
            "Illegal separator character %s", separator);
    this.separator = separator;
    this.toEscape = new char[]{separator, '\n', '\r', '"'};
  }

  public void writeCsvRow(final Appendable writer, final Object... elems) throws IOException {
    if (elems.length > 0) {
      int i = 0;
      Object elem = elems[i++];
      if (elem != null) {
        writeCsvElement(elem.toString(), writer);
      }
      while (i < elems.length) {
        writer.append(separator);
        elem = elems[i++];
        if (elem != null) {
          writeCsvElement(elem.toString(), writer);
        }
      }
    }
    writer.append('\n');
  }

  public void writeCsvRow2(final Appendable writer, final Object obj, final Object... elems)
          throws IOException {
    if (obj != null) {
      writeCsvElement(obj.toString(), writer);
    }
    for (Object elem : elems) {
      writer.append(separator);
      if (elem != null) {
        writeCsvElement(elem.toString(), writer);
      }
    }
    writer.append('\n');
  }

  public void writeCsvRow(final Appendable writer, final long... elems) throws IOException {
    writeCsvRowNoEOL(elems, writer);
    writer.append('\n');
  }

  public void writeCsvRowNoEOL(final long[] elems, final Appendable writer) throws IOException {
    if (elems.length > 0) {
      int i = 0;
      writer.append(Long.toString(elems[i++]));
      while (i < elems.length) {
        writer.append(separator);
        writer.append(Long.toString(elems[i++]));
      }
    }
  }

  public void writeCsvRow(final Appendable writer, final Iterable<?> elems) throws IOException {
    writeCsvRowNoEOL(elems, writer);
    writer.append('\n');
  }

  public void writeCsvRowNoEOL(final Iterable<?> elems, final Appendable writer) throws IOException {
    Iterator<?> it = elems.iterator();
    if (it.hasNext()) {
      Object next = it.next();
      if (next != null) {
        writeCsvElement(next.toString(), writer);
      }
      while (it.hasNext()) {
        writer.append(separator);
        next = it.next();
        if (next != null) {
          writeCsvElement(next.toString(), writer);
        }
      }
    }
  }

  public <T> T read(final File file, final Charset charset,
          final CsvMapHandler<T> handler) throws IOException {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset))) {
      return read(br, handler);
    }
  }

  public <T> T read(final File file, final Charset charset,
          final CsvHandler<T> handler) throws IOException {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset))) {
      return read(br, handler);
    }
  }

  public List<Map<String, String>> read(final Reader preader) throws IOException {
    return read(preader, new ToListMapHandler());
  }

  public <T> T read(final Reader preader,
          final CsvMapHandler<T> handler) throws IOException {
    return read(preader, new CsvMapHandler2CsvHandler<>(handler));
  }

  public List<String> readRow(final Reader reader) throws IOException {
    return readRow(reader, new CsvRow2List());
  }

  public <T> T readRow(final Reader reader, final CsvRowHandler<T> handler) throws IOException {
    return read(reader, new OneRowHandler<>(handler));
  }

  public <T> T read(final Reader preader,
          final CsvHandler<T> handler) throws IOException {
    PushbackReader reader = new PushbackReader(preader);
    int firstChar = reader.read();
    if (firstChar != UTF_BOM && firstChar >= 0) {
      reader.unread(firstChar);
    }
    return readNoBom(reader, handler);
  }

  /**
   * http://unicode.org/faq/utf_bom.html#BOM
   */
  public static final int UTF_BOM = '\uFEFF';

  /**
   * reads CSV format until EOF of reader.
   *
   * @param <T>
   * @param preader
   * @param handler
   * @return
   * @throws IOException
   */
  public <T> T readNoBom(final PushbackReader reader, final CsvHandler<T> handler) throws IOException {
    boolean start = true;
    StringBuilder strB = new StringBuilder();
    boolean loop = true;
    do {
      if (start) {
        handler.startRow();
        start = false;
      }
      strB.setLength(0);
      int c = readCsvElement(reader, strB);
      handler.element(strB);
      switch (c) {
        case '\r':
          handler.endRow();
          start = true;
          int c2 = reader.read();
          if (c2 < 0) {
            loop = false;
            break;
          }
          if (c2 != '\n') {
            reader.unread(c2);
          }
          break;
        case '\n':
          handler.endRow();
          start = true;
          break;
        default:
          if (c != separator) {
            if (c < 0) {
              loop = false;
            } else {
              throw new IOException("Unexpected character " + c);
            }
          }
      }
    } while (loop);
    out:
    return handler.eof();
  }

  /**
   * read a CSV stream, as a Iterable over rows.
   * the List<String> instance is reused during iteration, you will need to copy content into
   * own data structure.
   * @param preader
   * @return
   */
  public  Iterable<List<String>> asIterable(final Reader preader) {
    return () -> {
      try {
        return new CsvReader2Iterator(reader(preader));
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    };

  }


  public CsvReader reader(final Reader preader) throws IOException {
    PushbackReader reader = new PushbackReader(preader);
    int firstChar = reader.read();
    if (firstChar != UTF_BOM && firstChar >= 0) {
      reader.unread(firstChar);
    }
    return readerNoBOM(reader);
  }

  public CsvReader readerNoBOM(final PushbackReader reader) {
    return new CsvReaderImpl(reader);
  }



  public void writeCsvElement(final CharSequence elem, final Appendable writer) throws IOException {
    if (Strings.contains(elem, toEscape)) {
      writeQuotedCsvElement(elem, writer);
    } else {
      writer.append(elem);
    }
  }

  public static void writeQuotedCsvElement(final CharSequence elem, final Appendable writer) throws IOException {
    int length = elem.length();
    writer.append('"');
    for (int i = 0; i < length; i++) {
      char c = elem.charAt(i);
      if (c == '"') {
        writer.append("\"\"");
      } else {
        writer.append(c);
      }
    }
    writer.append('"');
  }

  public CharSequence toCsvElement(final CharSequence elem) {
    if (Strings.contains(elem, toEscape)) {
      StringWriter sw = new StringWriter(elem.length() - 1);
      try {
        writeQuotedCsvElement(elem, sw);
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
      return sw.toString();
    } else {
      return elem;
    }
  }

  /**
   * returns next character.
   *
   * @param reader
   * @param addElemTo
   * @return - next character or -1 if eof has been reached.
   * @throws IOException
   */
  @CheckReturnValue
  public int readCsvElement(final Reader reader, final StringBuilder addElemTo) throws IOException {
    int c = reader.read();
    if (c < 0) {
      return c;
    }
    if (c == '"') {
      c = reader.read();
      while (c >= 0) {
        if (c == '"') {
          int c2 = reader.read();
          if (c2 >= 0) {
            if (c2 == '"') {
              addElemTo.append((char) c);
            } else {
              return c2;
            }
          } else {
            return c2;
          }
        } else {
          addElemTo.append((char) c);
        }
        c = reader.read();
      }
    } else {
      while (c != separator && c != '\n' && c != '\r' && c >= 0) {
        addElemTo.append((char) c);
        c = reader.read();
      }
    }
    return c;
  }

  @Override
  public String toString() {
    return "CharSepValues{" + "separator=" + separator + '}';
  }

  private static class ToListMapHandler implements CsvMapHandler<List<Map<String, String>>> {

    private List<Map<String, String>> result = new ArrayList<>();

    @Override
    public void row(final Map<String, String> row) {
      result.add(row);
    }

    @Override
    public List<Map<String, String>> eof() {
      return result;
    }
  }

  private static class CsvMapHandler2CsvHandler<T> implements CsvHandler<T> {

    private final CsvMapHandler<T> handler;

    CsvMapHandler2CsvHandler(final CsvMapHandler<T> handler) {
      this.handler = handler;
    }
    private boolean first = true;
    private final List<String> header = new ArrayList<>();
    private int elemIdx;
    private Map<String, String> row = null;

    @Override
    public void startRow() {
      elemIdx = 0;
      if (!first) {
        row = new THashMap<>(header.size());
      }
    }

    @Override
    public void element(final CharSequence elem) {
      if (first) {
        header.add(elem.toString());
      } else {
        row.put(header.get(elemIdx), elem.toString());
      }
      elemIdx++;
    }

    @Override
    public void endRow() {
      if (first) {
        first = false;
      } else {
        handler.row(row);
      }
    }

    @Override
    public T eof() {
      return handler.eof();
    }
  }

  private class CsvReaderImpl implements CsvReader {

    private final PushbackReader reader;

    CsvReaderImpl(final PushbackReader reader) {
      this.reader = reader;
    }
    private final StringBuilder currentElement = new StringBuilder();
    private TokenType currentToken;
    private TokenType nextToken;

    private void readCurrentElement() throws IOException {
      currentElement.setLength(0);
      int next = readCsvElement(reader, currentElement);
      currentToken = TokenType.ELEMENT;
      switch (next) {
        case '\r':
          int c2 = reader.read();
          if (c2 < 0) {
            nextToken = TokenType.END_DOCUMENT;
            break;
          }
          if (c2 != '\n') {
            reader.unread(c2);
          }
          nextToken = TokenType.END_ROW;
          break;
        case '\n':
          nextToken = TokenType.END_ROW;
          break;
        default:
          if (next != separator) {
            if (next < 0) {
              nextToken = TokenType.END_DOCUMENT;
            } else {
              throw new IOException("Unexpected character " + next);
            }
          }
      }

    }

    @Override
    public TokenType next() throws IOException {
      if (currentToken == null) {
        if (nextToken == null) {
          readCurrentElement();
          TokenType result = currentToken;
          if (result != TokenType.END_DOCUMENT) {
            currentToken = null;
          }
          return result;
        } else {
          TokenType result = nextToken;
          if (result != TokenType.END_DOCUMENT) {
            nextToken = null;
          }
          return result;
        }
      } else {
        return currentToken;
      }
    }

    @Override
    public CharSequence getElement() {
      return currentElement;
    }
  }

  private static class OneRowHandler<T> implements CsvHandler<T> {

    private final CsvRowHandler<T> handler;

    private int nrRows;

    OneRowHandler(final CsvRowHandler<T> handler) {
      this.handler = handler;
      this.nrRows = 0;
    }

    @Override
    public void startRow() {
      if (nrRows > 0) {
        throw new IllegalStateException("Multiple rows encountered for " + this);
      }
    }

    @Override
    public void element(final CharSequence elem) {
      handler.element(elem);
    }

    @Override
    public void endRow() {
      nrRows++;
    }

    @Override
    public T eof() {
      return handler.eof();
    }
  }

  private static final class CsvRow2List implements CsvRowHandler<List<String>> {

    private final List<String> result = new ArrayList<>();

    @Override
    public void element(final CharSequence elem) {
      result.add(elem.toString());
    }

    @Override
    public List<String> eof() {
      return result;
    }
  }

}
