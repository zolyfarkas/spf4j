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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.avro.Schema;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.avro.AvroCloseableIterable;
import org.spf4j.perf.MeasurementStoreQuery;
import org.spf4j.perf.TimeSeriesRecord;
import org.spf4j.perf.impl.MeasurementsInfoImpl;
import org.spf4j.tsdb2.avro.Aggregation;
import org.spf4j.tsdb2.avro.MeasurementType;

/**
 *
 * @author Zoltan Farkas
 */
public class AvroMeasurementStoreTest {

  private static final Logger LOG = LoggerFactory.getLogger(AvroMeasurementStoreTest.class);

  @Test
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public void testStore() throws IOException {
    AvroMeasurementStore store = new AvroMeasurementStore(org.spf4j.base.Runtime.TMP_FOLDER_PATH,
            "testMetrics", false);
    try {
      long mid = store.alocateMeasurements(new MeasurementsInfoImpl("test", "test", new String[]{"v1", "v2"},
              new String[]{"t1", "t2"},
              new Aggregation[]{Aggregation.SUM, Aggregation.LAST},
              MeasurementType.GAUGE), 1000);
      store.saveMeasurements(mid, 0L, 1, 2);

      store.saveMeasurements(mid, 1000L, 3, 4);

      store.saveMeasurements(mid, 2000L, 5, 6);

      store.saveMeasurements(mid, 2000L, 7, 8);

      store.saveMeasurements(mid, 3000L, 9, 10);

      store.saveMeasurements(mid, 4000L, 11, 12);

      store.flush();

      MeasurementStoreQuery query = store.query();
      Collection<Schema> measurements = query.getMeasurements((x) -> true);
      Schema metric = measurements.iterator().next();
      Assert.assertEquals("test", metric.getName());
      List<TimeSeriesRecord> results = getMetrics(query, metric, Instant.EPOCH, Instant.now());
      Assert.assertEquals(6, results.size());
      TimeSeriesRecord rec = results.get(0);
      Assert.assertEquals(Instant.ofEpochMilli(0L), rec.getTimeStamp());
      Assert.assertEquals(1L, rec.getLongValue("v1"));
      Assert.assertEquals(2L, rec.getLongValue("v2"));
      rec = results.get(5);
      Assert.assertEquals(Instant.ofEpochMilli(4000L), rec.getTimeStamp());
      Assert.assertEquals(11L, rec.getLongValue("v1"));
      Assert.assertEquals(12L, rec.getLongValue("v2"));
      results = getMetrics(query, metric, Instant.ofEpochMilli(2000L), Instant.ofEpochMilli(2000L));
      Assert.assertEquals(2, results.size());
      rec = results.get(0);
      Assert.assertEquals(Instant.ofEpochMilli(2000L), rec.getTimeStamp());
      Assert.assertEquals(5L, rec.getLongValue("v1"));
      Assert.assertEquals(6L, rec.getLongValue("v2"));
      rec = results.get(1);
      Assert.assertEquals(Instant.ofEpochMilli(2000L), rec.getTimeStamp());
      Assert.assertEquals(7L, rec.getLongValue("v1"));
      Assert.assertEquals(8L, rec.getLongValue("v2"));

      List<TimeSeriesRecord> aggr = getMetrics(query, metric, Instant.EPOCH, Instant.now(), 1000);
      Assert.assertEquals(5, aggr.size());
      rec = aggr.get(0);
      Assert.assertEquals(Instant.ofEpochMilli(0L), rec.getTimeStamp());
      Assert.assertEquals(1L, rec.getLongValue("v1"));
      Assert.assertEquals(2L, rec.getLongValue("v2"));
      rec = aggr.get(2);
      Assert.assertEquals(Instant.ofEpochMilli(2000L), rec.getTimeStamp());
      Assert.assertEquals(12L, rec.getLongValue("v1"));
      Assert.assertEquals(8L, rec.getLongValue("v2"));

      aggr = getMetrics(query, metric, Instant.EPOCH, Instant.now(), 1500);
      Assert.assertEquals(5, aggr.size());

      aggr = getMetrics(query, metric, Instant.EPOCH, Instant.now(), 0);
      Assert.assertEquals(6, aggr.size());

      try (AvroCloseableIterable<TimeSeriesRecord> it = query.getAggregatedMeasurementData(metric,
              Instant.EPOCH, Instant.now(),
              Duration.between(Instant.EPOCH, Instant.now()).getSeconds(), TimeUnit.SECONDS)) {
        Iterator<TimeSeriesRecord> iterator = it.iterator();
        TimeSeriesRecord next = iterator.next();
        LOG.debug("The aggregate", next);
        Assert.assertFalse(iterator.hasNext());
        Assert.assertEquals(36L, next.getLongValue("v1"));
        Assert.assertEquals(12L, next.getLongValue("v2"));
      }
    } finally {
      store.close();
      Files.delete(store.getInfoFile());
      Files.delete(store.getDataFile());
    }

  }

  public static List<TimeSeriesRecord> getMetrics(final MeasurementStoreQuery query,
          final Schema metric, final Instant from, final Instant to) throws IOException {
    List<TimeSeriesRecord> results = new ArrayList<>();
    try (AvroCloseableIterable<TimeSeriesRecord> data = query.getMeasurementData(metric, from, to)) {
      for (TimeSeriesRecord rec : data) {
        LOG.debug("data", rec);
        results.add(rec);
      }
    }
    return results;
  }

  public static List<TimeSeriesRecord> getMetrics(final MeasurementStoreQuery query,
          final Schema metric, final Instant from, final Instant to, final int aggMillis) throws IOException {
    List<TimeSeriesRecord> results = new ArrayList<>();
    try (AvroCloseableIterable<TimeSeriesRecord> data = query.getAggregatedMeasurementData(metric, from, to,
            aggMillis, TimeUnit.MILLISECONDS)) {
      for (TimeSeriesRecord rec : data) {
        LOG.debug("agg", rec);
        results.add(rec);
      }
    }
    return results;
  }

}
