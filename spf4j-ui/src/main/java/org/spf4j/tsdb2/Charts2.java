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
package org.spf4j.tsdb2;

import com.google.common.primitives.Longs;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.avro.Schema;
import org.jfree.chart.JFreeChart;
import org.spf4j.base.Arrays;
import org.spf4j.base.Pair;
import org.spf4j.base.avro.AvroCloseableIterable;
import org.spf4j.perf.MeasurementStoreQuery;
import org.spf4j.perf.TimeSeriesRecord;
import static org.spf4j.perf.impl.chart.Charts.fillGaps;
import org.spf4j.tsdb2.avro.Observation;

/**
 *
 * @author zoly
 */
public final class Charts2 {

  private Charts2() {
  }

  public static boolean canGenerateMinMaxAvgCount(final Schema info) {
    int found = 0;
    for (Schema.Field colDef : info.getFields()) {
      switch (colDef.name()) {
        case "min":
        case "max":
        case "total":
        case "count":
          found++;
          break;
        default:
      }
    }
    return found >= 4;
  }

  public static boolean canGenerateCount(final Schema info) {
    int found = 0;
    for (Schema.Field colDef : info.getFields()) {
      switch (colDef.name()) {
        case "count":
          found++;
          break;
        default:
      }
    }
    return found >= 1;
  }

  @SuppressFBWarnings("STT_STRING_PARSING_A_FIELD")
  public static boolean canGenerateHeatChart(final Schema info) {
    for (Schema.Field colDef : info.getFields()) {
      String colName = colDef.name();
      if (colName.startsWith("Q") && colName.contains("_")) {
        return true;
      }
    }
    return false;
  }

  public static JFreeChart createHeatJFreeChart(final MeasurementStoreQuery query,
          final Schema table, final long startTime,
          final long endTime, final int aggTimeMillis) throws IOException {
    TimeSeries data = readToTs(query, table, startTime, endTime, aggTimeMillis);
    return createHeatJFreeChart(data, table, aggTimeMillis);
  }

  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE") // false positive.
  public static TimeSeries readToTs(final MeasurementStoreQuery query, final Schema table,
          final long startTime, final long endTime, final int aggTimeMillis) throws IOException {
    TLongList ts = new TLongArrayList();
    List<long[]> values = new ArrayList<>();
    try (AvroCloseableIterable<Observation> data
            = aggTimeMillis <= 0
            ? query.getObservations(table, Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime))
            : query.getAggregatedObservations(table, Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime),
                    aggTimeMillis, TimeUnit.MILLISECONDS)) {
      for (Observation rec : data) {
        ts.add(rec.getRelTimeStamp());
        values.add(Longs.toArray(rec.getData()));
      }
    }
    return new TimeSeries(ts.toArray(), values.toArray(new long[values.size()][]));
  }

  public static String[] getDataColumnNames(final Schema info) {
    List<Schema.Field> fields = info.getFields();
    int size = fields.size();
    String[] result = new String[size - 1];
    for (int i = 1, j = 0; i < size; i++, j++) {
      result[j] = fields.get(i).name();
    }
    return result;
  }

  public static JFreeChart createHeatJFreeChart(final TimeSeries data, final Schema info, final int aggTime) {
    int sampleTime = TimeSeriesRecord.getFrequencyMillis(info);
    Pair<long[], double[][]> mData = fillGaps(data.getTimeStamps(),
            data.getValues(), Math.max(sampleTime, aggTime), info.getFields().size() - 1);
    return org.spf4j.perf.impl.chart.Charts.createHeatJFreeChart(getDataColumnNames(info),
            mData.getSecond(), data.getTimeStamps()[0], sampleTime,
            TimeSeriesRecord.getUnit(info.getField("total").schema()), "Measurements distribution for "
            + info.getName() + ", sampleTime " + sampleTime + "ms, generated by spf4j");
  }

  public static JFreeChart createMinMaxAvgJFreeChart(final TimeSeries data, final Schema info) {
    long[][] vals = data.getValues();
    double[] min = Arrays.getColumnAsDoubles(vals, info.getField("min").pos() - 1);
    double[] max = Arrays.getColumnAsDoubles(vals, info.getField("max").pos() - 1);
    final int totalColumnIndex = info.getField("total").pos() - 1;
    double[] total = Arrays.getColumnAsDoubles(vals, totalColumnIndex);
    double[] count = Arrays.getColumnAsDoubles(vals, info.getField("count").pos() - 1);
    for (int i = 0; i < count.length; i++) {
      if (count[i] == 0) {
        min[i] = 0;
        max[i] = 0;
      }
    }
    int sampleTime = TimeSeriesRecord.getFrequencyMillis(info);
    long[] timestamps = data.getTimeStamps();
    return org.spf4j.perf.impl.chart.Charts.createTimeSeriesJFreeChart("Min,Max,Avg chart for "
            + info.getName() + ", sampleTime " + sampleTime + "ms, generated by spf4j", timestamps,
            new String[]{"min", "max", "avg"},
            TimeSeriesRecord.getUnit(info.getField("total").schema()),
            new double[][]{min, max, Arrays.divide(total, count)});
  }

  public static JFreeChart createMinMaxAvgJFreeChart(final MeasurementStoreQuery query, final Schema table,
          final long startTime, final long endTime, final int aggTimeMillis) throws IOException {
    TimeSeries data = readToTs(query, table, startTime, endTime, aggTimeMillis);
    return createMinMaxAvgJFreeChart(data, table);
  }

  public static JFreeChart createCountJFreeChart(final TimeSeries data, final Schema info) {
    long[][] vals = data.getValues();
    double[] count = Arrays.getColumnAsDoubles(vals, info.getField("count").pos() - 1);
    long[] timestamps = data.getTimeStamps();
    int sampleTime = TimeSeriesRecord.getFrequencyMillis(info);
    return org.spf4j.perf.impl.chart.Charts.createTimeSeriesJFreeChart("count chart for "
            + info.getName() + ", sampleTime " + sampleTime + " ms, generated by spf4j", timestamps,
            new String[]{"count"}, "count", new double[][]{count});
  }

  public static JFreeChart createCountJFreeChart(final MeasurementStoreQuery query,
          final Schema info, final long startTime,
          final long endTime, final int aggTimeMillis) throws IOException {
    TimeSeries data =  readToTs(query, info, startTime, endTime, aggTimeMillis);
    return createCountJFreeChart(data, info);
  }

  public static List<JFreeChart> createJFreeCharts(final TimeSeries data, final Schema info) {
    long[][] vals = data.getValues();
    Map<String, Pair<List<String>, List<double[]>>> measurementsByUom = new HashMap<>();
    int i = 0;
    Iterator<Schema.Field> it = info.getFields().iterator();
    it.next();
    while (it.hasNext()) {
      Schema.Field col = it.next();
      String uom = TimeSeriesRecord.getUnit(col.schema());
      Pair<List<String>, List<double[]>> meas = measurementsByUom.get(uom);
      if (meas == null) {
        meas = Pair.of((List<String>) new ArrayList<String>(), (List<double[]>) new ArrayList<double[]>());
        measurementsByUom.put(uom, meas);
      }
      meas.getFirst().add(col.name());
      meas.getSecond().add(Arrays.getColumnAsDoubles(vals, i));
      i++;
    }
    long[] timestamps = data.getTimeStamps();
    int sampleTime = TimeSeriesRecord.getFrequencyMillis(info);
    List<JFreeChart> result = new ArrayList<>(measurementsByUom.size());
    for (Map.Entry<String, Pair<List<String>, List<double[]>>> entry : measurementsByUom.entrySet()) {
      Pair<List<String>, List<double[]>> p = entry.getValue();
      final List<String> measurementNames = p.getFirst();
      final List<double[]> measurements = p.getSecond();
      result.add(org.spf4j.perf.impl.chart.Charts.createTimeSeriesJFreeChart("chart for "
              + info.getName() + ", sampleTime " + sampleTime
              + " ms, generated by spf4j", timestamps,
              measurementNames.toArray(new String[measurementNames.size()]), entry.getKey(),
              measurements.toArray(new double[measurements.size()][])));
    }
    return result;
  }

  public static List<JFreeChart> createJFreeCharts(final MeasurementStoreQuery query, final Schema td,
          final long startTime, final long endTime, final int aggTimeMillis) throws IOException {
    TimeSeries data = readToTs(query, td, startTime, endTime, aggTimeMillis);
    return createJFreeCharts(data, td);
  }

}
