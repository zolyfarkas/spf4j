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
package org.spf4j.perf.impl;

import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.base.Pair;
import org.spf4j.io.Csv;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.Registry;
import org.spf4j.perf.MeasurementAccumulator;
import org.spf4j.perf.MeasurementsInfo;
import org.spf4j.perf.MeasurementsSource;
import org.spf4j.perf.MeasurementStore;
import org.spf4j.perf.MeasurementRecorder;
import org.spf4j.perf.MeasurementRecorderSource;

@ThreadSafe
// a recorder instance is tipically alive for the entire life of the process
@edu.umd.cs.findbugs.annotations.SuppressWarnings("PMB_INSTANCE_BASED_THREAD_LOCAL")
public final class ScalableMeasurementRecorderSource implements
        MeasurementRecorderSource, MeasurementsSource, Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(ScalableMeasurementRecorderSource.class);


    private final Map<Thread, Map<Object, MeasurementAccumulator>> measurementProcessorMap;

    private final ThreadLocal<Map<Object, MeasurementAccumulator>> threadLocalMeasurementProcessorMap;


    private final ScheduledFuture<?> samplingFuture;
    private final MeasurementAccumulator processorTemplate;

    private final TObjectLongMap<MeasurementsInfo> tableIds;

    private final AbstractRunnable persister;

    ScalableMeasurementRecorderSource(final MeasurementAccumulator processor,
            final int sampleTimeMillis, final MeasurementStore database) {
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
        persister = new AbstractRunnable(true) {
            private volatile long lastRun = 0;

            @Override
            public void doRun() throws IOException {
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
                        database.saveMeasurements(tableId, currentTime, m.getThenReset());
                    }
                } else {
                    LOG.warn("Last measurement recording was at {} current run is {}, something is wrong",
                            lastRun, currentTime);
                }
            }
        };
        samplingFuture = DefaultScheduler.scheduleAllignedAtFixedRateMillis(persister, sampleTimeMillis);
        org.spf4j.base.Runtime.addHookAtBeginning(new AbstractRunnable(true) {

            @Override
            public void doRun() throws Exception {
                close();
            }
        });
    }

    @Override
    public MeasurementRecorder getRecorder(final Object forWhat) {
        Map<Object, MeasurementAccumulator> recorders = threadLocalMeasurementProcessorMap.get();
        synchronized (recorders) {
            MeasurementAccumulator result = recorders.get(forWhat);
            if (result == null)  {
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
            Iterator<Map.Entry<Thread, Map<Object, MeasurementAccumulator>>> iterator =
                    measurementProcessorMap.entrySet().iterator();
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
            Iterator<Map.Entry<Thread, Map<Object, MeasurementAccumulator>>> iterator =
                    measurementProcessorMap.entrySet().iterator();
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

    public void registerJmx() {
        Registry.export("org.spf4j.perf.recorders",
                this.processorTemplate.getInfo().getMeasuredEntity().toString(), this);
    }

    @Override
    public void close() {
        samplingFuture.cancel(false);
        persister.run();
        Registry.unregister("org.spf4j.perf.recorders",
                this.processorTemplate.getInfo().getMeasuredEntity().toString());
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
            throw new RuntimeException(ex);
        }
        return sw.toString();
    }

    @JmxExport
    public void clear() {
        getEntitiesMeasurementsAndReset();
    }


}
