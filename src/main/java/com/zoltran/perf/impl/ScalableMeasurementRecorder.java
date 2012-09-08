/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.perf.impl;

import com.zoltran.base.AbstractRunnable;
import com.zoltran.base.DefaultScheduler;
import com.zoltran.perf.EntityMeasurements;
import com.zoltran.perf.EntityMeasurementsInfo;
import com.zoltran.perf.MeasurementDatabase;
import com.zoltran.perf.MeasurementProcessor;
import com.zoltran.perf.MeasurementRecorder;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.concurrent.ThreadSafe;

/**
 *
 * @author zoly
 */
@ThreadSafe
public class ScalableMeasurementRecorder implements MeasurementRecorder, EntityMeasurements, Closeable {

    private final Map<Thread, MeasurementProcessor> threadLocalRecorders;
    private final ThreadLocal<MeasurementProcessor> threadLocalRecorder;
    private final ScheduledFuture<?> samplingFuture;
    private final MeasurementProcessor processorTemplate;

    public ScalableMeasurementRecorder(final MeasurementProcessor processor, final int sampleTimeMillis, final MeasurementDatabase database) {
        threadLocalRecorders = new HashMap<Thread, MeasurementProcessor>();
        processorTemplate = processor;
        threadLocalRecorder = new ThreadLocal<MeasurementProcessor>() {

            @Override
            protected MeasurementProcessor initialValue() {
                MeasurementProcessor result = (MeasurementProcessor) processor.createClone(false);
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
        samplingFuture = DefaultScheduler.scheduleAllignedAtFixedRateMillis(new AbstractRunnable(true) {
            @Override
            public void doRun() throws IOException {
                database.saveMeasurements(ScalableMeasurementRecorder.this, System.currentTimeMillis(), sampleTimeMillis);
            }
        }, sampleTimeMillis);

        
    }

    @Override
    public void record(long measurement) {
        threadLocalRecorder.get().record(measurement);
    }

    @Override
    public Map<String, Number> getMeasurements(boolean reset) {
        EntityMeasurements result  = null;
        synchronized (threadLocalRecorders) {
            List<Thread> removeThreads = new ArrayList<Thread>();
            for (Map.Entry<Thread, MeasurementProcessor> entry: threadLocalRecorders.entrySet()) {
                if (!entry.getKey().isAlive() && reset) {
                    removeThreads.add(entry.getKey());
                }
                EntityMeasurements measurements = entry.getValue().createClone(reset);  
                if (result == null) {
                    result = measurements;
                }
                else {
                    result = result.aggregate(measurements);
                }
            }
            for (Thread t : removeThreads) {
                threadLocalRecorders.remove(t);
            }
        }
        return (result == null) ? processorTemplate.getMeasurements(false) : result.getMeasurements(false);
    }

    @Override
    public EntityMeasurements aggregate(EntityMeasurements mSource) {
        throw new UnsupportedOperationException("Aggregating Scalable Recorders not supported");
    }



    @Override
    public EntityMeasurements createClone(boolean reset) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void close() {
        samplingFuture.cancel(false);             
    }

    @Override
    protected void finalize() throws Throwable {
        try{
            super.finalize();
        } finally {
            this.close();
        }
    }

    @Override
    public String toString() {
        return "ScalableMeasurementRecorder{" + "threadLocalRecorders=" + threadLocalRecorders + ", processorTemplate=" + processorTemplate + '}';
    }

    @Override
    public EntityMeasurements createLike(Object entity) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public EntityMeasurementsInfo getInfo() {
        return processorTemplate.getInfo();
    }

    
    
    
}
