package org.spf4j.tsdb2;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
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
public class TSDBReaderTest {


    @Test
    public void testTsdb() throws IOException {
        File TEST_FILE = File.createTempFile("test", ".tsdb2");
        TSDBWriter writer = new TSDBWriter(TEST_FILE, 4, "test", true);
        final TableDef tableDef = TableDef.newBuilder()
                .setName("test")
                .setDescription("test")
                .setSampleTime(0)
                .setColumns(Arrays.asList(
                        ColumnDef.newBuilder().setName("a").setDescription("atest").setUnitOfMeasurement("ms").build(),
                        ColumnDef.newBuilder().setName("b").setDescription("btest").setUnitOfMeasurement("ms").build(),
                        ColumnDef.newBuilder().setName("c").setDescription("ctest").setUnitOfMeasurement("ms").build()))
                .build();
        long tableId = writer.writeTableDef(tableDef);
        final long time = System.currentTimeMillis();
        writer.writeDataRow(tableId, time, 0, 1, 2);
        writer.writeDataRow(tableId, time + 10, 1, 1, 2);
        writer.writeDataRow(tableId, time + 20, 2, 1, 2);
        writer.writeDataRow(tableId, time + 30, 3, 1, 2);
        writer.writeDataRow(tableId, time + 40, 4, 1, 2);
        writer.close();
        TSDBReader reader = new TSDBReader(TEST_FILE, 1024);
        Either<TableDef, DataBlock> read;
        while ((read = reader.read()) != null ) {
            System.out.println(read);
        }
        reader.close();

        List<TableDef> allTables = TSDBQuery.getAllTables(TEST_FILE);
        Assert.assertEquals(1, allTables.size());
        Assert.assertEquals(tableDef.name, allTables.get(0).name.toString());
        TimeSeries timeSeries = TSDBQuery.getTimeSeries(TEST_FILE, tableId, 0, Long.MAX_VALUE);
        Assert.assertEquals(2L, timeSeries.getValues()[2][0]);

    }

}
