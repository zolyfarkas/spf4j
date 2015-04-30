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

import com.google.common.primitives.Longs;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.spf4j.base.Either;
import org.spf4j.base.Strings;
import org.spf4j.perf.tsdb.TimeSeries;
import org.spf4j.tsdb2.avro.DataBlock;
import org.spf4j.tsdb2.avro.DataRow;
import org.spf4j.tsdb2.avro.TableDef;

/**
 *
 * @author zoly
 */
public final class TSDBQuery {

    private TSDBQuery() { }

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

}
