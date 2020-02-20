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

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.avro.Schema;
import org.spf4j.base.Pair;
import org.spf4j.base.avro.AvroCloseableIterable;
import org.spf4j.perf.MeasurementStoreQuery;
import org.spf4j.perf.TimeSeriesRecord;
import org.spf4j.tsdb2.TSDBQuery;
import org.spf4j.tsdb2.TableDefs;
import org.spf4j.tsdb2.avro.TableDef;

/**
 * @author Zoltan Farkas
 */
public final class TSDBMeasurementStoreReader implements MeasurementStoreQuery {

  private final File dbFile;

  public TSDBMeasurementStoreReader(final File dbFile) {
    this.dbFile = dbFile;
  }

  @Override
  public Collection<Schema> getMeasurements(final Predicate<String> filter) throws IOException {
    ListMultimap<String, TableDef> allTables = TSDBQuery.getAllTables(dbFile);
    Map<String, Pair<Schema, Set<Long>>> schemas = Maps.newHashMapWithExpectedSize(allTables.size());
    for (Map.Entry<String, TableDef> entry : allTables.entries()) {
      String key = entry.getKey();
      if (filter.test(key)) {
        Pair<Schema, Set<Long>> exSch = schemas.get(key);
        TableDef td = entry.getValue();
        if (exSch == null) {
          Schema sch = TableDefs.createSchema(td);
          Set<Long> ids = new HashSet<>(2);
          ids.add(td.getId());
          exSch = Pair.of(sch, ids);
          schemas.put(key, exSch);
        } else {
          exSch.getValue().add(td.getId());
        }
      }
    }
    return schemas.values().stream().map((x) -> {
      Schema sch = x.getKey();
      sch.addProp("ids", x.getValue());
      return sch;
    }).collect(Collectors.toCollection(() -> new ArrayList<>(schemas.size())));
  }

  @Override
  @Nullable
  public AvroCloseableIterable<TimeSeriesRecord> getMeasurementData(final Schema measurement,
          final Instant from, final Instant to) throws IOException {
    return TSDBQuery.getTimeSeriesData(dbFile, from.toEpochMilli(), to.toEpochMilli(),
            (Collection<Long>) measurement.getObjectProp("ids"), measurement);
  }

  @Override
  public String toString() {
    return "TSDBMeasurementStoreReader{" + "dbFile=" + dbFile + '}';
  }

}
