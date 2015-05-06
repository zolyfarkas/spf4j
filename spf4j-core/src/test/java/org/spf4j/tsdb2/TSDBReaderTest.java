package org.spf4j.tsdb2;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.base.Either;
import org.spf4j.base.Handler;
import org.spf4j.tsdb2.avro.ColumnDef;
import org.spf4j.tsdb2.avro.DataBlock;
import org.spf4j.tsdb2.avro.TableDef;

/**
 *
 * @author zoly
 */
public class TSDBReaderTest {

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
    public void testTsdb() throws IOException {
        File TEST_FILE = File.createTempFile("test", ".tsdb2");
        long tableId;
          try (TSDBWriter writer = new TSDBWriter(TEST_FILE, 4, "test", true)) {
              tableId = writer.writeTableDef(tableDef);
              final long time = System.currentTimeMillis();
              writer.writeDataRow(tableId, time, 0, 1, 2);
              writer.writeDataRow(tableId, time + 10, 1, 1, 2);
              writer.writeDataRow(tableId, time + 20, 2, 1, 2);
              writer.writeDataRow(tableId, time + 30, 3, 1, 2);
              writer.writeDataRow(tableId, time + 40, 4, 1, 2);
          }
          try (TSDBReader reader = new TSDBReader(TEST_FILE, 1024)) {
              Either<TableDef, DataBlock> read;
              while ((read = reader.read()) != null ) {
                  System.out.println(read);
              } }

        List<TableDef> allTables = TSDBQuery.getAllTables(TEST_FILE);
        Assert.assertEquals(1, allTables.size());
        Assert.assertEquals(tableDef.name, allTables.get(0).name.toString());
        TimeSeries timeSeries = TSDBQuery.getTimeSeries(TEST_FILE, tableId, 0, Long.MAX_VALUE);
        Assert.assertEquals(2L, timeSeries.getValues()[2][0]);

    }

    @Test
    public void testTailing() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        File TEST_FILE = File.createTempFile("test", ".tsdb2");
          try (TSDBWriter writer = new TSDBWriter(TEST_FILE, 4, "test", true);
                  TSDBReader reader = new TSDBReader(TEST_FILE, 1024)) {
              writer.flush();
              Future<Void> bgWatch = reader.bgWatch(new Handler<Either<TableDef, DataBlock>, Exception> () {

                @Override
                public void handle(Either<TableDef, DataBlock> object, long deadline) throws Exception {
                    System.out.println("Tailed " + object);
                }
              }, TSDBReader.EventSensitivity.HIGH);
              Thread.sleep(1000L);
              long tableId = writer.writeTableDef(tableDef);
              writer.flush();
              final long time = System.currentTimeMillis();
              writer.writeDataRow(tableId, time, 0, 1, 2);
              writer.writeDataRow(tableId, time + 10, 1, 1, 2);
              writer.flush();
              Thread.sleep(1000L);
              writer.writeDataRow(tableId, time + 20, 2, 1, 2);
              writer.writeDataRow(tableId, time + 30, 3, 1, 2);
              writer.writeDataRow(tableId, time + 40, 4, 1, 2);
              writer.flush();
              Thread.sleep(1000);
              reader.stopWatching();
              bgWatch.get(10000, TimeUnit.MILLISECONDS);

       }


    }



}
