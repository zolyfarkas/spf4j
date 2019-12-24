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
package org.spf4j.tsdb2;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.CloseableIterable;
import org.spf4j.perf.TimeSeriesRecord;
import org.spf4j.tsdb2.avro.ColumnDef;
import org.spf4j.tsdb2.avro.TableDef;

/**
 *
 * @author Zoltan Farkas
 */
public class TSDBQueryTest {

  private static final Logger LOG = LoggerFactory.getLogger(TSDBQueryTest.class);

  private final TableDef tableDef = TableDef.newBuilder()
          .setName("test")
          .setDescription("test")
          .setSampleTime(0)
          .setColumns(Arrays.asList(
                  ColumnDef.newBuilder().setName("a").setDescription("atest").setUnitOfMeasurement("ms").build(),
                  ColumnDef.newBuilder().setName("b").setDescription("btest").setUnitOfMeasurement("ms").build(),
                  ColumnDef.newBuilder().setName("c").setDescription("ctest").setUnitOfMeasurement("ms").build()))
          .build();

  @Test
  public void testTsDbQuery() throws IOException {
    File testFile = File.createTempFile("test", ".tsdb2");
    try (TSDBWriter writer = new TSDBWriter(testFile, 4, "test", false)) {
      long tableId = writer.writeTableDef(tableDef);
      writer.writeDataRow(tableId, System.currentTimeMillis(), 0, 1, 2);
      writer.writeDataRow(tableId, System.currentTimeMillis(), 1, 1, 2);
      writer.writeDataRow(tableId, System.currentTimeMillis(), 2, 1, 2);
      writer.writeDataRow(tableId, System.currentTimeMillis(), 3, 1, 2);
      writer.writeDataRow(tableId, System.currentTimeMillis(), 4, 1, 2);
      writer.flush();
      writer.writeDataRow(tableId, System.currentTimeMillis(), 0, 1, 2);
      writer.writeDataRow(tableId, System.currentTimeMillis(), 1, 1, 2);
      writer.writeDataRow(tableId, System.currentTimeMillis(), 2, 1, 2);
      writer.writeDataRow(tableId, System.currentTimeMillis(), 3, 1, 2);
      writer.writeDataRow(tableId, System.currentTimeMillis(), 4, 1, 2);
    }
    try (CloseableIterable<TimeSeriesRecord> res = TSDBQuery.getTimeSeriesData(
            testFile, "test", System.currentTimeMillis() - 10000, System.currentTimeMillis())) {
      int i = 0;
      for (TimeSeriesRecord rec : res) {
        LOG.debug("measurement", rec);
        i++;
      }
      Assert.assertEquals(10, i);
    }

  }

}
