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

import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Collection;
import org.apache.avro.Schema;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.avro.AvroCloseableIterable;
import org.spf4j.perf.MeasurementStoreQuery;
import org.spf4j.perf.TimeSeriesRecord;
import org.spf4j.perf.impl.MeasurementsInfoImpl;
import org.spf4j.tsdb2.avro.MeasurementType;

/**
 *
 * @author Zoltan Farkas
 */
public class AvroMeasurementStoreTest {

  private static final Logger LOG = LoggerFactory.getLogger(AvroMeasurementStoreTest.class);

  @Test
  public void testStore() throws IOException {
    AvroMeasurementStore store = new AvroMeasurementStore(org.spf4j.base.Runtime.TMP_FOLDER_PATH,
          "testMetrics", false);
    long mid = store.alocateMeasurements(new MeasurementsInfoImpl("test", "test", new String[] {"v1", "v2"},
            new String[] {"t1", "t2"}, MeasurementType.GAUGE), 1000);
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
    try (AvroCloseableIterable<TimeSeriesRecord> data
            = query.getMeasurementData(metric, Instant.EPOCH, Instant.now())) {
      for (TimeSeriesRecord rec : data) {
        LOG.debug("data", rec);
      }
    }
    store.close();
    Files.delete(store.getInfoFile());
    Files.delete(store.getDataFile());

  }

}
