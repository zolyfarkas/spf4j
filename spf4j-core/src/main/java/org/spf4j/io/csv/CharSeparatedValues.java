/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
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
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.io.csv;

import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gnu.trove.map.hash.THashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.base.CharSequences;
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

  /**
   * http://unicode.org/faq/utf_bom.html#BOM
   */
  public static final int UTF_BOM = '\uFEFF';

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
          final CsvMapHandler<T> handler) throws IOException, CsvParseException {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), charset))) {
      return read(br, handler);
    }
  }

  public <T> T read(final File file, final Charset charset,
          final CsvHandler<T> handler) throws IOException, CsvParseException {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), charset))) {
      return read(br, handler);
    }
  }

  public List<Map<String, String>> read(final Reader preader) throws IOException, CsvParseException {
    return read(preader, new ToListMapHandler());
  }

  public <T> T read(final Reader preader,
          final CsvMapHandler<T> handler) throws IOException, CsvParseException {
    return read(preader, new CsvMapHandler2CsvHandler<>(handler));
  }

  public List<String> readRow(final Reader reader) throws IOException, CsvParseException {
    return readRow(reader, new CsvRow2List());
  }

  public <T> T readRow(final Reader reader, final CsvRowHandler<T> handler) throws IOException, CsvParseException {
    return read(reader, new OneRowHandler<>(handler));
  }

  public <T> T read(final Reader preader,
          final CsvHandler<T> handler) throws IOException, CsvParseException {
    PushbackReader reader = new PushbackReader(preader);
    int firstChar = reader.read();
    if (firstChar != UTF_BOM && firstChar >= 0) {
      reader.unread(firstChar);
    }
    return readNoBom(reader, handler);
  }

  /**
   * reads CSV format until EOF of reader.
   *
   * @param <T>
   * @param preader
   * @param handler
   * @return
   * @throws IOException
   */
  public <T> T readNoBom(final PushbackReader reader, final CsvHandler<T> handler)
          throws IOException, CsvParseException {
    boolean start = true;
    StringBuilder strB = new StringBuilder();
    boolean loop = true;
    int lineNr = 0;
    try {
      do {
        if (start) {
          handler.startRow(lineNr);
          start = false;
        }
        strB.setLength(0);
        int c = readCsvElement(reader, strB, lineNr);
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
            lineNr++;
            handler.endRow();
            start = true;
            break;
          default:
            if (c != separator) {
              if (c < 0) {
                loop = false;
              } else {
                throw new CsvParseException("Unexpected character " + c + " at line " + lineNr);
              }
            }
        }
      } while (loop);
    } catch (IOException ex) {
      throw new IOException("IO issue at line " + lineNr, ex);
    } catch (RuntimeException ex) {
      throw new CsvRuntimeException("Exception at line " + lineNr, ex);
    }
    handler.endRow();
    return handler.eof();
  }

  /**
   * read a CSV stream, as a Iterable over rows.
   * the List<String> instance is reused during iteration, you will need to copy content into
   * own data structure.
   * @param preader
   * @return
   */
  public  Iterable<Iterable<String>> asIterable(final Reader preader) {
    return () -> {
      try {
        return new CsvReader2Iterator(reader(preader));
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
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
    if (CharSequences.containsAnyChar(elem, toEscape)) {
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
    if (CharSequences.containsAnyChar(elem, toEscape)) {
      StringWriter sw = new StringWriter(elem.length() - 1);
      try {
        writeQuotedCsvElement(elem, sw);
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
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
  public int readCsvElement(final Reader reader, final StringBuilder addElemTo, final int lineNr)
          throws IOException, CsvParseException {
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
      throw new CsvParseException("Escaped CSV element " + addElemTo + " not terminated correctly at " + lineNr);
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
    private boolean first = true;
    private final List<String> header = new ArrayList<>();
    private int elemIdx;
    private Map<String, String> row = null;
    private int lineNr;


    CsvMapHandler2CsvHandler(final CsvMapHandler<T> handler) {
      this.handler = handler;
    }

    @Override
    public void startRow(final int ln) {
      lineNr = ln;
      elemIdx = 0;
      if (!first) {
        row = new THashMap<>(header.size());
      }
    }

    @Override
    public void element(final CharSequence elem) throws CsvParseException {
      if (first) {
        header.add(elem.toString());
      } else {
        if (header.size() <= elemIdx) {
          throw new CsvParseException("Too many elements in row " + row + " at line " + lineNr);
        }
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
    private final StringBuilder currentElement = new StringBuilder();
    private TokenType currentToken;
    private TokenType nextToken;
    private int lineNr = 0;

    CsvReaderImpl(final PushbackReader reader) {
      this.reader = reader;
    }

    private void readCurrentElement() throws IOException, CsvParseException {
      currentElement.setLength(0);
      int next = readCsvElement(reader, currentElement, lineNr);
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
          lineNr++;
          nextToken = TokenType.END_ROW;
          break;
        case '\n':
          lineNr++;
          nextToken = TokenType.END_ROW;
          break;
        default:
          if (next != separator) {
            if (next < 0) {
              nextToken = TokenType.END_DOCUMENT;
            } else {
              throw new CsvParseException("Unexpected character " + next + " at line" + lineNr);
            }
          }
      }

    }

    @Override
    public TokenType next() throws IOException, CsvParseException {
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


    OneRowHandler(final CsvRowHandler<T> handler) {
      this.handler = handler;
    }

    @Override
    public void startRow(final int rowNr) {
      if (rowNr > 0) {
        throw new IllegalArgumentException("Multiple rows encountered for " + this);
      }
    }

    @Override
    public void element(final CharSequence elem) {
      handler.element(elem);
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
