/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.perf.impl;

import com.zoltran.perf.EntityMeasurements;
import com.zoltran.perf.EntityMeasurementsInfo;
import com.zoltran.perf.MeasurementProcessor;

/**
 *
 * @author zoly
 */
public class CountingRecorder 
    implements MeasurementProcessor {

    private long counter;
    private long total;
    private final EntityMeasurementsInfo info;
    
    private static final String [] measurements ={"count", "total"};

    private CountingRecorder(final Object measuredEntity, final String unitOfMeasurement, long counter, long total) {
        this.info = new EntityMeasurementsInfoImpl(measuredEntity, unitOfMeasurement, measurements);
        this.counter = counter;
        this.total = total;
    }

    public CountingRecorder(final Object measuredEntity, final String unitOfMeasurement) {
        this(measuredEntity, unitOfMeasurement, 0, 0);
    }
    
    
    
    @Override
    public synchronized void record(long measurement) {
        total+=measurement;
        counter++;
    }

    @Override
    public synchronized long[] getMeasurements(boolean reset) {
        long[] result = new long[] {counter, total};
        if (reset) {
            counter = 0;
            total = 0;
        }
        return result;
    }

    @Override
    public synchronized EntityMeasurements aggregate(EntityMeasurements mSource) {
        CountingRecorder other = (CountingRecorder) mSource;
        long [] measurements = other.getMeasurements(false);
        return new CountingRecorder(this.info.getMeasuredEntity(), this.info.getUnitOfMeasurement(), 
                counter + measurements[0], total + measurements[1]);
    }

    @Override
    public synchronized EntityMeasurements createClone(boolean reset) {
        CountingRecorder result = new CountingRecorder(this.info.getMeasuredEntity(),
                this.info.getUnitOfMeasurement(), counter, total );
        if (reset) {
            counter = 0;
            total = 0;
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
