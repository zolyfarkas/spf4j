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
package org.spf4j.perf.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.spf4j.base.CharSequences;
import org.spf4j.io.Csv;
import org.spf4j.io.csv.CsvParseException;
import org.spf4j.perf.MeasurementStore;
import org.spf4j.perf.impl.ms.Flusher;
import org.spf4j.perf.impl.ms.MultiStore;
import org.spf4j.perf.impl.ms.StoreType;
import org.spf4j.perf.impl.ms.tsdb.AvroMeasurementStore;
import org.spf4j.recyclable.ObjectCreationException;

/**
 *
 * @author Zoltan Farkas
 */
public final class ProcessMeasurementStore {

  private static final MeasurementStore MEASUREMENT_STORE;

  static {
    MeasurementStore mStore;
    try {
      mStore = buildStoreFromConfig(System.getProperty("spf4j.perf.ms.config", null));
    } catch (IOException | ObjectCreationException ex) {
      Logger.getLogger(ProcessMeasurementStore.class.getName())
              .log(Level.SEVERE, "Cannot initialize measurement store, installing NOP store", ex);
      mStore = new NopMeasurementStore();
    }
    if (!(mStore instanceof NopMeasurementStore) && Boolean.getBoolean("spf4j.perf.ms.periodicFlush")) {
      Flusher.flushEvery(Integer.getInteger("spf4j.perf.ms.flushIntervalMillis", 60000), mStore);
    }
    MEASUREMENT_STORE = mStore;
  }

  private ProcessMeasurementStore() {
  }

  public static MeasurementStore getMeasurementStore() {
    return MEASUREMENT_STORE;
  }

  /**
   * Configuration is a coma separated list of stores:
   * TSDB@/path/to/file.tsdb,TSDB_TXT@/path/to/file.tsdbtxt,GRAPHITE_UDP@1.1.1.1:8080,GRAPHITE_TCP@1.1.1.1:8080
   *
   * @param configuration
   * @return a measurement store.
   */
  @Nonnull
  @SuppressFBWarnings("PATH_TRAVERSAL_IN") // the config is not supplied by a user.
  private static MeasurementStore buildStoreFromConfig(@Nullable final String configuration)
          throws IOException, ObjectCreationException {

    if (configuration == null || configuration.trim().isEmpty()) {
      return new AvroMeasurementStore(Paths.get(System.getProperty("spf4j.perf.ms.defaultTsdbFolderPath",
              System.getProperty("java.io.tmpdir"))),
              CharSequences.validatedFileName(System.getProperty("spf4j.perf.ms.defaultTsdbFileNamePrefix",
                      ManagementFactory.getRuntimeMXBean().getName())));
    }

    List<String> stores;
    try {
      stores = Csv.readRow(new StringReader(configuration));
    } catch (CsvParseException ex) {
      throw new IllegalArgumentException("Invalid configuration " + configuration, ex);
    }
    final int size = stores.size();
    if (size == 1) {
      return StoreType.fromString(stores.get(0));
    } else {
      MeasurementStore[] mstores = new MeasurementStore[size];
      int i = 0;
      for (String config : stores) {
        mstores[i] = StoreType.fromString(config);
        i++;
      }
      return new MultiStore(mstores);
    }
  }

}
