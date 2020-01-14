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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.primitives.Longs;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.spf4j.avro.AvroCompatUtils;
import org.spf4j.base.DateTimeFormats;
import org.spf4j.base.Either;
import org.spf4j.base.Strings;
import org.spf4j.base.avro.AvroCloseableIterable;
import org.spf4j.io.Csv;
import org.spf4j.perf.TimeSeriesRecord;
import org.spf4j.tsdb2.avro.ColumnDef;
import org.spf4j.tsdb2.avro.DataBlock;
import org.spf4j.tsdb2.avro.DataRow;
import org.spf4j.tsdb2.avro.MeasurementType;
import org.spf4j.tsdb2.avro.TableDef;
import org.spf4j.tsdb2.avro.Type;

/**
 *
 * @author zoly
 */
public final class TSDBQuery {

  public static final String MEASUREMENT_TYPE_PROP = "measurementType";

  private TSDBQuery() {
  }

  @SuppressFBWarnings("STT_STRING_PARSING_A_FIELD")
  public static MeasurementType getMeasurementType(final TableDef info) {
    MeasurementType measurementType = info.getMeasurementType();
    if (measurementType != MeasurementType.UNTYPED) {
      return measurementType;
    }
    boolean hasCount = false;
    for (ColumnDef colDef : info.getColumns()) {
      String colName = colDef.getName();
      if (colName.startsWith("Q") && colName.contains("_")) {
        return MeasurementType.HISTOGRAM;
      } else if ("sum".equals(colName)) {
        return MeasurementType.SUMMARY;
      } else if ("count".equals(colName)) {
        hasCount = true;
      }
    }
    if (hasCount) {
      return MeasurementType.COUNTER;
    }
    return MeasurementType.UNTYPED;
  }

  public static MeasurementType getMeasurementType(final Schema schema) {
    String mt = schema.getProp(MEASUREMENT_TYPE_PROP);
    if (mt == null) {
      return MeasurementType.UNTYPED;
    }
    return MeasurementType.valueOf(mt);
  }


  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
  public static ListMultimap<String, TableDef> getAllTables(final File tsdbFile) throws IOException {
    ListMultimap<String, TableDef> result = ArrayListMultimap.create();
    try (TSDBReader reader = new TSDBReader(tsdbFile, 8192)) {
      Either<TableDef, DataBlock> read;
      while ((read = reader.read()) != null) {
        if (read.isLeft()) {
          final TableDef tdef = read.getLeft();
          result.put(tdef.getName(), tdef);
        }
      }
    }
    return result;
  }


  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
  public static ListMultimap<String, TableDef> getTables(final File tsdbFile, final Set<String> tables)
          throws IOException {
    ListMultimap<String, TableDef> result = ArrayListMultimap.create();
    try (TSDBReader reader = new TSDBReader(tsdbFile, 8192)) {
      Either<TableDef, DataBlock> read;
      while ((read = reader.read()) != null) {
        if (read.isLeft()) {
          final TableDef tdef = read.getLeft();
          final String name = tdef.getName();
          if (tables.contains(name)) {
            result.put(name, tdef);
          }
        }
      }
    }
    return result;
  }

  public static final class TableDefEx {

    private final TableDef tableDef;
    private long startTime;
    private long endTime;

    public TableDefEx(final TableDef tableDef, final long startTime, final long endTime) {
      this.tableDef = tableDef;
      this.startTime = startTime;
      this.endTime = endTime;
    }

    public TableDef getTableDef() {
      return tableDef;
    }

    public long getStartTime() {
      return startTime;
    }

    public long getEndTime() {
      return endTime;
    }

    public void setStartTime(final long startTime) {
      this.startTime = startTime;
    }

    public void setEndTime(final long endTime) {
      this.endTime = endTime;
    }

    @Override
    public String toString() {
      return "TableDefEx{" + "tableDef=" + tableDef + ", startTime=" + startTime + ", endTime=" + endTime + '}';
    }

  }

  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
  public static ListMultimap<String, TableDefEx> getAllTablesWithDataRanges(final File tsdbFile) throws IOException {
    ListMultimap<String, TableDefEx> result = ArrayListMultimap.create();
    TLongObjectMap<TableDefEx> id2Def = new TLongObjectHashMap<>();
    try (TSDBReader reader = new TSDBReader(tsdbFile, 8192)) {
      Either<TableDef, DataBlock> read;
      while ((read = reader.read()) != null) {
        if (read.isLeft()) {
          final TableDef left = read.getLeft();
          final TableDefEx tableDefEx = new TableDefEx(left, Long.MAX_VALUE, 0L);
          id2Def.put(left.getId(), tableDefEx);
          result.put(tableDefEx.getTableDef().getName(), tableDefEx);
        } else {
          DataBlock right = read.getRight();
          long baseTs = right.getBaseTimestamp();
          for (DataRow row : right.getValues()) {
            TableDefEx tdex = id2Def.get(row.getTableDefId());
            if (tdex == null) {
              throw new IOException("Potentially corupted file data row with no tableDef " + row);
            }
            long ts = baseTs + row.getRelTimeStamp();
            if (ts < tdex.getStartTime()) {
              tdex.setStartTime(ts);
            }
            if (ts > tdex.getEndTime()) {
              tdex.setEndTime(ts);
            }
          }
        }
      }
    }
    return result;
  }

  @Nonnull
  public static List<TableDef> getTableDef(final File tsdbFile, final String tableName) throws IOException {
    List<TableDef> result = new ArrayList<>();
    try (TSDBReader reader = new TSDBReader(tsdbFile, 8192)) {
      Either<TableDef, DataBlock> read;
      while ((read = reader.read()) != null) {
        if (read.isLeft()) {
          TableDef left = read.getLeft();
          if (tableName.equals(left.getName())) {
            result.add(left);
          }
        }
      }
    }
    return result;
  }

  public static TimeSeries getTimeSeries(final File tsdbFile, final long[] tableIds,
          final long startTimeMillis, final long endTimeMillis) throws IOException {
    TLongList timestamps = new TLongArrayList();
    List<long[]> metrics = new ArrayList<>();
    getTimeSeries(tsdbFile, tableIds, startTimeMillis, endTimeMillis, (ts, data) -> {
      timestamps.add(ts);
      metrics.add(data);
    });
    return new TimeSeries(timestamps.toArray(), metrics.toArray(new long[metrics.size()][]));
  }

 public static void getTimeSeries(final File tsdbFile, final long[] tableIds,
          final long startTimeMillis, final long endTimeMillis, final BiConsumer<Long, long[]> consumer)
         throws IOException {
    try (TSDBReader reader = new TSDBReader(tsdbFile, 8192)) {
      Either<TableDef, DataBlock> read;
      while ((read = reader.read()) != null) {
        if (read.isRight()) {
          DataBlock data = read.getRight();
          long baseTs = data.getBaseTimestamp();
          for (DataRow row : data.getValues()) {
            for (long tableId : tableIds) {
              if (tableId == row.getTableDefId()) {
                final long ts = baseTs + row.getRelTimeStamp();
                if (ts >= startTimeMillis && ts <= endTimeMillis) {
                  consumer.accept(ts, Longs.toArray(row.getData()));
                }
              }
            }
          }
        }
      }
    }
  }

 /**
  *
  * @param tsdbFile
  * @param tableName
  * @param startTimeMillis
  * @param endTimeMillis
  * @return iterator through the results, null when not table found.
  * @throws IOException
  */
  @Nullable
  public static AvroCloseableIterable<TimeSeriesRecord> getTimeSeriesData(final File tsdbFile, final String tableName,
          final long startTimeMillis, final long endTimeMillis)
          throws IOException {
    List<TableDef> tableDef = getTableDef(tsdbFile, tableName);
    if (tableDef.isEmpty()) {
      return null;
    }
    TLongSet ids = new TLongHashSet(getIds(tableDef));
    TableDef td = tableDef.get(0);
    Schema rSchema = createSchema(td);
    TSDBReader reader = new TSDBReader(tsdbFile, 8192);
    try {
      DataScan dataScan = new DataScan(reader);
      Iterable<Row> filtered = Iterables.filter(dataScan,
              (x) -> x.timestamp >= startTimeMillis
              && x.timestamp <= endTimeMillis && ids.contains(x.data.getTableDefId()));

      Iterable<TimeSeriesRecord> it = Iterables.transform(filtered, (x) -> {
        GenericRecord rec = new GenericData.Record(rSchema);
        rec.put(0, Instant.ofEpochMilli(x.timestamp));
        List<Long> nrs = x.data.getData();
        List<Schema.Field> fields = rSchema.getFields();
        for (int i = 1, l = fields.size(); i < l; i++) {
          Schema.Type type = fields.get(i).schema().getType();
          switch (type) {
            case DOUBLE:
              rec.put(i, Double.longBitsToDouble(nrs.get(i - 1)));
              break;
            case LONG:
              rec.put(i, nrs.get(i - 1));
              break;
            default:
              throw new IllegalStateException("Unsupported data type: " + type);
          }
        }
        return TimeSeriesRecord.from(rec);
      });
      return AvroCloseableIterable.from(it, reader, rSchema);
    } catch (RuntimeException | IOException ex) {
      reader.close();
      throw ex;
    }
  }

  public static Schema createSchema(final TableDef td) {
    Schema recSchema = AvroCompatUtils.createRecordSchema(td.getName(), td.getDescription(), null, false, false);
    List<ColumnDef> columns = td.getColumns();
    List<Schema.Field> fields = new ArrayList<>(columns.size() + 1);
    Schema ts = new Schema.Parser().parse("{\"type\":\"string\",\"logicalType\":\"instant\"}");
    fields.add(AvroCompatUtils.createField("ts", ts, "Measurement time stamp", null, true, false,
            Schema.Field.Order.IGNORE));
    for (ColumnDef cd : columns) {
      Type type = cd.getType();
      switch (type) {
        case DOUBLE:
          fields.add(AvroCompatUtils.createField(cd.getName(),
                  new Schema.Parser().parse("{\"type\":\"double\",\"unit\":\""
                          + cd.getUnitOfMeasurement() + "\"}"), cd.getDescription(), null, true, false,
            Schema.Field.Order.IGNORE));
          break;
        case LONG:
          fields.add(AvroCompatUtils.createField(cd.getName(),
                  new Schema.Parser().parse("{\"type\":\"long\",\"unit\":\""
                          + cd.getUnitOfMeasurement() + "\"}"), cd.getDescription(), null, true, false,
            Schema.Field.Order.IGNORE));
          break;
        default:
          throw new IllegalStateException("Invalid data type " + type);
      }
    }
    recSchema.setFields(fields);
    int sampleTime = td.getSampleTime();
    if (sampleTime > 0) {
      recSchema.addProp("frequencyMillis", sampleTime);
    }
    recSchema.addProp(MEASUREMENT_TYPE_PROP, getMeasurementType(td));
    return recSchema;
  }

  private static class Row {
    Row(final long ts, final DataRow data) {
      this.timestamp = ts;
      this.data = data;
    }
    private final long timestamp;
    private final DataRow data;
  }


    private static class DataScan implements Iterable<Row> {

      private final TSDBReader reader;

      DataScan(final TSDBReader tsdb) throws IOException {
        reader = tsdb;
      }


     @Override
     public Iterator<Row> iterator() {
       return new Iterator<Row>() {

         private long baseTs;
         private Iterator<DataRow> dataBlock;
         {
           nextBlock();
         }

         private void nextBlock() {
           Either<TableDef, DataBlock> read;
           try {
            while ((read = reader.read()) != null) {
              if (read.isRight()) {
                 DataBlock block = read.getRight();
                 baseTs = block.getBaseTimestamp();
                 dataBlock = block.getValues().iterator();
                 return;
              }
            }
           } catch (IOException ex) {
             throw new UncheckedIOException(ex);
           }
           dataBlock = null;
         }

         @Override
         public boolean hasNext() {
           while (true) {
              if (dataBlock == null) {
                return false;
              }
              if (dataBlock.hasNext()) {
                return true;
              }
              nextBlock();
           }
         }

         @Override
         public Row next() {
           while (true) {
              if (dataBlock == null) {
                throw new NoSuchElementException();
              }
              if (dataBlock.hasNext()) {
                DataRow next = dataBlock.next();
                return new Row(baseTs + next.getRelTimeStamp(), next);
              }
              nextBlock();
           }
         }
       };
     }
   }





  public static long[] getIds(final Collection<TableDef> tableDefs) {
    long[] result = new long[tableDefs.size()];
    int i = 0;
    for (TableDef tdef : tableDefs) {
      result[i++] = tdef.getId();
    }
    return result;
  }

  public static void writeCsvTable(final File tsDB, final String tableName, final File output)
          throws IOException {
    try (Writer writer = new BufferedWriter(new OutputStreamWriter(
            Files.newOutputStream(output.toPath()), StandardCharsets.UTF_8))) {
      writeAsCsv(writer, tsDB, tableName);
    }
  }

  public static void writeAsCsv(final Appendable writer, final File tsDB, final String tableName)
          throws IOException {

    List<TableDef> tableDefs = getTableDef(tsDB, tableName);
    TimeSeries data = getTimeSeries(tsDB, getIds(tableDefs), 0, Long.MAX_VALUE);

    Csv.writeCsvElement("timestamp", writer);
    for (ColumnDef col : tableDefs.get(0).getColumns()) {
      writer.append(',');
      Csv.writeCsvElement(col.getName(), writer);
    }
    writer.append('\n');
    long[] timestamps = data.getTimeStamps();
    long[][] values = data.getValues();
    for (int i = 0; i < timestamps.length; i++) {
      Csv.writeCsvElement(DateTimeFormats.TS_FORMAT.format(Instant.ofEpochMilli(timestamps[i])), writer);
      for (long val : values[i]) {
        writer.append(',');
        Csv.writeCsvElement(Long.toString(val), writer);
      }
      writer.append('\n');
    }
  }

  public static void writeCsvTables(final File tsDB, final Set<String> tableNames, final File output)
          throws IOException {
    if (tableNames.isEmpty()) {
      return;
    }
    ListMultimap<String, TableDef> tables = getTables(tsDB, tableNames);
    try (Writer writer = new BufferedWriter(new OutputStreamWriter(
            Files.newOutputStream(output.toPath()), StandardCharsets.UTF_8))) {
      TableDef table = tables.values().iterator().next();
      Csv.writeCsvElement("table", writer);
      writer.append(',');
      Csv.writeCsvElement("timestamp", writer);
      for (ColumnDef col : table.getColumns()) {
        writer.append(',');
        Csv.writeCsvElement(col.getName(), writer);
      }
      writer.write('\n');

      for (Map.Entry<String, Collection<TableDef>> tEntry : tables.asMap().entrySet()) {

        TimeSeries data = getTimeSeries(tsDB, getIds(tEntry.getValue()), 0, Long.MAX_VALUE);
        long[] timestamps = data.getTimeStamps();
        long[][] values = data.getValues();
        for (int i = 0; i < timestamps.length; i++) {
          Csv.writeCsvElement(tEntry.getKey(), writer);
          writer.append(',');
          Csv.writeCsvElement(DateTimeFormats.TS_FORMAT.format(Instant.ofEpochMilli(timestamps[i])), writer);
          for (long val : values[i]) {
            writer.append(',');
            Csv.writeCsvElement(Long.toString(val), writer);
          }
          writer.write('\n');
        }
      }
    }
  }

  @Nullable
  public static ColumnDef getColumnDefIfExists(final TableDef td, final String columnName) {
    for (ColumnDef cdef : td.getColumns()) {
      if (Strings.equals(columnName, cdef.getName())) {
        return cdef;
      }
    }
    return null;
  }

  @Nonnull
  public static ColumnDef getColumnDef(final TableDef td, final String columnName) {
    for (ColumnDef cdef : td.getColumns()) {
      if (columnName.equals(cdef.getName())) {
        return cdef;
      }
    }
    throw new IllegalArgumentException("Column " + columnName + " not found in " + td);
  }

  public static int getColumnIndex(final TableDef td, final String columnName) {
    int i = 0;
    for (ColumnDef cdef : td.getColumns()) {
      if (columnName.equals(cdef.getName())) {
        return i;
      }
      i++;
    }
    return -1;
  }

  public static String[] getColumnNames(final TableDef td) {
    List<ColumnDef> columns = td.getColumns();
    String[] result = new String[columns.size()];
    int i = 0;
    for (ColumnDef cd : columns) {
      result[i++] = cd.getName();
    }
    return result;
  }

  public static String[] getColumnUnitsOfMeasurement(final TableDef td) {
    List<ColumnDef> columns = td.getColumns();
    String[] result = new String[columns.size()];
    int i = 0;
    for (ColumnDef cd : columns) {
      result[i++] = cd.getUnitOfMeasurement();
    }
    return result;
  }

}
