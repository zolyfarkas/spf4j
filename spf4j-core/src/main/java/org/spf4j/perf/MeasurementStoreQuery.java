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
package org.spf4j.perf;

import com.google.common.annotations.Beta;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.apache.avro.Schema;
import org.spf4j.base.avro.AvroCloseableIterable;
import org.spf4j.tsdb2.TableDefs;
import org.spf4j.tsdb2.avro.Observation;

/**
 *
 * @author Zoltan Farkas
 */
public interface MeasurementStoreQuery {

  Collection<Schema> getMeasurements(Predicate<String> filter) throws IOException;

  AvroCloseableIterable<Observation> getObservations() throws IOException;

  default AvroCloseableIterable<Observation> getObservations(final Schema measurement,
          final Instant from, final Instant to) throws IOException {
    @SuppressWarnings("unchecked")
    Collection<Long> mids = (Collection<Long>) measurement.getObjectProp("ids");
    @SuppressWarnings("unchecked")
    long fromMs = from.toEpochMilli();
    long toMs = to.toEpochMilli();
    AvroCloseableIterable<Observation> observations = getObservations();
    Iterable<Observation> filtered = Iterables.filter(observations, (Observation row) -> {
      long ts = row.getRelTimeStamp();
      return ts >= fromMs && ts <= toMs && mids.contains(row.getTableDefId());
    });
    return AvroCloseableIterable.from(filtered, observations, measurement);
  }

  default AvroCloseableIterable<Observation> getAggregatedObservations(final Schema measurement,
          final Instant from, final Instant to, final int aggFreq,  final TimeUnit tu) throws IOException {
    long aggTime = tu.toMillis(aggFreq);
    AvroCloseableIterable<Observation> observations = getObservations(measurement, from, to);
     return AvroCloseableIterable.from(() -> new TimeSeriesAggregatingIterator<>(observations,
             Observation::getRelTimeStamp,
            (a, b) -> TimeSeriesRecord.accumulateObservations(measurement, a, b), aggTime),
            observations, measurement);
  }

  /**
   * Query measurement data.
   * @param measurement
   * @param from
   * @param to
   * @return data iterable
   * @throws IOException
   */
  default AvroCloseableIterable<TimeSeriesRecord> getMeasurementData(final Schema measurement,
          final Instant from, final Instant to) throws IOException {
    AvroCloseableIterable<Observation> observations = getObservations(measurement, from, to);
    Iterable<TimeSeriesRecord> tsr = Iterables.transform(observations,
             (obs) -> TableDefs.toRecord(measurement, obs));
    return AvroCloseableIterable.from(tsr, observations, measurement);
  }

  @Beta
  default AvroCloseableIterable<TimeSeriesRecord> getAggregatedMeasurementData(
          final Schema measurement,
          final Instant from, final Instant to,
          final int aggFreq,  final TimeUnit tu) throws IOException {
    long aggTime = tu.toMillis(aggFreq);
    AvroCloseableIterable<TimeSeriesRecord> iterable = getMeasurementData(measurement, from, to);
    return AvroCloseableIterable.from(() -> new TimeSeriesAggregatingIterator<>(iterable,
            (TimeSeriesRecord rec) -> rec.getTimeStamp().toEpochMilli(),
            TimeSeriesRecord::accumulate, aggTime),
            iterable, measurement);
  }

}
