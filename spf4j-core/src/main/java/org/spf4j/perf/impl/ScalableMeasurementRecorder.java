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

import org.spf4j.base.AbstractRunnable;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.perf.EntityMeasurements;
import org.spf4j.perf.EntityMeasurementsInfo;
import org.spf4j.perf.MeasurementStore;
import org.spf4j.perf.MeasurementProcessor;
import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.io.Csv;
import org.spf4j.jmx.JmxExport;

/**
 *
 * @author zoly
 */
@ThreadSafe
// a recorder instance is tipically alive for the entire life of the process
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings("PMB_INSTANCE_BASED_THREAD_LOCAL")
public final class ScalableMeasurementRecorder extends MeasurementAggregator
    implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(ScalableMeasurementRecorder.class);

    private final Map<Thread, MeasurementProcessor> threadLocalRecorders;
    private final ThreadLocal<MeasurementProcessor> threadLocalRecorder;
    private final ScheduledFuture<?> samplingFuture;
    private final MeasurementProcessor processorTemplate;
    private final AbstractRunnable persister;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("PMB_INSTANCE_BASED_THREAD_LOCAL")
    ScalableMeasurementRecorder(final MeasurementProcessor processor, final int sampleTimeMillis,
            final MeasurementStore measurementStore) {
        threadLocalRecorders = new HashMap<>();
        processorTemplate = processor;
        threadLocalRecorder = new ThreadLocal<MeasurementProcessor>() {

            @Override
            protected MeasurementProcessor initialValue() {
                MeasurementProcessor result = (MeasurementProcessor) processor.createClone();
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
            throw new RuntimeException(ex);
        }
        persister = new AbstractRunnable(true) {
            private volatile long lastRun = 0;

            @Override
            public void doRun() throws IOException {
                long currentTime = System.currentTimeMillis();
                if (currentTime > lastRun) {
                    lastRun = currentTime;
                    final long[] measurements = ScalableMeasurementRecorder.this.getMeasurementsAndReset();
                    if (measurements != null) {
                        measurementStore.saveMeasurements(tableId, currentTime, measurements);
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
    public void record(final long measurement) {
        threadLocalRecorder.get().record(measurement);
    }

    @Override
    public long [] getMeasurements() {
        EntityMeasurements result  = null;
        synchronized (threadLocalRecorders) {
            for (Map.Entry<Thread, MeasurementProcessor> entry : threadLocalRecorders.entrySet()) {
                EntityMeasurements measurements = entry.getValue().createClone();
                if (result == null) {
                    result = measurements;
                } else {
                    result = result.aggregate(measurements);
                }
            }
        }
        return (result == null) ? null : result.getMeasurements();
    }

    @JmxExport(description = "measurements as csv")
    public String getMeasurementsAsString() {
        StringWriter sw = new StringWriter(128);
        EntityMeasurementsInfo info = getInfo();
        try {
            Csv.writeCsvRow(sw, (Object[]) info.getMeasurementNames());
            Csv.writeCsvRow(sw, (Object[]) info.getMeasurementUnits());
            Csv.writeCsvRow(sw, getMeasurements());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return sw.toString();
    }

    @JmxExport
    public void clear() {
        getMeasurementsAndReset();
    }


    @Override
    public EntityMeasurements aggregate(final EntityMeasurements mSource) {
        throw new UnsupportedOperationException("Aggregating Scalable Recorders not supported");
    }



    @Override
    public EntityMeasurements createClone() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void close() {
        samplingFuture.cancel(false);
        persister.run();
    }

    @Override
    public String toString() {
        return "ScalableMeasurementRecorder{" + "threadLocalRecorders=" + threadLocalRecorders
                + ", processorTemplate=" + processorTemplate + '}';
    }

    @Override
    public EntityMeasurements createLike(final Object entity) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public EntityMeasurementsInfo getInfo() {
        return processorTemplate.getInfo();
    }

    @Override
    public EntityMeasurements reset() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long[] getMeasurementsAndReset() {
        EntityMeasurements result  = null;
        synchronized (threadLocalRecorders) {
            Iterator<Map.Entry<Thread, MeasurementProcessor>> iterator = threadLocalRecorders.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Thread, MeasurementProcessor> entry = iterator.next();
                Thread t = entry.getKey();
                if (!t.isAlive()) {
                    iterator.remove();
                }
                EntityMeasurements measurements = entry.getValue().reset();
                if (result == null) {
                    result = measurements;
                } else {
                    if (measurements != null) {
                        result = result.aggregate(measurements);
                    }
                }
            }
        }
        return (result == null) ? null : result.getMeasurements();
    }

}
