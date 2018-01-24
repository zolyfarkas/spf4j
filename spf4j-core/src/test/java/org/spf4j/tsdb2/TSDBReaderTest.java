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

import com.google.common.collect.ListMultimap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Either;
import org.spf4j.tsdb2.avro.ColumnDef;
import org.spf4j.tsdb2.avro.DataBlock;
import org.spf4j.tsdb2.avro.TableDef;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
public class TSDBReaderTest {

  private static final Logger LOG = LoggerFactory.getLogger(TSDBReaderTest.class);

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
  @SuppressFBWarnings({"RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", "RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE",
    "NP_LOAD_OF_KNOWN_NULL_VALUE", "CLI_CONSTANT_LIST_INDEX"})
  // try with resources trips up findbugs sometimes.
  @SuppressWarnings("checkstyle:EmptyBlock")
  public void testTsdb() throws IOException {
    File testFile = File.createTempFile("test", ".tsdb2");
    long tableId;
    try (TSDBWriter writer = new TSDBWriter(testFile, 4, "test", false)) {

    }
    try (TSDBWriter writer = new TSDBWriter(testFile, 4, "test", false)) {
      tableId = writer.writeTableDef(tableDef);
      final long time = System.currentTimeMillis();
      writer.writeDataRow(tableId, time, 0, 1, 2);
      writer.writeDataRow(tableId, time + 10, 1, 1, 2);
      writer.writeDataRow(tableId, time + 20, 2, 1, 2);
      writer.writeDataRow(tableId, time + 30, 3, 1, 2);
      writer.writeDataRow(tableId, time + 40, 4, 1, 2);
    }
    try (TSDBReader reader = new TSDBReader(testFile, 1024)) {
      Either<TableDef, DataBlock> read;
      while ((read = reader.read()) != null) {
        LOG.debug("TSDB block: {}", read);
      }
    }

    ListMultimap<String, TableDef> allTables = TSDBQuery.getAllTables(testFile);
    Assert.assertEquals(1, allTables.size());
    Assert.assertTrue(allTables.containsKey(tableDef.name));
    TimeSeries timeSeries = TSDBQuery.getTimeSeries(testFile, new long[]{tableId}, 0, Long.MAX_VALUE);
    Assert.assertEquals(2L, timeSeries.getValues()[2][0]);

  }

  @Test
  public void testTailing() throws IOException, InterruptedException, ExecutionException, TimeoutException {
    File testFile = File.createTempFile("test", ".tsdb2");
    try (TSDBWriter writer = new TSDBWriter(testFile, 4, "test", true);
            TSDBReader reader = new TSDBReader(testFile, 1024)) {
      writer.flush();
      final BlockingQueue<Either<TableDef, DataBlock>> queue = new ArrayBlockingQueue<>(100);
      Future<Void> bgWatch = reader.bgWatch((Either<TableDef, DataBlock> object, long deadline) -> {
        queue.put(object);
      }, TSDBReader.EventSensitivity.HIGH);
      long tableId = writer.writeTableDef(tableDef);
      writer.flush();
      final long time = System.currentTimeMillis();
      writer.writeDataRow(tableId, time, 0, 1, 2);
      writer.writeDataRow(tableId, time + 10, 1, 1, 2);
      writer.flush();
      Either<TableDef, DataBlock> td = queue.take();
      Assert.assertEquals(tableDef, td.getLeft());
      Either<TableDef, DataBlock> take = queue.take();
      Assert.assertEquals(2, take.getRight().getValues().size());
      writer.writeDataRow(tableId, time + 20, 2, 1, 2);
      writer.writeDataRow(tableId, time + 30, 3, 1, 2);
      writer.writeDataRow(tableId, time + 40, 4, 1, 2);
      writer.flush();
      Assert.assertEquals(3, queue.take().getRight().getValues().size());
      reader.stopWatching();
      bgWatch.get(10000, TimeUnit.MILLISECONDS);

    }

  }

}
