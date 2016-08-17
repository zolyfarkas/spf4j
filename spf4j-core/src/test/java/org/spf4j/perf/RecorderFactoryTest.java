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
package org.spf4j.perf;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.spf4j.perf.impl.RecorderFactory;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spf4j.base.Throwables;
import org.spf4j.perf.impl.ms.tsdb.TSDBMeasurementStore;
import org.spf4j.tsdb2.TimeSeries;
import org.spf4j.tsdb2.TSDBQuery;
import org.spf4j.tsdb2.TSDBWriter;
import org.spf4j.tsdb2.avro.TableDef;

/**
 *
 * @author zoly
 */
@SuppressWarnings("SleepWhileInLoop")
@SuppressFBWarnings("MDM_THREAD_YIELD")
public final class RecorderFactoryTest {

  @BeforeClass
  public static void init() {
    Thread.setDefaultUncaughtExceptionHandler((final Thread t, final Throwable e) -> {
      StringBuilder sb = new StringBuilder(128);
      try {
        Throwables.writeTo(e, sb, Throwables.Detail.STANDARD);
      } catch (IOException ex) {
        Assert.fail("Got Exception: " + ex);
      }
      Assert.fail("Got Exception: " + sb.toString());
    });
  }

  /**
   * Test of createScalableQuantizedRecorder method, of class RecorderFactory.
   */
  @Test
  public void testCreateScalableQuantizedRecorder() throws IOException, InterruptedException {
    System.out.println("createScalableQuantizedRecorder");
    String forWhat = "test1";
    String unitOfMeasurement = "ms";
    int sampleTime = 1000;
    int factor = 10;
    int lowerMagnitude = 0;
    int higherMagnitude = 3;
    int quantasPerMagnitude = 10;
    MeasurementRecorder result = RecorderFactory.createScalableQuantizedRecorder(
            forWhat, unitOfMeasurement, sampleTime, factor, lowerMagnitude, higherMagnitude, quantasPerMagnitude);
    for (int i = 0; i < 500; i++) {
      result.record(i);
      Thread.sleep(20);
    }
    result.close();
    assertData(forWhat, 124750);
  }

  /**
   * Test of createScalableQuantizedRecorderSource method, of class RecorderFactory.
   */
  @Test
  public void testCreateScalableQuantizedRecorderSource() throws IOException, InterruptedException {
    System.out.println("createScalableQuantizedRecorderSource");
    Object forWhat = "bla";
    String unitOfMeasurement = "ms";
    int sampleTime = 1000;
    int factor = 10;
    int lowerMagnitude = 0;
    int higherMagnitude = 3;
    int quantasPerMagnitude = 10;
    MeasurementRecorderSource result = RecorderFactory.createScalableQuantizedRecorderSource(
            forWhat, unitOfMeasurement, sampleTime, factor, lowerMagnitude, higherMagnitude, quantasPerMagnitude);
    for (int i = 0; i < 5000; i++) {
      result.getRecorder("X" + i % 2).record(1);
      Thread.sleep(1);
    }
    result.close();
    assertData("bla,X0", 2500);
  }

  @Test
  public void testOutofQuantizedZoneValues() throws IOException, InterruptedException {
    System.out.println("testOutofQuantizedZoneValues");
    String forWhat = "largeVals";
    String unitOfMeasurement = "ms";
    int sampleTime = 1000;
    int factor = 10;
    int lowerMagnitude = 0;
    int higherMagnitude = 3;
    int quantasPerMagnitude = 10;
    MeasurementRecorder result = RecorderFactory.createScalableQuantizedRecorder(
            forWhat, unitOfMeasurement, sampleTime, factor, lowerMagnitude, higherMagnitude, quantasPerMagnitude);
    for (int i = 0; i < 500; i++) {
      result.record(10000);
      Thread.sleep(20);
    }
    ((Closeable) result).close();
    assertData(forWhat, 5000000);

  }

  /**
   * Test of createScalableQuantizedRecorderSource method, of class RecorderFactory.
   */
  @Test
  public void testCreateScalableCountingRecorderSource() throws IOException, InterruptedException {
    System.out.println("createScalableCountingRecorderSource");
    String forWhat = "counters";
    String unitOfMeasurement = "counts";
    int sampleTime = 1000;
    MeasurementRecorderSource result = RecorderFactory.createScalableCountingRecorderSource(
            forWhat, unitOfMeasurement, sampleTime);
    for (int i = 0; i < 5000; i++) {
      result.getRecorder("X" + i % 2).record(1);
      Thread.sleep(1);
    }
    ((Closeable) result).close();
    assertData(forWhat + ",X1", 2500);
  }

  @SuppressFBWarnings("CLI_CONSTANT_LIST_INDEX")
  public static void assertData(final String forWhat, final long expectedValue) throws IOException {
    TSDBWriter dbWriter = ((TSDBMeasurementStore) RecorderFactory.MEASUREMENT_STORE).getDBWriter();
    dbWriter.flush();
    final File file = dbWriter.getFile();
    List<TableDef> tableDefs = TSDBQuery.getTableDef(file, forWhat);
    TableDef tableDef = tableDefs.get(0);
    TimeSeries timeSeries = TSDBQuery.getTimeSeries(file, new long[]{tableDef.id}, 0, Long.MAX_VALUE);
    long sum = 0;
    long[][] values = timeSeries.getValues();
    for (long[] row : values) {
      sum += row[0];
    }
    Assert.assertEquals(expectedValue, sum);
  }

  public static void main(final String[] args) throws IOException, InterruptedException {
    new RecorderFactoryTest().testCreateScalableQuantizedRecorder();
  }

}
