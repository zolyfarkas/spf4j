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
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.base.Pair;
import org.spf4j.base.ShutdownHooks;
import org.spf4j.base.ShutdownThread;
import org.spf4j.io.Csv;
import org.spf4j.jmx.GenericExportedValue;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.DynamicMBeanBuilder;
import org.spf4j.jmx.Registry;
import org.spf4j.perf.CloseableMeasurementRecorderSource;
import org.spf4j.perf.JmxSupport;
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
        MeasurementRecorderSource, MeasurementsSource, CloseableMeasurementRecorderSource, JmxSupport {

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
    if (!ShutdownThread.get().queueHook(ShutdownHooks.ShutdownPhase.OBSERVABILITY_SERVICES, runnable)) {
      close();
    }
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
  @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
  public void registerJmx() {
    MeasurementsInfo info = this.processorTemplate.getInfo();
     CompositeType targetType = addNameDescription(info.toCompositeType());
    try {
      String description = info.getDescription();
      if (description.isEmpty()) {
        description = "Dynamic measurements";
      }
      new DynamicMBeanBuilder().withJmxExportObject(this)
              .withAttribute(new GenericExportedValue<>("measurements", description,
                      this::getMeasurements, null, new TabularType(info.getMeasuredEntity().toString(),
                              description, targetType, new String[]{"name"})))
              .register("org.spf4j.perf.recorders", info.getMeasuredEntity().toString());
    } catch (OpenDataException ex) {
      throw new RuntimeException("Cannot create tabular type for " +  targetType, ex);
    }
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

  static CompositeType addNameDescription(final CompositeType initType) {
    Set<String> keys = initType.keySet();
    int colNr = keys.size() + 2;
    String[] names = new String[colNr];
    String[] descrs = new String[colNr];
    OpenType[] types = new OpenType[colNr];
    names[0] = "name";
    names[1] = "description";
    descrs[0] = "metric name";
    descrs[1] = "metric description";
    types[0] = javax.management.openmbean.SimpleType.STRING;
    types[1] = javax.management.openmbean.SimpleType.STRING;
    int i = 2;
    for (String key : keys) {
      names[i] = key;
      descrs[i] = initType.getDescription(key);
      types[i++] = initType.getType(key);
    }
    try {
      return new CompositeType(initType.getTypeName(), initType.getDescription(),
              names, descrs, types);
    } catch (OpenDataException ex) {
      throw new IllegalArgumentException("Invalid type contructed from " + initType, ex);
    }
  }

  static CompositeData addNameDescription(final CompositeType targetType, final CompositeData data,
          final String name, final String description) {
    Set<String> keySet = targetType.keySet();
    int size = keySet.size();
    Map<String, Object> vals = com.google.common.collect.Maps.newLinkedHashMapWithExpectedSize(size);
    vals.put("name", name);
    vals.put("description", description);
    for (String key : data.getCompositeType().keySet()) {
      vals.put(key, data.get(key));
    }
    try {
      return new CompositeDataSupport(targetType, vals);
    } catch (OpenDataException ex) {
      throw new IllegalArgumentException("Invalid open data contructed from " + data, ex);
    }
  }

  public TabularDataSupport getMeasurements() {
    Map<Object, MeasurementAccumulator> entitiesMeasurements = getEntitiesMeasurements();
    MeasurementsInfo info = this.processorTemplate.getInfo();
    CompositeType targetType = addNameDescription(info.toCompositeType());
    TabularDataSupport result;
    try {
      String name = info.getMeasuredEntity().toString();
      String description = info.getDescription();
      if (description.isEmpty()) {
        description = name;
      }
      result = new TabularDataSupport(new TabularType(name, description, targetType, new String[]{"name"}));
    } catch (OpenDataException ex) {
      throw new RuntimeException("Enable to contruct tabular data " + entitiesMeasurements, ex);
    }
    for (Map.Entry<Object, MeasurementAccumulator> entry : entitiesMeasurements.entrySet()) {
      MeasurementAccumulator acc = entry.getValue();
      MeasurementsInfo eInfo = acc.getInfo();
      String cattrName = eInfo.getMeasuredEntity().toString();
      String cattrDesc = eInfo.getDescription();
      if (cattrDesc.isEmpty()) {
        cattrDesc = cattrName;
      }
      result.put(addNameDescription(targetType, acc.getCompositeData(), cattrName, cattrDesc));
    }
    return result;
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
        Logger.getLogger(ScalableMeasurementRecorderSource.class.getName())
                .log(Level.WARNING,
                        "Last measurement recording for {0} was at {1} current run is {2}, something is wrong",
                        new Object[] {processor.getInfo(), lastRun, currentTime});
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
