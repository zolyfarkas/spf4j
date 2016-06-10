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
  @SuppressFBWarnings({ "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", "RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE",
                        "NP_LOAD_OF_KNOWN_NULL_VALUE", "CLI_CONSTANT_LIST_INDEX"})
  // try with resources trips up findbugs sometimes.
  public void testTsdb() throws IOException {
    File TEST_FILE = File.createTempFile("test", ".tsdb2");
    long tableId;
    try (TSDBWriter writer = new TSDBWriter(TEST_FILE, 4, "test", false)) {

    }
    try (TSDBWriter writer = new TSDBWriter(TEST_FILE, 4, "test", false)) {
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
      while ((read = reader.read()) != null) {
        System.out.println(read);
      }
    }

    ListMultimap<String, TableDef> allTables = TSDBQuery.getAllTables(TEST_FILE);
    Assert.assertEquals(1, allTables.size());
    Assert.assertTrue(allTables.containsKey(tableDef.name));
    TimeSeries timeSeries = TSDBQuery.getTimeSeries(TEST_FILE, new long[]{tableId}, 0, Long.MAX_VALUE);
    Assert.assertEquals(2L, timeSeries.getValues()[2][0]);

  }

  @Test
  public void testTailing() throws IOException, InterruptedException, ExecutionException, TimeoutException {
    File TEST_FILE = File.createTempFile("test", ".tsdb2");
    try (TSDBWriter writer = new TSDBWriter(TEST_FILE, 4, "test", true);
            TSDBReader reader = new TSDBReader(TEST_FILE, 1024)) {
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
