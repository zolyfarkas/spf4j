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
package org.spf4j.perf.impl.ms.tsdb;

import org.spf4j.perf.MeasurementsInfo;
import org.spf4j.perf.MeasurementStore;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import javax.annotation.concurrent.ThreadSafe;
import org.spf4j.jmx.JmxExport;
import org.spf4j.perf.MeasurementStoreQuery;
import org.spf4j.tsdb2.TSDBQuery;
import org.spf4j.tsdb2.TSDBWriter;
import org.spf4j.tsdb2.TableDefs;

/**
 *
 * @author zoly
 */
@ThreadSafe
public final class TSDBMeasurementStore
        implements MeasurementStore {

  private final TSDBWriter database;

  private final TSDBMeasurementStoreReader reader;

  public TSDBMeasurementStore(final File databaseFile) throws IOException {
    this.database = new TSDBWriter(databaseFile, 1024, "", false);
    reader = new TSDBMeasurementStoreReader(databaseFile);
  }

  @Override
  public long alocateMeasurements(final MeasurementsInfo measurement,
          final int sampleTimeMillis) throws IOException {

    return database.writeTableDef(TableDefs.from(measurement, sampleTimeMillis, -1L));
  }

  @Override
  public void saveMeasurements(final long tableId,
          final long timeStampMillis, final long... measurements)
          throws IOException {
    database.writeDataRow(tableId, timeStampMillis, measurements);
  }

  @Override
  public void close() throws IOException {
    database.close();
  }

  @JmxExport(description = "flush out buffers")
  @Override
  public void flush() throws IOException {
    database.flush();
  }

  @JmxExport(description = "list all tables")
  public String[] getTables() throws IOException {
    final Set<String> metrics = TSDBQuery.getAllTables(database.getFile()).keySet();
    return metrics.toArray(new String[metrics.size()]);
  }

  @JmxExport(description = "getTable As Csv")
  public String getTableAsCsv(@JmxExport("tableName") final String tableName) throws IOException {
    StringBuilder result = new StringBuilder(1024);
    TSDBQuery.writeAsCsv(result, database.getFile(), tableName);
    return result.toString();
  }

  @Override
  public String toString() {
    return "TSDBMeasurementStore{" + "database=" + database + '}';
  }

  public TSDBWriter getDBWriter() {
    return database;
  }

  @Override
  public MeasurementStoreQuery query() {
    return reader;
  }

}
