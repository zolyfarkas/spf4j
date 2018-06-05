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
import org.spf4j.base.AbstractRunnable;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.perf.MeasurementAccumulator;
import org.spf4j.perf.MeasurementsInfo;
import org.spf4j.perf.MeasurementStore;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.io.Csv;
import org.spf4j.jmx.GenericExportedValue;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.DynamicMBeanBuilder;
import org.spf4j.jmx.Registry;
import org.spf4j.perf.CloseableMeasurementRecorder;

/**
 *
 * @author zoly
 */
@ThreadSafe
// a recorder instance is tipically alive for the entire life of the process
@SuppressFBWarnings("PMB_INSTANCE_BASED_THREAD_LOCAL")
public final class ScalableMeasurementRecorder extends AbstractMeasurementAccumulator
  implements CloseableMeasurementRecorder {

  private static final Logger LOG = LoggerFactory.getLogger(ScalableMeasurementRecorder.class);

  private final Map<Thread, MeasurementAccumulator> threadLocalRecorders;
  private final ThreadLocal<MeasurementAccumulator> threadLocalRecorder;
  private final ScheduledFuture<?> samplingFuture;
  private final MeasurementAccumulator processorTemplate;
  private final Persister persister;
  private final Runnable shutdownHook;

  ScalableMeasurementRecorder(final MeasurementAccumulator processor, final int sampleTimeMillis,
          final MeasurementStore measurementStore, final boolean closeOnShutdown) {
    if (sampleTimeMillis < 1000) {
      throw new IllegalArgumentException("sample time needs to be at least 1000 and not " + sampleTimeMillis);
    }
    threadLocalRecorders = new HashMap<>();
    processorTemplate = processor;
    threadLocalRecorder = new ThreadLocal<MeasurementAccumulator>() {

      @Override
      protected MeasurementAccumulator initialValue() {
        MeasurementAccumulator result = (MeasurementAccumulator) processor.createClone();
        synchronized (threadLocalRecorders) {
          threadLocalRecorders.put(Thread.currentThread(), result);
        }
        return result;
      }
    };
    final long tableId;
    try {
      tableId = measurementStore.alocateMeasurements(processor.getInfo(), sampleTimeMillis);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    persister = new Persister(measurementStore, tableId, processor);
    samplingFuture = DefaultScheduler.scheduleAllignedAtFixedRateMillis(persister, sampleTimeMillis);
    if (closeOnShutdown) {
      shutdownHook = closeOnShutdown();
    } else {
      shutdownHook = null;
    }
  }

  public Runnable closeOnShutdown() {
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
  public void record(final long measurement) {
    threadLocalRecorder.get().record(measurement);
  }

  @Override
  public long[] get() {
    MeasurementAccumulator result = null;
    synchronized (threadLocalRecorders) {
      for (Map.Entry<Thread, MeasurementAccumulator> entry : threadLocalRecorders.entrySet()) {
        MeasurementAccumulator measurements = entry.getValue().createClone();
        if (result == null) {
          result = measurements;
        } else {
          result = result.aggregate(measurements);
        }
      }
    }
    return (result == null) ? null : result.get();
  }

  @JmxExport(description = "measurements as csv")
  public String getMeasurementsAsString() {
    StringWriter sw = new StringWriter(128);
    MeasurementsInfo info = getInfo();
    try {
      Csv.writeCsvRow(sw, (Object[]) info.getMeasurementNames());
      Csv.writeCsvRow(sw, (Object[]) info.getMeasurementUnits());
      final long[] values = get();
      if (values != null) {
        Csv.writeCsvRow(sw, values);
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return sw.toString();
  }

  @JmxExport
  public void clear() {
    getThenReset();
  }

  @Override
  public MeasurementAccumulator aggregate(final MeasurementAccumulator mSource) {
    throw new UnsupportedOperationException();
  }

  @Override
  public MeasurementAccumulator createClone() {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("unchecked")
  public void registerJmx() {
    MeasurementsInfo info = processorTemplate.getInfo();
    new DynamicMBeanBuilder().withJmxExportObject(this)
            .withAttribute(new GenericExportedValue<>("measurements", info.getDescription(),
                    this::getCompositeData, null, getInfo().toCompositeType()))
            .register("org.spf4j.perf.recorders", info.getMeasuredEntity().toString());
  }

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
                processorTemplate.getInfo().getMeasuredEntity().toString());
      }
    }
  }

  @Override
  public String toString() {
    return "ScalableMeasurementRecorder{" + "threadLocalRecorders=" + threadLocalRecorders
            + ", processorTemplate=" + processorTemplate + '}';
  }

  @Override
  public MeasurementAccumulator createLike(final Object entity) {
    throw new UnsupportedOperationException();
  }

  @Override
  public MeasurementsInfo getInfo() {
    return processorTemplate.getInfo();
  }

  @Override
  public MeasurementAccumulator reset() {
    throw new UnsupportedOperationException();
  }

  @Override
  public long[] getThenReset() {
    MeasurementAccumulator result = null;
    synchronized (threadLocalRecorders) {
      Iterator<Map.Entry<Thread, MeasurementAccumulator>> iterator = threadLocalRecorders.entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<Thread, MeasurementAccumulator> entry = iterator.next();
        Thread t = entry.getKey();
        if (!t.isAlive()) {
          iterator.remove();
        }
        MeasurementAccumulator measurements = entry.getValue().reset();
        if (result == null) {
          result = measurements;
        } else {
          if (measurements != null) {
            result = result.aggregate(measurements);
          }
        }
      }
    }
    return (result == null) ? null : result.get();
  }

  private class Persister extends AbstractRunnable {

    private final MeasurementStore measurementStore;
    private final long tableId;
    private final MeasurementAccumulator processor;
    private volatile long lastRun = 0;

    Persister(final MeasurementStore measurementStore,
            final long tableId, final MeasurementAccumulator processor) {
      super(true);
      this.measurementStore = measurementStore;
      this.tableId = tableId;
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
        final long[] measurements = ScalableMeasurementRecorder.this.getThenReset();
        if (measurements != null) {
          measurementStore.saveMeasurements(tableId, currentTime, measurements);
        }
      } else if (warn) {
        LOG.warn("Last measurement recording for {} was at {} current run is {}, something is wrong",
                processor.getInfo(), lastRun, currentTime);
      }
    }
  }

}
