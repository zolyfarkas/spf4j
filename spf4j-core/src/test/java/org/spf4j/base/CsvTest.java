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

package org.spf4j.base;

import com.google.common.base.Charsets;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        
        File testFile = File.createTempFile("csvTest", ".csv");
        
        System.out.println("test file : " + testFile);
        
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(testFile), Charsets.UTF_8));
        try {
            Csv.writeCsvRow(writer, "a", "b", "c", "d");
            Csv.writeCsvRow(writer, "1.2", "1", "1,3", 1);
            Csv.writeCsvRow(writer, "0", "\"", "0\n", "1,3");
        } finally {
            writer.close();
        }
        System.out.println("test file written");
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(testFile), Charsets.UTF_8));
        try {
            List<Map<String, String>> data = Csv.read(reader, new Csv.CsvHandler<List<Map<String, String>>>() {
                
                private boolean firstRow = true;
                
                private final List<Map<String, String>> result = new ArrayList<Map<String, String>>();
                
                private List<String> header;
                private Map<String, String> row;
                private int i = 0;
                
                @Override
                public void startRow() {
                    if (firstRow) {
                        header = new ArrayList<String>();
                    } else {
                        row = new HashMap<String, String>(header.size());
                        i = 0;
                    }
                }

                @Override
                public void element(final StringBuilder elem) {
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
        } finally {
            reader.close();
        }
    }
    
}
