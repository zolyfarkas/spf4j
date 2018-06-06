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
import org.spf4j.perf.impl.ms.StoreType;
import org.spf4j.perf.impl.ms.graphite.GraphiteTcpStore;
import org.spf4j.perf.impl.ms.graphite.GraphiteUdpStore;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.CharSequences;
import org.spf4j.io.Csv;
import org.spf4j.io.csv.CsvParseException;
import org.spf4j.perf.CloseableMeasurementRecorder;
import org.spf4j.perf.CloseableMeasurementRecorderSource;
import org.spf4j.perf.MeasurementRecorder;
import org.spf4j.perf.MeasurementRecorderSource;
import org.spf4j.perf.MeasurementStore;
import org.spf4j.perf.MultiMeasurementRecorder;
import org.spf4j.perf.impl.ms.Flusher;
import org.spf4j.perf.impl.ms.MultiStore;
import org.spf4j.perf.impl.ms.tsdb.TSDBMeasurementStore;
import org.spf4j.recyclable.ObjectCreationException;

/**
 *
 * @author zoly
 */
public final class RecorderFactory {

  private static final Logger LOG = LoggerFactory.getLogger(RecorderFactory.class);

  public static final MeasurementStore MEASUREMENT_STORE;

  static {
    MeasurementStore mStore;
    try {
      mStore = buildStoreFromConfig(System.getProperty("spf4j.perf.ms.config", null));
    } catch (IOException | ObjectCreationException ex) {
      LOG.error("Cannot initialize measurement store, installing NOP store", ex);
      mStore = new NopMeasurementStore();
    }
    if (!(mStore instanceof NopMeasurementStore)) {
      Flusher.flushEvery(Integer.getInteger("spf4j.perf.ms.flushIntervalMillis", 60000), mStore);
    }
    MEASUREMENT_STORE = mStore;
  }

  private RecorderFactory() {
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
      return new TSDBMeasurementStore(new File(
              System.getProperty("spf4j.perf.ms.defaultTsdbFolderPath",
                      System.getProperty("java.io.tmpdir"))
              + File.separator
              + CharSequences.validatedFileName(System.getProperty("spf4j.perf.ms.defaultTsdbFileNamePrefix",
                      ManagementFactory.getRuntimeMXBean().getName() + ".tsdb2"))));
    }

    List<String> stores;
    try {
      stores = Csv.readRow(new StringReader(configuration));
    } catch (CsvParseException ex) {
      throw new IllegalArgumentException("Invalid configuration " + configuration, ex);
    }
    final int size = stores.size();
    if (size == 1) {
      return fromString(stores.get(0));
    } else {
      MeasurementStore[] mstores = new MeasurementStore[size];
      int i = 0;
      for (String config : stores) {
        mstores[i] = fromString(config);
        i++;
      }
      return new MultiStore(mstores);
    }
  }

  public static MeasurementStore fromString(final String string) throws IOException, ObjectCreationException {
    int atIdx = string.indexOf('@');
    final int length = string.length();
    if (atIdx < 0) {
      atIdx = length;
    }
    StoreType type = StoreType.valueOf(string.substring(0, atIdx));
    if (atIdx >= length) {
      return type.create("");
    } else {
      return type.create(string.substring(atIdx + 1));
    }
  }

  /**
   * Create a Quantized Measurement recorder. (see concept at http://dtrace.org/blogs/bmc/2011/02/08/llquantize/ )
   *
   * A quantized measurement recorder is appropriate for low overhead measurement recording with good detail
   * using quantized data resolution.
   * For lower overhead recorder see createScalableMinMaxAvgRecorder.
   *
   * example : createScalableQuantizedRecorder("response time", "ms", 60000, 10, -3, 3, 5)
   * will aggregate and persist measurements every minute.
   * will have the following measurement buckets:
   *
   * QNI_-1000, Q-1000_-800, Q-800_-600, Q-600_-400, Q-400_-100, Q-100_-80, Q-80_-60, Q-60_-40, Q-40_-10, Q-10_-8,
   * Q-8_-6, Q-6_-4, Q-4_-2, Q-2_0, Q0_2, Q2_4, Q4_6,
   * Q6_8, Q8_10, Q10_40, Q40_60, Q60_80, Q80_100, Q100_400, Q400_600, Q600_800, Q800_1000, Q1000_PI
   *
   * where A_B equivalent Math notation is [A,B)
   * where NI = negative infinity
   * where PI is positive infinity
   *
   * @param forWhat an object identifying what is being measured, ex: "response time"
   * @param unitOfMeasurement the unit of measurement of the measurements, ex "milliseconds"
   * @param sampleTimeMillis the sampling (accumulating interval) ex: 60000 for minute level detail.
   * @param factor the log factor of the magnitudes, ex: 10 for 0-1,1-10,10-100,100 - 1000 magnitudes.
   * @param lowerMagnitude the lowest magnitude. ex: 10 for 10 * factor min bucket limit value.
   * @param higherMagnitude th highest magnitude. ex 10 for 10 * factor max bucket limit value.
   * @param quantasPerMagnitude number of equally divided measurement buckets per magnitude. ex: 10
   * @return a measurement recorder that
   */
  public static MeasurementRecorder createScalableQuantizedRecorder(
          final Object forWhat, final String unitOfMeasurement, final int sampleTimeMillis,
          final int factor, final int lowerMagnitude,
          final int higherMagnitude, final int quantasPerMagnitude) {
    ScalableMeasurementRecorder mr = new ScalableMeasurementRecorder(new QuantizedAccumulator(forWhat, "",
            unitOfMeasurement, factor, lowerMagnitude, higherMagnitude,
            quantasPerMagnitude), sampleTimeMillis, MEASUREMENT_STORE, true);
    mr.registerJmx();
    return mr;
  }

  public static CloseableMeasurementRecorder createScalableQuantizedRecorder2(
          final Object forWhat, final String unitOfMeasurement,  final int sampleTimeMillis,
          final int factor, final int lowerMagnitude, final int higherMagnitude,
          final int quantasPerMagnitude) {
    ScalableMeasurementRecorder mr = new ScalableMeasurementRecorder(new QuantizedAccumulator(forWhat, "",
            unitOfMeasurement, factor, lowerMagnitude, higherMagnitude,
            quantasPerMagnitude), sampleTimeMillis, MEASUREMENT_STORE, false);
    mr.registerJmx();
    return mr;
  }

  public static MeasurementRecorder createScalableCountingRecorder(
          final Object forWhat, final String unitOfMeasurement, final int sampleTimeMillis) {
    ScalableMeasurementRecorder mr = new ScalableMeasurementRecorder(new CountingAccumulator(forWhat, "",
            unitOfMeasurement), sampleTimeMillis, MEASUREMENT_STORE, true);
    mr.registerJmx();
    return mr;
  }

  public static MeasurementRecorder createScalableMinMaxAvgRecorder(
          final Object forWhat, final String unitOfMeasurement, final int sampleTimeMillis) {
    ScalableMeasurementRecorder mr = new ScalableMeasurementRecorder(new MinMaxAvgAccumulator(forWhat, "",
            unitOfMeasurement), sampleTimeMillis, MEASUREMENT_STORE, true);
    mr.registerJmx();
    return mr;
  }

  public static CloseableMeasurementRecorder createScalableMinMaxAvgRecorder2(
          final Object forWhat, final String unitOfMeasurement, final int sampleTimeMillis) {
    ScalableMeasurementRecorder mr = new ScalableMeasurementRecorder(new MinMaxAvgAccumulator(forWhat, "",
            unitOfMeasurement), sampleTimeMillis, MEASUREMENT_STORE, false);
    mr.registerJmx();
    return mr;
  }

  public static MeasurementRecorderSource createScalableQuantizedRecorderSource(
          final Object forWhat, final String unitOfMeasurement, final int sampleTimeMillis,
          final int factor, final int lowerMagnitude,
          final int higherMagnitude, final int quantasPerMagnitude) {
    ScalableMeasurementRecorderSource mrs = new ScalableMeasurementRecorderSource(
            new QuantizedAccumulator(forWhat, "",
                    unitOfMeasurement, factor, lowerMagnitude, higherMagnitude, quantasPerMagnitude),
            sampleTimeMillis, MEASUREMENT_STORE, true);
    mrs.registerJmx();
    return mrs;
  }

  public static CloseableMeasurementRecorderSource createScalableQuantizedRecorderSource2(final Object forWhat,
          final String unitOfMeasurement,  final int sampleTimeMillis,
          final int factor, final int lowerMagnitude, final int higherMagnitude,
          final int quantasPerMagnitude) {
    ScalableMeasurementRecorderSource mrs = new ScalableMeasurementRecorderSource(
            new QuantizedAccumulator(forWhat, "",
                    unitOfMeasurement, factor, lowerMagnitude, higherMagnitude, quantasPerMagnitude),
            sampleTimeMillis, MEASUREMENT_STORE, false);
    mrs.registerJmx();
    return mrs;
  }

  public static MeasurementRecorderSource createScalableCountingRecorderSource(
          final Object forWhat, final String unitOfMeasurement, final int sampleTimeMillis) {
    ScalableMeasurementRecorderSource mrs = new ScalableMeasurementRecorderSource(
            new CountingAccumulator(forWhat, "",
                    unitOfMeasurement), sampleTimeMillis, MEASUREMENT_STORE, true);
    mrs.registerJmx();
    return mrs;
  }

  public static CloseableMeasurementRecorderSource createScalableCountingRecorderSource2(
          final Object forWhat, final String unitOfMeasurement, final int sampleTimeMillis) {
    ScalableMeasurementRecorderSource mrs = new ScalableMeasurementRecorderSource(
            new CountingAccumulator(forWhat, "",
                    unitOfMeasurement), sampleTimeMillis, MEASUREMENT_STORE, false);
    mrs.registerJmx();
    return mrs;
  }

  public static MeasurementRecorderSource createScalableMinMaxAvgRecorderSource(
          final Object forWhat, final String unitOfMeasurement, final int sampleTimeMillis) {
    ScalableMeasurementRecorderSource mrs = new ScalableMeasurementRecorderSource(
            new MinMaxAvgAccumulator(forWhat, "",
                    unitOfMeasurement), sampleTimeMillis, MEASUREMENT_STORE, true);
    mrs.registerJmx();
    return mrs;
  }

  public static MultiMeasurementRecorder createDirectRecorder(final Object measuredEntity, final String description,
          final String[] measurementNames, final String[] measurementUnits) {
    DirectStoreMultiAccumulator mr = new DirectStoreMultiAccumulator(
            new MeasurementsInfoImpl(measuredEntity, description,
                    measurementNames, measurementUnits), MEASUREMENT_STORE);
    mr.registerJmx();
    return mr;
  }

  public static MeasurementRecorder createDirectRecorder(final Object forWhat, final String unitOfMeasurement) {
    DirectStoreAccumulator mr = new DirectStoreAccumulator(forWhat, "", unitOfMeasurement, 0, MEASUREMENT_STORE);
    mr.registerJmx();
    return mr;
  }

  public static MeasurementRecorder createDirectRecorder(final Object forWhat,
          final String unitOfMeasurement, final int sampleTimeMillis) {
    DirectStoreAccumulator mr = new DirectStoreAccumulator(
            forWhat, "", unitOfMeasurement, sampleTimeMillis, MEASUREMENT_STORE);
    mr.registerJmx();
    return mr;
  }

  public static MeasurementRecorderSource createDirectRecorderSource(final Object forWhat,
          final String unitOfMeasurement) {
    return new DirectRecorderSource(forWhat, "", unitOfMeasurement, 0, MEASUREMENT_STORE);
  }

  public static MeasurementRecorder createDirectGraphiteUdpRecorder(final Object forWhat,
          final String unitOfMeasurement,
          final String graphiteHost, final int graphitePort) throws ObjectCreationException {
    DirectStoreAccumulator mr = new DirectStoreAccumulator(forWhat, "", unitOfMeasurement, 0,
            new GraphiteUdpStore(graphiteHost, graphitePort));
    mr.registerJmx();
    return mr;
  }

  public static MeasurementRecorder createDirectGraphiteTcpRecorder(final Object forWhat,
          final String unitOfMeasurement,
          final String graphiteHost, final int graphitePort) throws ObjectCreationException {
    DirectStoreAccumulator mr = new DirectStoreAccumulator(forWhat, "", unitOfMeasurement, 0,
            new GraphiteTcpStore(graphiteHost, graphitePort));
    mr.registerJmx();
    return mr;
  }
}
