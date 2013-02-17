/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.perf.impl;

import com.zoltran.perf.EntityMeasurements;
import com.zoltran.perf.EntityMeasurementsInfo;
import com.zoltran.perf.MeasurementProcessor;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author zoly
 */
public class CountingRecorder 
    implements MeasurementProcessor {

    private long counter;
    private final EntityMeasurementsInfo info;
    
    private static final Set<String> measurements;
    static {
        HashSet set = new HashSet();
        set.add("counter");
        measurements = Collections.unmodifiableSet(set);
    }

    private CountingRecorder(final Object measuredEntity, final String unitOfMeasurement, long counter) {
        this(measuredEntity, unitOfMeasurement);
        this.counter = counter;
    }

    public CountingRecorder(final Object measuredEntity, final String unitOfMeasurement) {
        this.counter=0;
        this.info = new EntityMeasurementsInfoImpl(measuredEntity, unitOfMeasurement, measurements);
    }
    
    
    
    @Override
    public synchronized void record(long measurement) {
        counter+=measurement;
    }

    @Override
    public synchronized Map<String, Number> getMeasurements(boolean reset) {
        
        Map<String, Number> result = new HashMap<String, Number>();
        result.put("counter", counter);
        if (reset) {
            counter = 0;
        }
        return result;
    }

    @Override
    public synchronized EntityMeasurements aggregate(EntityMeasurements mSource) {
        CountingRecorder other = (CountingRecorder) mSource;
        return new CountingRecorder(this.info.getMeasuredEntity(), this.info.getUnitOfMeasurement(), counter + other.counter);
    }

    @Override
    public synchronized EntityMeasurements createClone(boolean reset) {
        CountingRecorder result = new CountingRecorder(this.info.getMeasuredEntity(), this.info.getUnitOfMeasurement(), counter );
        if (reset) {
            counter = 0;
        }
        return result;
    }

    @Override
    public EntityMeasurements createLike(Object entity) {
        return new CountingRecorder(entity, this.info.getUnitOfMeasurement());
    }

    @Override
    public EntityMeasurementsInfo getInfo() {
        return info;
    }
}
