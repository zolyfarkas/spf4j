/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.perf.impl;

import com.zoltran.base.AbstractRunnable;
import com.zoltran.base.DefaultScheduler;
import com.zoltran.perf.*;
import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class ScalableMeasurementRecorderSource implements MeasurementRecorderSource, EntityMeasurementsSource, Closeable {

    
    private final Map<Thread, Map<Object, MeasurementProcessor>> measurementProcessorMap;
    
    private final ThreadLocal<Map<Object, MeasurementProcessor>> threadLocalMeasurementProcessorMap;
    

    private final ScheduledFuture<?> samplingFuture;
    private final MeasurementProcessor processorTemplate;
    
    public ScalableMeasurementRecorderSource (final MeasurementProcessor processor , 
            final int sampleTimeMillis, final MeasurementDatabase database) {
        this.processorTemplate = processor;
        measurementProcessorMap = new HashMap<Thread, Map<Object, MeasurementProcessor>>(); 
        threadLocalMeasurementProcessorMap = new ThreadLocal<Map<Object, MeasurementProcessor>>() {

            @Override
            protected Map<Object, MeasurementProcessor> initialValue() {
                Map<Object, MeasurementProcessor> result = new HashMap<Object, MeasurementProcessor>();
                synchronized(measurementProcessorMap) {
                    measurementProcessorMap.put(Thread.currentThread(), result);
                }
                return result;
            }
            
        };
        samplingFuture = DefaultScheduler.scheduleAllignedAtFixedRateMillis(new AbstractRunnable(true) {
            @Override
            public void doRun() throws IOException {
                for (EntityMeasurements m: ScalableMeasurementRecorderSource.this.getEntitiesMeasurements(true).values()) {
                    database.saveMeasurements(m, System.currentTimeMillis(), sampleTimeMillis);
                }
            }
        }, sampleTimeMillis);
    }
    
    public MeasurementRecorder getRecorder(Object forWhat) {        
        Map<Object, MeasurementProcessor> recorders = threadLocalMeasurementProcessorMap.get();
        synchronized(recorders) {
            MeasurementProcessor result = recorders.get(forWhat);
            if (result == null)  {
                result = (MeasurementProcessor) processorTemplate.createLike(forWhat);
                recorders.put(forWhat, result);
            }
            return result; 
        }      
    }

    @Override
    public Map<Object, EntityMeasurements> getEntitiesMeasurements(boolean reset) {
        
        Map<Object, EntityMeasurements> result = new HashMap<Object, EntityMeasurements>();
        
        synchronized(measurementProcessorMap) {        
            for(Map.Entry<Thread, Map<Object, MeasurementProcessor>> entry: measurementProcessorMap.entrySet()) {
                
                Map<Object, MeasurementProcessor> measurements = entry.getValue();
                synchronized(measurements) {
                    for( Map.Entry<Object, MeasurementProcessor> lentry : measurements.entrySet()) {

                        Object what = lentry.getKey();
                        EntityMeasurements existingMeasurement = result.get(what);
                        if (existingMeasurement == null) {                           
                            existingMeasurement = lentry.getValue().createClone(reset);
                        }
                        else {
                            existingMeasurement = existingMeasurement.aggregate(lentry.getValue().createClone(reset));
                        }
                        result.put(what, existingMeasurement);
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

    @Override
    protected void finalize() throws Throwable {
        try{
            super.finalize();
        } finally {
            this.close();
        }
    }
    
    
}
