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

import com.google.common.base.Charsets;
import com.google.common.io.CharSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.spf4j.io.csv.CsvReader;
import org.spf4j.io.csv.CsvReader.TokenType;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
public final class CsvTest {

    @Test
    public void testCsvReadWrite() throws IOException {

        File testFile = createTestCsv();

        List<Map<String, String>> data =
                Csv.read(testFile, Charsets.UTF_8, new Csv.CsvHandler<List<Map<String, String>>>() {

            private boolean firstRow = true;

            private final List<Map<String, String>> result = new ArrayList<>();

            private List<String> header = new ArrayList<>();
            private Map<String, String> row = null;
            private int i = 0;

            @Override
            public void startRow() {
                if (!firstRow) {
                    row = new HashMap<>(header.size());
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

    @Test
    public void testCsvReadWrite2() throws IOException {
        File testFile = createTestCsv();
        List<Map<String, String>> data
                = Csv.read(testFile, Charsets.UTF_8, new Csv.CsvMapHandler<List<Map<String, String>>>() {

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
        System.out.println("test file : " + testFile);
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(testFile), Charsets.UTF_8))) {
            Csv.writeCsvRow(writer, "a", "b", "c", "d");
            Csv.writeCsvRow(writer, "1.2\r", "1", "1,3", 1);
            Csv.writeCsvRow(writer, "0", "\"", "0\n", "1,3");
        }
        System.out.println("test file written");
        return testFile;
    }

    @Test
    public void testReadRow() throws IOException {
        List<String> row = Csv.readRow(new StringReader("a,b,\",c\",d"));
        Assert.assertEquals("a", row.get(0));
    }


    public static void main(final String [] params) throws IOException {
        CsvTest test = new CsvTest();
        for (int i=0 ; i < 10 ; i++) {
            test.testLargeFileRead();
        }
    }

    @Test
    @Ignore
    public void testLargeFileRead() throws IOException {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(
                        new URL("http://www.maxmind.com/download/worldcities/worldcitiespop.txt.gz").openStream()),
                Charsets.UTF_8), 65536)) {
        long startTime = System.currentTimeMillis();
        long count = Csv.read(reader,
                new Csv.CsvHandler<Long>() {

                    private long count = 0;
                    @Override
                    public void startRow() {
                    }

                    @Override
                    public void element(CharSequence elem) {
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
        System.out.println("Line count is " + count + " in " + (System.currentTimeMillis() - startTime));
        Assert.assertEquals(3173959L, count);
        }
    }

    @Test
    public void testCsvRowParsing() throws IOException {
      List<String> readRow = Csv.readRow(new StringReader(""));
      Assert.assertEquals(Arrays.asList(""), readRow);
    }

    @Test
    public void testCsvRowParsing2() throws IOException {
      List<String> readRow = Csv.readRow(CharSource.wrap("").openStream());
      Assert.assertEquals(Arrays.asList(""), readRow);
    }

    @Test
    public void testCsvStream1() throws IOException {
      CsvReader reader = Csv.readerNoBOM(new PushbackReader(new StringReader("")));
      Assert.assertEquals(TokenType.ELEMENT, reader.next());
      Assert.assertEquals("", reader.getElement().toString());
      Assert.assertEquals(TokenType.END_DOCUMENT, reader.next());
      Assert.assertEquals(TokenType.END_DOCUMENT, reader.next());
    }

    @Test
    public void testCsvStream1_1() throws IOException {
      CsvReader reader = Csv.readerNoBOM(new PushbackReader(new StringReader(",")));
      Assert.assertEquals(TokenType.ELEMENT, reader.next());
      Assert.assertEquals("", reader.getElement().toString());
      Assert.assertEquals(TokenType.ELEMENT, reader.next());
      Assert.assertEquals("", reader.getElement().toString());
      Assert.assertEquals(TokenType.END_DOCUMENT, reader.next());
      Assert.assertEquals(TokenType.END_DOCUMENT, reader.next());
    }

    @Test
    public void testCsvStream2() throws IOException {
      CsvReader reader = Csv.readerNoBOM(new PushbackReader(new StringReader("bla")));
      Assert.assertEquals(TokenType.ELEMENT, reader.next());
      Assert.assertEquals("bla", reader.getElement().toString());
      Assert.assertEquals(TokenType.END_DOCUMENT, reader.next());
      Assert.assertEquals(TokenType.END_DOCUMENT, reader.next());
    }

    @Test
    public void testCsvStream3() throws IOException {
      CsvReader reader = Csv.readerNoBOM(new PushbackReader(new StringReader("\"bla\"")));
      Assert.assertEquals(TokenType.ELEMENT, reader.next());
      Assert.assertEquals("bla", reader.getElement().toString());
      Assert.assertEquals(TokenType.END_DOCUMENT, reader.next());
      Assert.assertEquals(TokenType.END_DOCUMENT, reader.next());
    }

    @Test
    public void testCsvStream4() throws IOException {
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
    public void testCsvStream5() throws IOException {
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
      for(List<String> line : Csv.asIterable(new StringReader("bla,\"bla\"\nuhu,uhu2\n"))) {
        System.out.println(line);
        nr++;
      };
      Assert.assertEquals(3, nr);
    }



}
