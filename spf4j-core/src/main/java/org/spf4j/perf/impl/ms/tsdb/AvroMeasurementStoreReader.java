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

import com.google.common.collect.Iterables;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.THashSet;
import gnu.trove.set.hash.TLongHashSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.specific.SpecificDatumReader;
import org.spf4j.base.avro.AvroCloseableIterable;
import org.spf4j.perf.MeasurementStoreQuery;
import org.spf4j.perf.TimeSeriesRecord;
import org.spf4j.tsdb2.TableDefs;
import org.spf4j.tsdb2.avro.Observation;
import org.spf4j.tsdb2.avro.TableDef;

/**
 *
 * @author Zoltan Farkas
 */
public final class AvroMeasurementStoreReader implements MeasurementStoreQuery {

  private final Path infoFile;

  private final Path dataFile;

  public AvroMeasurementStoreReader(final Path infoFile, final Path dataFile) {
    this.infoFile = infoFile;
    this.dataFile = dataFile;
  }

  @Override
  public Set<String> getMeasurements() throws IOException {
    Set<String> result = new THashSet<>();
    try (DataFileStream<TableDef> stream = new DataFileStream<TableDef>(Files.newInputStream(infoFile),
            new SpecificDatumReader<>(TableDef.class))) {
      for (TableDef td : stream) {
        result.add(td.getName());
      }
    }
    return result;
  }

  @Override
  @Nullable
  public AvroCloseableIterable<TimeSeriesRecord> getMeasurementData(final String measurement,
          final Instant from, final Instant to) throws IOException {
    TLongSet mids = new TLongHashSet(4, 0.7f, -1L);
    TableDef def = null;
    try (DataFileStream<TableDef> stream = new DataFileStream<TableDef>(Files.newInputStream(infoFile),
            new SpecificDatumReader<>(TableDef.class))) {
      for (TableDef td : stream) {
        if (measurement.equals(td.getName())) {
          mids.add(td.getId());
          if (def != null && !td.getColumns().equals(def.getColumns())) {
            throw new IllegalStateException("Invalid measurement table redefinition " + def + " != " + td);
          }
          def = td;
        }
      }
    }
    if (def == null) {
      return null;
    }
    Schema schema = TableDefs.createSchema(def);
    DataFileStream<Observation> stream = new DataFileStream<Observation>(Files.newInputStream(dataFile),
            new SpecificDatumReader<>(Observation.class));
    long fileTimeRef = stream.getMetaLong("timeRef");
    long fromMs = from.toEpochMilli();
    long toMs = to.toEpochMilli();
    Iterable<Observation> filtered = Iterables.filter(stream, (Observation row) -> {
      long ts = fileTimeRef + row.getRelTimeStamp();
      return ts >= fromMs && ts <= toMs && mids.contains(row.getTableDefId());
    });
    return AvroCloseableIterable.from(Iterables.transform(filtered,
            (obs) -> TableDefs.toRecord(schema, fileTimeRef, obs)),
            stream, schema);
  }

  @Override
  @Nullable
  public Schema getMeasurementSchema(final String measurement) throws IOException {
    TableDef def = null;
    try (DataFileStream<TableDef> stream = new DataFileStream<TableDef>(Files.newInputStream(infoFile),
            new SpecificDatumReader<>(TableDef.class))) {
      for (TableDef td : stream) {
        if (measurement.equals(td.getName())) {
          if (def != null && !td.getColumns().equals(def.getColumns())) {
            throw new IllegalStateException("Invalid measurement table redefinition " + def + " != " + td);
          }
          def = td;
        }
      }
    }
    if (def == null) {
      return null;
    }
    return TableDefs.createSchema(def);
  }

  @Override
  public String toString() {
    return "AvroMeasurementStoreReader{" + "infoFile=" + infoFile + ", dataFile=" + dataFile + '}';
  }


}
