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
import org.spf4j.perf.MeasurementRecorder;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zoly
 */
@ThreadSafe
// a recorder instance is tipically alive for the entire life of the process
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings("PMB_INSTANCE_BASED_THREAD_LOCAL")
public final class ScalableMeasurementRecorder implements MeasurementRecorder, EntityMeasurements, Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(ScalableMeasurementRecorder.class);
    
    private final Map<Thread, MeasurementProcessor> threadLocalRecorders;
    private final ThreadLocal<MeasurementProcessor> threadLocalRecorder;
    private final ScheduledFuture<?> samplingFuture;
    private final MeasurementProcessor processorTemplate;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("PMB_INSTANCE_BASED_THREAD_LOCAL")
    ScalableMeasurementRecorder(final MeasurementProcessor processor, final int sampleTimeMillis,
            final MeasurementStore database) {
        threadLocalRecorders = new HashMap<Thread, MeasurementProcessor>();
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
        try {
            database.alocateMeasurements(processor.getInfo(), sampleTimeMillis);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        final AbstractRunnable persister = new AbstractRunnable(true) {
            private volatile long lastRun = 0;

            @Override
            public void doRun() throws IOException {
                long currentTime = System.currentTimeMillis();
                if (currentTime > lastRun) {
                    lastRun = currentTime;
                    database.saveMeasurements(ScalableMeasurementRecorder.this.getInfo(),
                            ScalableMeasurementRecorder.this.getMeasurementsAndReset(),
                            currentTime, sampleTimeMillis);
                } else {
                    LOG.warn("Last measurement recording was at {} current run is {}, something is wrong",
                            lastRun, currentTime);
                }
            }
        };
        samplingFuture = DefaultScheduler.scheduleAllignedAtFixedRateMillis(persister, sampleTimeMillis);
        org.spf4j.base.Runtime.addHookAtBeginning(persister);
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
        return (result == null) ? processorTemplate.getMeasurements() : result.getMeasurements();
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
    }

    @Override
    protected void finalize() throws Throwable  {
        try {
            super.finalize();
        } finally {
            this.close();
        }
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
            List<Thread> removeThreads = new ArrayList<Thread>();
            for (Map.Entry<Thread, MeasurementProcessor> entry : threadLocalRecorders.entrySet()) {
                Thread t = entry.getKey();
                if (!t.isAlive()) {
                    removeThreads.add(t);
                }
                EntityMeasurements measurements = entry.getValue().reset();
                if (result == null) {
                    result = measurements;
                } else {
                    result = result.aggregate(measurements);
                }
            }
            for (Thread t : removeThreads) {
                threadLocalRecorders.remove(t);
            }
        }
        return (result == null) ? processorTemplate.getMeasurements() : result.getMeasurements();
    }
    
}
