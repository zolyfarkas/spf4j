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
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.base.Pair;
import org.spf4j.io.Csv;
import org.spf4j.jmx.GenericExportedValue;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.DynamicMBeanBuilder;
import org.spf4j.jmx.Registry;
import org.spf4j.perf.CloseableMeasurementRecorderSource;
import org.spf4j.perf.MeasurementAccumulator;
import org.spf4j.perf.MeasurementsInfo;
import org.spf4j.perf.MeasurementsSource;
import org.spf4j.perf.MeasurementStore;
import org.spf4j.perf.MeasurementRecorder;
import org.spf4j.perf.MeasurementRecorderSource;

@ThreadSafe
// a recorder instance is tipically alive for the entire life of the process
@SuppressFBWarnings("PMB_INSTANCE_BASED_THREAD_LOCAL")
public final class ScalableMeasurementRecorderSource implements
        MeasurementRecorderSource, MeasurementsSource, CloseableMeasurementRecorderSource {

  private static final Logger LOG = LoggerFactory.getLogger(ScalableMeasurementRecorderSource.class);

  private final Map<Thread, Map<Object, MeasurementAccumulator>> measurementProcessorMap;

  private final ThreadLocal<Map<Object, MeasurementAccumulator>> threadLocalMeasurementProcessorMap;

  private final ScheduledFuture<?> samplingFuture;
  private final MeasurementAccumulator processorTemplate;

  private final TObjectLongMap<MeasurementsInfo> tableIds;

  private final Persister persister;
  private final Runnable shutdownHook;

  ScalableMeasurementRecorderSource(final MeasurementAccumulator processor,
          final int sampleTimeMillis, final MeasurementStore database, final boolean closeOnShutdown) {
    if (sampleTimeMillis < 1000) {
      throw new IllegalArgumentException("sample time needs to be at least 1000 and not " + sampleTimeMillis);
    }
    this.processorTemplate = processor;
    measurementProcessorMap = new HashMap<>();
    threadLocalMeasurementProcessorMap = new ThreadLocal<Map<Object, MeasurementAccumulator>>() {

      @Override
      protected Map<Object, MeasurementAccumulator> initialValue() {
        Map<Object, MeasurementAccumulator> result = new HashMap<>();
        synchronized (measurementProcessorMap) {
          measurementProcessorMap.put(Thread.currentThread(), result);
        }
        return result;
      }

    };
    tableIds = new TObjectLongHashMap<>();
    persister = new Persister(database, sampleTimeMillis, processor);
    samplingFuture = DefaultScheduler.scheduleAllignedAtFixedRateMillis(persister, sampleTimeMillis);
    if (closeOnShutdown) {
      shutdownHook = closeOnShutdown();
    } else {
      shutdownHook = null;
    }
  }

  private Runnable closeOnShutdown() {
    final AbstractRunnable runnable = new AbstractRunnable(true) {

      @Override
      public void doRun() {
        close();
      }
    };
    org.spf4j.base.Runtime.queueHook(0, runnable);
    return runnable;
  }

  @Override
  public MeasurementRecorder getRecorder(final Object forWhat) {
    Map<Object, MeasurementAccumulator> recorders = threadLocalMeasurementProcessorMap.get();
    synchronized (recorders) {
      MeasurementAccumulator result = recorders.get(forWhat);
      if (result == null) {
        result = (MeasurementAccumulator) processorTemplate.createLike(
                Pair.of(processorTemplate.getInfo().getMeasuredEntity(), forWhat));
        recorders.put(forWhat, result);
      }
      return result;
    }
  }

  @Override
  public Map<Object, MeasurementAccumulator> getEntitiesMeasurements() {
    Map<Object, MeasurementAccumulator> result = new HashMap<>();

    synchronized (measurementProcessorMap) {
      Iterator<Map.Entry<Thread, Map<Object, MeasurementAccumulator>>> iterator
              = measurementProcessorMap.entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<Thread, Map<Object, MeasurementAccumulator>> entry = iterator.next();
        Map<Object, MeasurementAccumulator> measurements = entry.getValue();
        synchronized (measurements) {
          for (Map.Entry<Object, MeasurementAccumulator> lentry : measurements.entrySet()) {

            Object what = lentry.getKey();
            MeasurementAccumulator existingMeasurement = result.get(what);
            if (existingMeasurement == null) {
              existingMeasurement = lentry.getValue().createClone();
            } else {
              existingMeasurement = existingMeasurement.aggregate(lentry.getValue().createClone());
            }
            result.put(what, existingMeasurement);
          }
        }
      }
    }
    return result;
  }

  @Override
  @Nonnull
  public Map<Object, MeasurementAccumulator> getEntitiesMeasurementsAndReset() {
    Map<Object, MeasurementAccumulator> result = new HashMap<>();

    synchronized (measurementProcessorMap) {
      Iterator<Map.Entry<Thread, Map<Object, MeasurementAccumulator>>> iterator
              = measurementProcessorMap.entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<Thread, Map<Object, MeasurementAccumulator>> entry = iterator.next();
        Thread thread = entry.getKey();
        if (!thread.isAlive()) {
          iterator.remove();
        }
        Map<Object, MeasurementAccumulator> measurements = entry.getValue();
        synchronized (measurements) {
          Iterator<Map.Entry<Object, MeasurementAccumulator>> iterator1 = measurements.entrySet().iterator();
          while (iterator1.hasNext()) {
            Map.Entry<Object, MeasurementAccumulator> lentry = iterator1.next();
            Object what = lentry.getKey();
            MeasurementAccumulator existingMeasurement = result.get(what);
            if (existingMeasurement == null) {
              existingMeasurement = lentry.getValue().reset();
              if (existingMeasurement == null) {
                iterator1.remove();
              } else {
                result.put(what, existingMeasurement);
              }
            } else {
              final MeasurementAccumulator vals = lentry.getValue().reset();
              if (vals != null) {
                existingMeasurement = existingMeasurement.aggregate(vals);
                result.put(what, existingMeasurement);
              } else {
                iterator1.remove();
              }
            }
          }
        }
      }
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  public void registerJmx() {
    MeasurementsInfo info = this.processorTemplate.getInfo();
    new DynamicMBeanBuilder().withJmxExportObject(this)
            .withAttribute(new GenericExportedValue<>("measurements", info.getDescription(),
                    this::getMeasurements, null, info.toCompositeType()))
            .register("org.spf4j.perf.recorders", info.getMeasuredEntity().toString());
  }

  @Override
  @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
  public void close() {
    synchronized (persister) {
      if (!samplingFuture.isCancelled()) {
        if (shutdownHook != null) {
          org.spf4j.base.Runtime.removeQueuedShutdownHook(shutdownHook);
        }
        samplingFuture.cancel(false);
        try {
          persister.persist(false);
        } catch (IOException ex) {
          throw new UncheckedIOException(ex);
        }
        Registry.unregister("org.spf4j.perf.recorders",
                this.processorTemplate.getInfo().getMeasuredEntity().toString());
      }
    }
  }

  @JmxExport(description = "measurements as csv")
  public String getMeasurementsAsString() {
    StringWriter sw = new StringWriter(128);
    Map<Object, MeasurementAccumulator> entitiesMeasurements = getEntitiesMeasurements();
    MeasurementsInfo info = this.processorTemplate.getInfo();
    try {
      Csv.writeCsvRow2(sw, "Measured", (Object[]) info.getMeasurementNames());
      Csv.writeCsvRow2(sw, "string", (Object[]) info.getMeasurementUnits());
      for (Map.Entry<Object, MeasurementAccumulator> entry : entitiesMeasurements.entrySet()) {
        Csv.writeCsvElement(entry.getKey().toString(), sw);
        sw.write(',');
        final long[] measurements = entry.getValue().get();
        if (measurements != null) {
          Csv.writeCsvRow(sw, measurements);
        }
      }

    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return sw.toString();
  }

  public CompositeDataSupport getMeasurements() {
      Map<Object, MeasurementAccumulator> entitiesMeasurements = getEntitiesMeasurements();
      MeasurementsInfo info = this.processorTemplate.getInfo();
      int nrStuff = entitiesMeasurements.size();
      String[] names = new String[nrStuff];
      String[] descriptions = new String[nrStuff];
      OpenType<?>[] types = new OpenType[nrStuff];
      Object[] values = new Object[nrStuff];
      int i = 0;
      for (Map.Entry<Object, MeasurementAccumulator> entry : entitiesMeasurements.entrySet()) {
        MeasurementAccumulator acc = entry.getValue();
        MeasurementsInfo eInfo = acc.getInfo();
        String cattrName = eInfo.getMeasuredEntity().toString();
        names[i] = cattrName;
        String cattrDesc = eInfo.getDescription();
        if (cattrDesc.isEmpty()) {
          cattrDesc = cattrName;
        }
        descriptions[i] = cattrDesc;
        types[i] = eInfo.toCompositeType();
        values[i] = acc.getCompositeData();
        i++;
      }
     try {
      String name = info.getMeasuredEntity().toString();
      String description = info.getDescription();
      if (description.isEmpty()) {
        description = name;
      }
      CompositeType setType = new CompositeType(name, description, names, descriptions, types);
      return new CompositeDataSupport(setType, names, values);
    } catch (OpenDataException ex) {
      throw new IllegalArgumentException("Not composite data compatible " + this, ex);
    }
  }


  @JmxExport
  public void clear() {
    getEntitiesMeasurementsAndReset();
  }

  private class Persister extends AbstractRunnable {

    private final MeasurementStore database;
    private final int sampleTimeMillis;
    private final MeasurementAccumulator processor;
    private volatile long lastRun = 0;


    Persister(final MeasurementStore database, final int sampleTimeMillis,
            final MeasurementAccumulator processor) {
      super(true);
      this.database = database;
      this.sampleTimeMillis = sampleTimeMillis;
      this.processor = processor;
    }

    @Override
    public void doRun() throws IOException {
      persist(true);
    }

    public void persist(final boolean warn) throws IOException {
      long currentTime = System.currentTimeMillis();
      if (currentTime > lastRun) {
        lastRun = currentTime;
        for (MeasurementAccumulator m
                : ScalableMeasurementRecorderSource.this.getEntitiesMeasurementsAndReset().values()) {
          final MeasurementsInfo info = m.getInfo();
          long tableId;
          synchronized (tableIds) {
            tableId = tableIds.get(info);
            if (tableId == 0) {
              tableId = database.alocateMeasurements(info, sampleTimeMillis);
              tableIds.put(info, tableId);
            }
          }
          final long[] data = m.getThenReset();
          if (data != null) {
            database.saveMeasurements(tableId, currentTime, data);
          }
        }
      } else if (warn) {
        LOG.warn("Last measurement recording for {} was at {} current run is {}, something is wrong",
                processor.getInfo(), lastRun, currentTime);
      }
    }
  }

  @Override
  public String toString() {
    return "ScalableMeasurementRecorderSource{" + "measurementProcessorMap=" + measurementProcessorMap
            + ", threadLocalMeasurementProcessorMap=" + threadLocalMeasurementProcessorMap
            + ", samplingFuture=" + samplingFuture + ", processorTemplate=" + processorTemplate
            + ", tableIds=" + tableIds + ", persister=" + persister + ", shutdownHook=" + shutdownHook + '}';
  }

}
