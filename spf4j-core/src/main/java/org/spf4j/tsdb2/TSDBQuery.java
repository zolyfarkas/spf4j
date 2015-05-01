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
package org.spf4j.tsdb2;

import com.google.common.base.Charsets;
import com.google.common.primitives.Longs;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.spf4j.base.Either;
import org.spf4j.base.Strings;
import org.spf4j.io.Csv;
import org.spf4j.tsdb2.avro.ColumnDef;
import org.spf4j.tsdb2.avro.DataBlock;
import org.spf4j.tsdb2.avro.DataRow;
import org.spf4j.tsdb2.avro.TableDef;

/**
 *
 * @author zoly
 */
public final class TSDBQuery {

    private TSDBQuery() {
    }

    public static List<TableDef> getAllTables(final File tsdbFile) throws IOException {
        List<TableDef> result = new ArrayList<>();
        try (TSDBReader reader = new TSDBReader(tsdbFile, 8192)) {
            Either<TableDef, DataBlock> read;
            while ((read = reader.read()) != null) {
                if (read.isLeft()) {
                    result.add(read.getLeft());
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

    }

    public static Collection<TableDefEx> getAllTablesWithDataRanges(final File tsdbFile) throws IOException {
        List<TableDefEx> result = new ArrayList<>();
        TLongObjectMap<TableDefEx> id2Def = new TLongObjectHashMap<>();
        try (TSDBReader reader = new TSDBReader(tsdbFile, 8192)) {
            Either<TableDef, DataBlock> read;
            while ((read = reader.read()) != null) {
                if (read.isLeft()) {
                    final TableDef left = read.getLeft();
                    final TableDefEx tableDefEx = new TableDefEx(left, Long.MAX_VALUE, 0L);
                    id2Def.put(left.id, tableDefEx);
                    result.add(tableDefEx);
                } else {
                    DataBlock right = read.getRight();
                    long baseTs = right.baseTimestamp;
                    for (DataRow row : right.getValues()) {
                        TableDefEx tdex = id2Def.get(row.tableDefId);
                        if (tdex == null) {
                            throw new IOException("Potentially corupted file data row with no tableDef " + row);
                        }
                        long ts = baseTs + row.relTimeStamp;
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

    public static TableDef getTableDef(final File tsdbFile, final String tableName) throws IOException {
        try (TSDBReader reader = new TSDBReader(tsdbFile, 8192)) {
            Either<TableDef, DataBlock> read;
            while ((read = reader.read()) != null) {
                if (read.isLeft()) {
                    TableDef left = read.getLeft();
                    if (Strings.equals(tableName, left.name)) {
                        return left;
                    }
                }
            }
        }
        return null;
    }

    public static TimeSeries getTimeSeries(final File tsdbFile, final long tableId,
            final long startTimeMillis, final long endTimeMillis) throws IOException {
        TLongList timestamps = new TLongArrayList();
        List<long[]> metrics = new ArrayList<>();
        try (TSDBReader reader = new TSDBReader(tsdbFile, 8192)) {
            Either<TableDef, DataBlock> read;
            while ((read = reader.read()) != null) {
                if (read.isRight()) {
                    DataBlock data = read.getRight();
                    long baseTs = data.baseTimestamp;
                    for (DataRow row : data.getValues()) {
                        if (tableId == row.tableDefId) {
                            final long ts = baseTs + row.relTimeStamp;
                            if (ts >= startTimeMillis && ts <= endTimeMillis) {
                                timestamps.add(ts);
                                metrics.add(Longs.toArray(row.data));
                            }
                        }
                    }
                }
            }
        }
        return new TimeSeries(timestamps.toArray(), metrics.toArray(new long[metrics.size()][]));
    }

    public static void writeCsvTable(final File tsDB, final String tableName, final File output)
            throws IOException {

        TableDef tableDef = getTableDef(tsDB, tableName);

        TimeSeries data = getTimeSeries(tsDB, tableDef.id, 0, Long.MAX_VALUE);
        DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), Charsets.UTF_8))) {
            Csv.writeCsvElement("timestamp", writer);
            for (ColumnDef col : tableDef.getColumns()) {
                writer.append(',');
                Csv.writeCsvElement(col.getName(), writer);
            }
            writer.write('\n');
            long[] timestamps = data.getTimeStamps();
            long[][] values = data.getValues();
            for (int i = 0; i < timestamps.length; i++) {
                Csv.writeCsvElement(formatter.print(timestamps[i]), writer);
                for (long val : values[i]) {
                    writer.append(',');
                    Csv.writeCsvElement(Long.toString(val), writer);
                }
                writer.write('\n');
            }
        }
    }

    public static void writeCsvTables(final File tsDB, final List<String> tableNames, final File output)
            throws IOException {
        DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), Charsets.UTF_8))) {
            String firstTable = tableNames.get(0);
            TableDef table = getTableDef(tsDB, firstTable);
            Csv.writeCsvElement("table", writer);
            writer.append(',');
            Csv.writeCsvElement("timestamp", writer);
            for (ColumnDef col : table.getColumns()) {
                writer.append(',');
                Csv.writeCsvElement(col.getName(), writer);
            }
            writer.write('\n');

            for (String tableName : tableNames) {
                TableDef td = getTableDef(tsDB, firstTable);
                TimeSeries data = getTimeSeries(tsDB, td.id, 0, Long.MAX_VALUE);
                long[] timestamps = data.getTimeStamps();
                long[][] values = data.getValues();
                for (int i = 0; i < timestamps.length; i++) {
                    Csv.writeCsvElement(tableName, writer);
                    writer.append(',');
                    Csv.writeCsvElement(formatter.print(timestamps[i]), writer);
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
            if (Strings.equals(columnName, cdef.getName())) {
                return cdef;
            }
        }
        throw new IllegalArgumentException("Column " + columnName + " not found in " + td);
    }


    public static int getColumnIndex(final TableDef td, final String columnName) {
        int i = 0;
        for (ColumnDef cdef : td.getColumns()) {
            if (Strings.equals(columnName, cdef.getName())) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public static String [] getColumnNames(final TableDef td) {
        List<ColumnDef> columns = td.getColumns();
        String [] result = new String [columns.size()];
        int i = 0;
        for (ColumnDef cd : columns) {
            result[i++] = cd.getName();
        }
        return result;
    }

    public static String [] getColumnUnitsOfMeasurement(final TableDef td) {
        List<ColumnDef> columns = td.getColumns();
        String [] result = new String [columns.size()];
        int i = 0;
        for (ColumnDef cd : columns) {
            result[i++] = cd.getUnitOfMeasurement();
        }
        return result;
    }

}
