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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gnu.trove.map.hash.THashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.base.Arrays;
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
    if (separator == '\n' || separator == '\r' || separator == '"') {
      throw new IllegalArgumentException("Illegal separator character " + separator);
    }
    this.separator = separator;
    this.toEscape = new char[]{separator, '\n', '\r', '"'};
  }

  public CharSeparatedValues(final char separator, final char... extraCharsToEscape) {
    if (separator == '\n' || separator == '\r' || separator == '"') {
      throw new IllegalArgumentException("Illegal separator character " + separator);
    }
    this.separator = separator;
    this.toEscape = new char[4 + extraCharsToEscape.length];
    this.toEscape[0] = separator;
    this.toEscape[1] = '\n';
    this.toEscape[2] = '\r';
    this.toEscape[3] = '"';
    System.arraycopy(extraCharsToEscape, 0, this.toEscape, 4, extraCharsToEscape.length);
  }

  public void writeCsvRow(final Appendable writer, final Object... elems) throws IOException {
    writeCsvRowNoEOL(writer, elems);
    writer.append('\n');
  }

  @SafeVarargs
  public final String toCsvRowString(final Object... elems) {
    StringBuilder result = new StringBuilder(elems.length * 8);
    try {
      writeCsvRowNoEOL(result, elems);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return result.toString();
  }

  public void writeCsvRowNoEOL(final Appendable writer, final Object... elems) throws IOException {
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
    CsvReader r = reader(reader);
    handler.startRow(0);
    CsvReader.TokenType token = r.next();
    while (token != CsvReader.TokenType.END_DOCUMENT) {
      if (token == CsvReader.TokenType.ELEMENT) {
        handler.element(r.getElement());
        token = r.next();
      } else if (token == CsvReader.TokenType.END_ROW) {
        handler.endRow();
        token = r.next();
        if (token == CsvReader.TokenType.ELEMENT) {
          handler.startRow(r.currentLineNumber());
        }
      }
    }
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

  /**
   * Iterate through the first row of your CSV.
   * the CharSequence is a re-0used char buffer you either need to parse the content out of copy it.
   * @param preader
   * @return
   */
  public Iterable<CharSequence> singleRow(final Reader preader) {
    try {
      CsvReader reader = reader(preader);
      return () -> new OneRowIterator(reader);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public CsvReader reader(final Reader preader) throws IOException {
    PushbackReader reader = new PushbackReader(preader);
    int firstChar = reader.read();
    if (firstChar != UTF_BOM && firstChar >= 0) {
      reader.unread(firstChar);
    }
    return readerNoBOMILEL(reader);
  }

  /**
   * will ignore last empty line.
   * @param preader
   * @return
   * @throws IOException
   * @deprecated use reader
   */
  @Deprecated
  public CsvReader readerILEL(final Reader preader) throws IOException {
    return reader(preader);
  }

  /**
   * assumes there is not BOM. (byte order marker)
   * @param reader
   * @return
   */
  public CsvReader readerNoBOM(final PushbackReader reader) {
    return new CsvReaderImpl(reader);
  }

  /**
   * reader that there is not BOM. (byte order marker) and will ignore last empty line.
   * @param reader
   * @return
   * @deprecated use readerNoBOM.
   */
  @Deprecated
  public CsvReader readerNoBOMILEL(final PushbackReader reader) {
    return new CsvReaderImpl(reader);
  }

  public CsvWriter writer(final Writer writer) {
    return new CsvWriterImpl(writer);
  }

  public void writeCsvElement(final CharSequence elem, final Appendable writer) throws IOException {
    if (CharSequences.containsAnyChar(elem, toEscape)) {
      writeQuotedCsvElement(elem, writer);
    } else {
      writer.append(elem);
    }
  }

  public static void writeQuotedCsvElement(final CharSequence elem, final Appendable writer) throws IOException {
    writer.append('"');
    writeQuotedElementContent(elem, 0, elem.length(), writer);
    writer.append('"');
  }

  public static void writeQuotedElementContent(final CharSequence elem,
          final int start, final int end, final Appendable writer) throws IOException {
    for (int i = start; i < end; i++) {
      char c = elem.charAt(i);
      writeQuotedChar(c, writer);
    }
  }

  public static void writeQuotedChar(final char c, final Appendable writer) throws IOException {
    if (c == '"') {
      writer.append("\"\"");
    } else {
      writer.append(c);
    }
  }

   public CharSequence toCsvElement(final CharSequence elem) {
    if (CharSequences.containsAnyChar(elem, toEscape)) {
      StringBuilder sw = new StringBuilder(elem.length() + 4);
      try {
        writeQuotedCsvElement(elem, sw);
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
      return sw;
    } else {
      return elem;
    }
  }

   public String toCsvElement(final String elem) {
    if (CharSequences.containsAnyChar(elem, toEscape)) {
      StringBuilder sw = new StringBuilder(elem.length() + 4);
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
  public int readCsvElement(final Reader reader, final StringBuilder addElemTo, final long lineNr)
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
    private long lineNr;


    CsvMapHandler2CsvHandler(final CsvMapHandler<T> handler) {
      this.handler = handler;
    }

    @Override
    public void startRow(final long ln) {
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
    private CsvReader.TokenType currentToken;
    private CsvReader.TokenType nextToken;
    private long lineNr = 0;

    CsvReaderImpl(final PushbackReader reader) {
      this.reader = reader;
      this.currentToken = CsvReader.TokenType.START_DOCUMENT;
      this.nextToken = null;
    }

   @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
   private void readNext() throws IOException, CsvParseException {
     // nextToken will always be null;
     switch (currentToken) {
       case END_DOCUMENT:
         nextToken = TokenType.END_DOCUMENT;
         return;
       case END_ROW:
         // handle special case of EOF followed by EOL.
         int peek = reader.read();
         if (peek < 0) {
           currentToken = TokenType.END_DOCUMENT;
           nextToken = TokenType.END_DOCUMENT;
           return;
         }
         reader.unread(peek);
       case START_DOCUMENT:
       case ELEMENT:
         currentElement.setLength(0);
         int next = readCsvElement(reader, currentElement, lineNr);
         currentToken = CsvReader.TokenType.ELEMENT;
         switch (next) {
           case '\r':
             lineNr++;
             nextToken = CsvReader.TokenType.END_ROW;
             int c2 = reader.read();
             if (c2 < 0) {
               return;
             }
             if (c2 != '\n') {
               reader.unread(c2);
             }
             return;
           case '\n':
             lineNr++;
             nextToken = CsvReader.TokenType.END_ROW;
             c2 = reader.read();
             if (c2 < 0) {
               return;
             }
             if (c2 != '\r') {
               reader.unread(c2);
               break;
             }
             break;
           default:
             if (next != separator) {
               if (next < 0) {
                 nextToken = CsvReader.TokenType.END_ROW;
               } else {
                 throw new CsvParseException("Unexpected character " + next + " at line" + lineNr);
               }
             }
         }
         return;
       default:
         throw new IllegalStateException("Invalid current token " + currentToken);

     }

   }

    @Override
    public CsvReader.TokenType next() throws IOException, CsvParseException {
      if (nextToken == null) {
        readNext();
        return currentToken;
      } else {
        CsvReader.TokenType result = nextToken;
        if (result != CsvReader.TokenType.END_DOCUMENT) {
          nextToken = null;
        }
        currentToken = result;
        return result;
      }
    }

    @Override
    public CsvReader.TokenType current() {
      return currentToken;
    }

    @Override
    public CharSequence getElement() {
      if (currentToken != TokenType.ELEMENT) {
        throw new IllegalStateException("No current element, current token is " + currentToken);
      }
      return currentElement;
    }

    @Override
    public long currentLineNumber() {
      return lineNr;
    }

  }

  private static class OneRowHandler<T> implements CsvHandler<T> {

    private final CsvRowHandler<T> handler;


    OneRowHandler(final CsvRowHandler<T> handler) {
      this.handler = handler;
    }

    @Override
    public void startRow(final long rowNr) {
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

  private class CsvWriterImpl implements CsvWriter {

    private final Writer writer;

    CsvWriterImpl(final Writer writer) {
      this.writer = writer;
    }
    private boolean isStartLine = true;

    @Override
    public void writeElement(final CharSequence cs) throws IOException {
      addComma();
      writeCsvElement(cs, writer);
    }

    private void addComma() throws IOException {
      if (isStartLine) {
        isStartLine = false;
      } else {
        writer.append(separator);
      }
    }

    @Override
    public void writeEol() throws IOException {
      writer.append('\n');
      isStartLine = true;
    }

    @Override
    public void flush() throws IOException {
      writer.flush();
    }


    @Override
    public ElementAppendable startQuotedElement() throws IOException {
      addComma();
      writer.write('"');
      return new ElementAppendable() {
        @Override
        public Appendable append(final CharSequence csq) throws IOException {
          writeQuotedElementContent(csq, 0, csq.length(), writer);
          return this;
        }

        @Override
        public Appendable append(final CharSequence csq, final int start, final int end) throws IOException {
          writeQuotedElementContent(csq, start, end, writer);
          return this;
        }

        @Override
        public Appendable append(final char c) throws IOException {
          writeQuotedChar(c, writer);
          return this;
        }

        @Override
        public void close() throws IOException {
          writer.write('"');
        }
      };
    }

    @Override
    public Appendable startRawElement() throws IOException {
      addComma();
      return new Appendable() {
        @Override
        public Appendable append(final CharSequence csq) throws IOException {
          if (CharSequences.containsAnyChar(csq, toEscape)) {
            throw new IllegalStateException("Attempting to write str containing escapeable seq " + csq);
          }
          writer.append(csq);
          return this;
        }

        @Override
        public Appendable append(final CharSequence csq, final int start, final int end) throws IOException {
          if (CharSequences.containsAnyChar(csq, start, end, toEscape)) {
            throw new IllegalStateException("Attempting to write str containing escapeable seq " + csq);
          }
          writer.append(csq, start, end);
          return this;
        }

        @Override
        public Appendable append(final char c) throws IOException {
          if (Arrays.search(toEscape, c) >= 0) {
            throw new IllegalStateException("Attempting to write str containing escapeable seq " + c);
          }
          writer.append(c);
          return this;
        }
      };
    }
  }

}
