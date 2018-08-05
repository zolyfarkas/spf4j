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
package org.spf4j.io;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.CharSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.io.csv.CsvParseException;
import org.spf4j.io.csv.CsvReader;
import org.spf4j.io.csv.CsvReader.TokenType;
import org.spf4j.io.csv.CsvRuntimeException;
import org.spf4j.io.csv.UncheckedCsvParseException;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
public final class CsvTest {

  private static final Logger LOG = LoggerFactory.getLogger(CsvTest.class);

  @Test
  public void testCsvReadWrite() throws IOException, CsvParseException {

    File testFile = createTestCsv();

    List<Map<String, String>> data
            = Csv.read(testFile, StandardCharsets.UTF_8, new Csv.CsvHandler<List<Map<String, String>>>() {

              private boolean firstRow = true;

              private final List<Map<String, String>> result = new ArrayList<>();

              private List<String> header = new ArrayList<>();
              private Map<String, String> row = null;
              private int i = 0;

              @Override
              public void startRow() {
                if (!firstRow) {
                  row = Maps.newHashMapWithExpectedSize(header.size());
                  i = 0;
                }
              }

              @Override
              public void element(final CharSequence elem) {
                if (firstRow) {
                  header.add(elem.toString());
                } else {
                  row.put(header.get(i++), elem.toString());
                }
              }

              @Override
              public void endRow() {
                if (firstRow) {
                  firstRow = false;
                } else {
                  result.add(row);
                }
              }

              @Override
              public List<Map<String, String>> eof() {
                return result;
              }
            });
    Map<String, String> d0 = data.get(0);
    Assert.assertEquals("1,3", d0.get("c"));
    Assert.assertEquals("1", d0.get("d"));
    Map<String, String> d1 = data.get(1);
    Assert.assertEquals("1,3", d1.get("d"));
    Assert.assertEquals("\"", d1.get("b"));
    Assert.assertEquals("0\n", d1.get("c"));
  }

  @Test(expected = CsvParseException.class)
  public void testCsvReadWriteException() throws IOException, CsvParseException {
    File testFile = createTestCsv();

    Csv.read(testFile, StandardCharsets.UTF_8, new Csv.CsvHandler<Void>() {
      @Override
      public void element(final CharSequence elem) throws CsvParseException {
        throw new CsvParseException("Yohooo at " + elem);
      }

      @Override
      public Void eof() {
        return null;
      }
    });
  }


  @Test(expected = CsvRuntimeException.class)
  public void testCsvReadWriteException2() throws IOException, CsvParseException {
    File testFile = createTestCsv();

    Csv.read(testFile, StandardCharsets.UTF_8, new Csv.CsvHandler<Void>() {
      @Override
      public void element(final CharSequence elem) {
        throw new IllegalArgumentException("Yohooo at " + elem);
      }

      @Override
      public Void eof() {
        return null;
      }
    });
  }


  @Test
  public void testCsvReadWrite2() throws IOException, CsvParseException {
    File testFile = createTestCsv();
    List<Map<String, String>> data
            = Csv.read(testFile, StandardCharsets.UTF_8, new Csv.CsvMapHandler<List<Map<String, String>>>() {

              private final List<Map<String, String>> result = new ArrayList<>();

              @Override
              public void row(final Map<String, String> row) {
                result.add(row);
              }

              @Override
              public List<Map<String, String>> eof() {
                return result;
              }
            });
    Map<String, String> d0 = data.get(0);

    Assert.assertEquals("1,3", d0.get("c"));
    Assert.assertEquals("1", d0.get("d"));
    Map<String, String> d1 = data.get(1);
    Assert.assertEquals("1,3", d1.get("d"));
    Assert.assertEquals("\"", d1.get("b"));
    Assert.assertEquals("0\n", d1.get("c"));
  }

  private File createTestCsv() throws IOException {
    File testFile = File.createTempFile("csvTest", ".csv");
    LOG.debug("test file : {}", testFile);
    try (BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(Files.newOutputStream(testFile.toPath()), StandardCharsets.UTF_8))) {
      Csv.writeCsvRow(writer, "a", "b", "c", "d");
      Csv.writeCsvRow(writer, "1.2\r", "1", "1,3", 1);
      Csv.writeCsvRow(writer, "0", "\"", "0\n", "1,3");
    }
    LOG.debug("test file written {}", testFile);
    return testFile;
  }

  @Test
  public void testReadRow() throws IOException, CsvParseException {
    List<String> row = Csv.readRow(new StringReader("a,b,\",c\",d"));
    Assert.assertEquals("a", row.get(0));
  }

  public static void main(final String[] params) throws IOException, CsvParseException {
    CsvTest test = new CsvTest();
    for (int i = 0; i < 10; i++) {
      test.testLargeFileRead();
    }
  }

  @Test
  @Ignore
  public void testLargeFileRead() throws IOException, CsvParseException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
            new GZIPInputStream(
                    new URL("http://www.maxmind.com/download/worldcities/worldcitiespop.txt.gz").openStream()),
            StandardCharsets.UTF_8), 65536)) {
      long startTime = System.currentTimeMillis();
      long count = Csv.read(reader,
              new Csv.CsvHandler<Long>() {

        private long count = 0;

        @Override
        public void element(final CharSequence elem) {
          // do nothing, we are counting rows only
        }

        @Override
        public void endRow() {
          count++;
        }

        @Override
        public Long eof() {
          return count;
        }
      });
      LOG.debug("Line count is {} in {}", count, (System.currentTimeMillis() - startTime));
      Assert.assertEquals(3173959L, count);
    }
  }

  @Test
  public void testCsvRowParsing() throws IOException, CsvParseException {
    List<String> readRow = Csv.readRow(new StringReader(""));
    Assert.assertEquals(Collections.singletonList(""), readRow);
  }

  @Test
  public void testCsvRowParsing2() throws IOException, CsvParseException {
    List<String> readRow = Csv.readRow(CharSource.wrap("").openStream());
    Assert.assertEquals(Collections.singletonList(""), readRow);
  }

  @Test
  public void testCsvStream1() throws IOException, CsvParseException {
    CsvReader reader = Csv.readerNoBOM(new PushbackReader(new StringReader("")));
    Assert.assertEquals(TokenType.ELEMENT, reader.next());
    Assert.assertEquals("", reader.getElement().toString());
    Assert.assertEquals(TokenType.END_DOCUMENT, reader.next());
    Assert.assertEquals(TokenType.END_DOCUMENT, reader.next());
  }

  @Test
  public void testCsvStream1n1() throws IOException, CsvParseException {
    CsvReader reader = Csv.readerNoBOM(new PushbackReader(new StringReader(",")));
    Assert.assertEquals(TokenType.ELEMENT, reader.next());
    Assert.assertEquals("", reader.getElement().toString());
    Assert.assertEquals(TokenType.ELEMENT, reader.next());
    Assert.assertEquals("", reader.getElement().toString());
    Assert.assertEquals(TokenType.END_DOCUMENT, reader.next());
    Assert.assertEquals(TokenType.END_DOCUMENT, reader.next());
  }

  @Test
  public void testCsvStream2() throws IOException, CsvParseException {
    CsvReader reader = Csv.readerNoBOM(new PushbackReader(new StringReader("bla")));
    Assert.assertEquals(TokenType.ELEMENT, reader.next());
    Assert.assertEquals("bla", reader.getElement().toString());
    Assert.assertEquals(TokenType.END_DOCUMENT, reader.next());
    Assert.assertEquals(TokenType.END_DOCUMENT, reader.next());
  }

  @Test
  public void testCsvStream3() throws IOException, CsvParseException {
    CsvReader reader = Csv.readerNoBOM(new PushbackReader(new StringReader("\"bla\"")));
    Assert.assertEquals(TokenType.ELEMENT, reader.next());
    Assert.assertEquals("bla", reader.getElement().toString());
    Assert.assertEquals(TokenType.END_DOCUMENT, reader.next());
    Assert.assertEquals(TokenType.END_DOCUMENT, reader.next());
  }

  @Test
  public void testCsvStream4() throws IOException, CsvParseException {
    CsvReader reader = Csv.readerNoBOM(new PushbackReader(new StringReader("bla,\"bla\"\n")));
    Assert.assertEquals(TokenType.ELEMENT, reader.next());
    Assert.assertEquals("bla", reader.getElement().toString());
    Assert.assertEquals(TokenType.ELEMENT, reader.next());
    Assert.assertEquals("bla", reader.getElement().toString());
    Assert.assertEquals(TokenType.END_ROW, reader.next());
    Assert.assertEquals(TokenType.ELEMENT, reader.next());
    Assert.assertEquals("", reader.getElement().toString());
    Assert.assertEquals(TokenType.END_DOCUMENT, reader.next());
    Assert.assertEquals(TokenType.END_DOCUMENT, reader.next());
  }

  @Test
  public void testCsvStream5() throws IOException, CsvParseException {
    CsvReader reader = Csv.reader(new StringReader("bla,\"bla\"\nuhu,uhu2\n"));
    Assert.assertEquals(TokenType.ELEMENT, reader.next());
    Assert.assertEquals("bla", reader.getElement().toString());
    Assert.assertEquals(TokenType.ELEMENT, reader.next());
    Assert.assertEquals("bla", reader.getElement().toString());
    Assert.assertEquals(TokenType.END_ROW, reader.next());
    Assert.assertEquals(TokenType.ELEMENT, reader.next());
    Assert.assertEquals("uhu", reader.getElement().toString());
    Assert.assertEquals(TokenType.ELEMENT, reader.next());
    Assert.assertEquals("uhu2", reader.getElement().toString());
    Assert.assertEquals(TokenType.END_ROW, reader.next());
    Assert.assertEquals(TokenType.ELEMENT, reader.next());
    Assert.assertEquals("", reader.getElement().toString());
    Assert.assertEquals(TokenType.END_DOCUMENT, reader.next());
    Assert.assertEquals(TokenType.END_DOCUMENT, reader.next());
  }

  @Test
  public void testLineIteration() {
    int nr = 0;
    for (Iterable<String> line : Csv.asIterable(new StringReader("bla,\"bla\"\nuhu,uhu2\n"))) {
      LOG.debug("{}", line);
      nr++;
    }
    Assert.assertEquals(3, nr);
  }

  @Test(expected = UncheckedCsvParseException.class)
  public void testLineIterationError() {
    for (Iterable<String> line : Csv.asIterable(new StringReader("bla,\"bla"))) {
      LOG.debug("{}", line);
    }
    Assert.fail();
  }

  @Test
  public void testCsvFileParsing() throws IOException, CsvParseException {
    try (InputStream resourceAsStream = CsvTest.class.getResourceAsStream("/test.csv")) {
      int nr = Csv.read(new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8),
              new Csv.CsvMapHandler<Integer>() {
        private int i = 0;

        @Override
        public void row(final Map<String, String> row) {
          LOG.debug("Row {}", row);
          i++;
        }

        @Override
        public Integer eof() {
          return i;
        }
      });
      Assert.assertEquals(3, nr);
    }
  }

  @Test
  public void testCsvFileParsing2() throws IOException {
    try (InputStream resourceAsStream = CsvTest.class.getResourceAsStream("/test.csv")) {
      Iterable<Iterable<String>> asIterable
              = Csv.asIterable(new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8));
      int i = 0;
      List<String> lastRow = null;
      for (Iterable<String> row : asIterable) {
        lastRow = Lists.newArrayList(row);
        LOG.debug("row {}", row);
        i++;
      }
      Assert.assertEquals(4, i);
      Assert.assertEquals("3,,\nasdg,,ahsd\nsdf", lastRow.get(0));
    }
  }

  @Test(expected = CsvParseException.class)
  public void testCsvFileParsingBad() throws IOException, CsvParseException {
    try (InputStream resourceAsStream = CsvTest.class.getResourceAsStream("/test_bad.csv")) {
      Csv.read(new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8),
              new Csv.CsvMapHandler<Void>() {
        @Override
        public void row(final Map<String, String> row) {
          LOG.debug("Row {}", row);
        }

        @Override
        public Void eof() {
          return null;
        }
      });
    }
  }

}
