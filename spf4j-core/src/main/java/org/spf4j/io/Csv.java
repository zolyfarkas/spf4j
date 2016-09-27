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
package org.spf4j.io;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.base.Strings;
import org.spf4j.io.csv.CharSeparatedValues;
import org.spf4j.io.csv.CsvReader;

/**
 * Supports CSV format as described at: https://en.wikipedia.org/wiki/Comma-separated_values. either of \n \r or \r\n
 * are valid end of line delimiters
 *
 * why another implementation? because I need one that is as fast as possible, and as flexible as possible.
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
@SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE") // FB gets it wrong here
public final class Csv {

  private Csv() {
  }

  public interface CsvHandler<T> extends org.spf4j.io.csv.CsvHandler<T> {
  }

  public interface CsvRowHandler<T> extends org.spf4j.io.csv.CsvRowHandler<T> {
  }

  public interface CsvMapHandler<T> extends org.spf4j.io.csv.CsvMapHandler<T> {
  }

  public static final CharSeparatedValues CSV = new CharSeparatedValues(',');

  public static void writeCsvRow(final Appendable writer, final Object... elems) throws IOException {
    CSV.writeCsvRow(writer, elems);
  }

  public static void writeCsvRow2(final Appendable writer, final Object obj, final Object... elems)
          throws IOException {
    CSV.writeCsvRow2(writer, obj, elems);
  }

  public static void writeCsvRow(final Appendable writer, final long... elems) throws IOException {
    CSV.writeCsvRow(writer, elems);
  }

  public static void writeCsvRowNoEOL(final long[] elems, final Appendable writer) throws IOException {
    CSV.writeCsvRowNoEOL(elems, writer);
  }

  public static void writeCsvRow(final Appendable writer, final Iterable<?> elems) throws IOException {
    CSV.writeCsvRow(writer, elems);
  }

  public static void writeCsvRowNoEOL(final Iterable<?> elems, final Appendable writer) throws IOException {
    CSV.writeCsvRowNoEOL(elems, writer);
  }

  public static <T> T read(final File file, final Charset charset,
          final CsvMapHandler<T> handler) throws IOException {
    return CSV.read(file, charset, handler);
  }

  public static <T> T read(final File file, final Charset charset,
          final CsvHandler<T> handler) throws IOException {
    return CSV.read(file, charset, handler);
  }

  public static List<Map<String, String>> read(final Reader preader) throws IOException {
    return CSV.read(preader);
  }

  public static <T> T read(final Reader preader,
          final CsvMapHandler<T> handler) throws IOException {
    return CSV.read(preader, handler);
  }

  public static List<String> readRow(final Reader reader) throws IOException {
    return CSV.readRow(reader);
  }

  public static <T> T readRow(final Reader reader, final CsvRowHandler<T> handler) throws IOException {
    return CSV.readRow(reader, handler);
  }

  public static <T> T read(final Reader preader,
          final CsvHandler<T> handler) throws IOException {
    return CSV.read(preader, handler);
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
  public static <T> T readNoBom(final PushbackReader reader, final CsvHandler<T> handler) throws IOException {
    return CSV.readNoBom(reader, handler);
  }

  /**
   * read a CSV stream, as a Iterable over rows.
   * the List<String> instance is reused during iteration, you will need to copy content into
   * own data structure.
   * @param preader
   * @return
   */
  public static Iterable<List<String>> asIterable(final Reader preader) {
    return CSV.asIterable(preader);
  }

  public static CsvReader reader(final Reader preader) throws IOException {
    return CSV.reader(preader);
  }

  public static CsvReader readerNoBOM(final PushbackReader reader) {
    return CSV.readerNoBOM(reader);
  }

  private static final char[] TO_ESCAPE = new char[]{',', '\n', '\r', '"'};

  public static void writeCsvElement(final CharSequence elem, final Appendable writer) throws IOException {
    if (Strings.contains(elem, TO_ESCAPE)) {
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

  public static CharSequence toCsvElement(final CharSequence elem) {
    if (Strings.contains(elem, TO_ESCAPE)) {
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
  public static int readCsvElement(final Reader reader, final StringBuilder addElemTo) throws IOException {
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
      while (c != ',' && c != '\n' && c != '\r' && c >= 0) {
        addElemTo.append((char) c);
        c = reader.read();
      }
    }
    return c;
  }

}
