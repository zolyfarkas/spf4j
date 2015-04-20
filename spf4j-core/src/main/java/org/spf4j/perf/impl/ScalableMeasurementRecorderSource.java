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
import org.spf4j.perf.EntityMeasurements;
import org.spf4j.perf.EntityMeasurementsInfo;
import org.spf4j.perf.EntityMeasurementsSource;
import org.spf4j.perf.MeasurementStore;
import org.spf4j.perf.MeasurementProcessor;
import org.spf4j.perf.MeasurementRecorder;
import org.spf4j.perf.MeasurementRecorderSource;

@ThreadSafe
// a recorder instance is tipically alive for the entire life of the process
@edu.umd.cs.findbugs.annotations.SuppressWarnings("PMB_INSTANCE_BASED_THREAD_LOCAL")
public final class ScalableMeasurementRecorderSource implements
        MeasurementRecorderSource, EntityMeasurementsSource, Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(ScalableMeasurementRecorderSource.class);


    private final Map<Thread, Map<Object, MeasurementProcessor>> measurementProcessorMap;

    private final ThreadLocal<Map<Object, MeasurementProcessor>> threadLocalMeasurementProcessorMap;


    private final ScheduledFuture<?> samplingFuture;
    private final MeasurementProcessor processorTemplate;

    ScalableMeasurementRecorderSource(final MeasurementProcessor processor,
            final int sampleTimeMillis, final MeasurementStore database) {
        this.processorTemplate = processor;
        measurementProcessorMap = new HashMap<>();
        threadLocalMeasurementProcessorMap = new ThreadLocal<Map<Object, MeasurementProcessor>>() {

            @Override
            protected Map<Object, MeasurementProcessor> initialValue() {
                Map<Object, MeasurementProcessor> result = new HashMap<>();
                synchronized (measurementProcessorMap) {
                    measurementProcessorMap.put(Thread.currentThread(), result);
                }
                return result;
            }

        };
        final AbstractRunnable persister = new AbstractRunnable(true) {
            private volatile long lastRun = 0;

            @Override
            public void doRun() throws IOException {
                long currentTime = System.currentTimeMillis();
                if (currentTime > lastRun) {
                    lastRun = currentTime;
                    for (EntityMeasurements m
                            : ScalableMeasurementRecorderSource.this.getEntitiesMeasurementsAndReset().values()) {
                        final EntityMeasurementsInfo info = m.getInfo();
                        database.alocateMeasurements(info, sampleTimeMillis);
                        database.saveMeasurements(info, currentTime, sampleTimeMillis, m.getMeasurementsAndReset());
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
                persister.doRun();
                close();
            }
        });
    }

    @Override
    public MeasurementRecorder getRecorder(final Object forWhat) {
        Map<Object, MeasurementProcessor> recorders = threadLocalMeasurementProcessorMap.get();
        synchronized (recorders) {
            MeasurementProcessor result = recorders.get(forWhat);
            if (result == null)  {
                result = (MeasurementProcessor) processorTemplate.createLike(
                        Pair.of(processorTemplate.getInfo().getMeasuredEntity(), forWhat));
                recorders.put(forWhat, result);
            }
            return result;
        }
    }

    @Override
    public Map<Object, EntityMeasurements> getEntitiesMeasurements() {
        Map<Object, EntityMeasurements> result = new HashMap<>();

        synchronized (measurementProcessorMap) {
            Iterator<Map.Entry<Thread, Map<Object, MeasurementProcessor>>> iterator =
                    measurementProcessorMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Thread, Map<Object, MeasurementProcessor>> entry = iterator.next();
                Map<Object, MeasurementProcessor> measurements = entry.getValue();
                synchronized (measurements) {
                    for (Map.Entry<Object, MeasurementProcessor> lentry : measurements.entrySet()) {

                        Object what = lentry.getKey();
                        EntityMeasurements existingMeasurement = result.get(what);
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
    public Map<Object, EntityMeasurements> getEntitiesMeasurementsAndReset() {
        Map<Object, EntityMeasurements> result = new HashMap<>();

        synchronized (measurementProcessorMap) {
            Iterator<Map.Entry<Thread, Map<Object, MeasurementProcessor>>> iterator =
                    measurementProcessorMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Thread, Map<Object, MeasurementProcessor>> entry = iterator.next();
                Thread thread = entry.getKey();
                if (!thread.isAlive()) {
                    iterator.remove();
                }
                Map<Object, MeasurementProcessor> measurements = entry.getValue();
                synchronized (measurements) {
                    Iterator<Map.Entry<Object, MeasurementProcessor>> iterator1 = measurements.entrySet().iterator();
                    while (iterator1.hasNext()) {
                        Map.Entry<Object, MeasurementProcessor> lentry = iterator1.next();
                        Object what = lentry.getKey();
                        EntityMeasurements existingMeasurement = result.get(what);
                        if (existingMeasurement == null) {
                            existingMeasurement = lentry.getValue().reset();
                            if (existingMeasurement == null) {
                                iterator1.remove();
                            } else {
                                result.put(what, existingMeasurement);
                            }
                        } else {
                            final EntityMeasurements vals = lentry.getValue().reset();
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



    @Override
    public void close() {
        samplingFuture.cancel(false);
    }

    @JmxExport
    public String getMeasurementsAsString() {
        StringWriter sw = new StringWriter(128);
        Map<Object, EntityMeasurements> entitiesMeasurements = getEntitiesMeasurements();
        EntityMeasurementsInfo info = this.processorTemplate.getInfo();
        try {
            Csv.writeCsvRow2(sw, "Measured", (Object[]) info.getMeasurementNames());
            Csv.writeCsvRow2(sw, "string", (Object[]) info.getMeasurementUnits());
            for (Map.Entry<Object, EntityMeasurements> entry : entitiesMeasurements.entrySet()) {
                Csv.writeCsvElement(entry.getKey().toString(), sw);
                sw.write(',');
                final long[] measurements = entry.getValue().getMeasurements();
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
