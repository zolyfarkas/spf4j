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
import com.google.common.io.Resources;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class CsvTest {

    public CsvTest() {
    }

    @Test
    public void testCsvReadWrite() throws IOException {

        File testFile = createTestCsv();

        List<Map<String, String>> data =
                Csv.read(testFile, Charsets.UTF_8, new Csv.CsvHandler<List<Map<String, String>>>() {

            private boolean firstRow = true;

            private final List<Map<String, String>> result = new ArrayList<>();

            private List<String> header;
            private Map<String, String> row;
            private int i = 0;

            @Override
            public void startRow() {
                if (firstRow) {
                    header = new ArrayList<>();
                } else {
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
        Assert.assertEquals("1,3", data.get(0).get("c"));
        Assert.assertEquals("1", data.get(0).get("d"));
        Assert.assertEquals("1,3", data.get(1).get("d"));
        Assert.assertEquals("\"", data.get(1).get("b"));
        Assert.assertEquals("0\n", data.get(1).get("c"));
    }

    @Test
    public void testCsvReadWrite2() throws IOException {
        File testFile = createTestCsv();
        List<Map<String, CharSequence>> data
                = Csv.read(testFile, Charsets.UTF_8, new Csv.CsvMapHandler<List<Map<String, CharSequence>>>() {

                    private final List<Map<String, CharSequence>> result = new ArrayList<Map<String, CharSequence>>();

                    @Override
                    public void row(final Map<String, CharSequence> row) {
                        result.add(row);
                    }

                    @Override
                    public List<Map<String, CharSequence>> eof() {
                        return result;
                    }
                });

        Assert.assertEquals("1,3", data.get(0).get("c").toString());
        Assert.assertEquals("1", data.get(0).get("d").toString());
        Assert.assertEquals("1,3", data.get(1).get("d").toString());
        Assert.assertEquals("\"", data.get(1).get("b").toString());
        Assert.assertEquals("0\n", data.get(1).get("c").toString());
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


    public static void main(final String [] params) throws IOException {
        CsvTest test = new CsvTest();
        for (int i=0 ; i < 10 ; i++) {
            test.testLargeFileRead();
        }
    }

    @Test
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

}
