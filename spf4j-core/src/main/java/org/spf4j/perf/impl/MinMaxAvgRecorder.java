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

import org.spf4j.perf.EntityMeasurements;
import org.spf4j.perf.EntityMeasurementsInfo;
import org.spf4j.perf.MeasurementProcessor;

/**
 *
 * @author zoly
 */
public final class MinMaxAvgRecorder
    implements MeasurementProcessor {

    private long counter;
    private long total;
    private long min;
    private long max;
    private final EntityMeasurementsInfo info;
    
    private static final String [] MEASUREMENTS = {"count", "total", "min", "max"};

    private MinMaxAvgRecorder(final Object measuredEntity, final String unitOfMeasurement,
            final long counter, final long total, final long min, final long max) {
        this.info = new EntityMeasurementsInfoImpl(measuredEntity, unitOfMeasurement,
                MEASUREMENTS, new String [] {"count", unitOfMeasurement, unitOfMeasurement, unitOfMeasurement});
        this.counter = counter;
        this.total = total;
        this.min = min;
        this.max = max;
    }

    public MinMaxAvgRecorder(final Object measuredEntity, final String unitOfMeasurement) {
        this(measuredEntity, unitOfMeasurement, 0, 0, Long.MAX_VALUE, Long.MIN_VALUE);
    }
    
    
    
    @Override
    public synchronized void record(final long measurement) {
        total += measurement;
        counter++;
        if (measurement < min) {
            min = measurement;
        }
        if (measurement > max) {
            max = measurement;
        }
    }

    @Override
    public synchronized long[] getMeasurements() {
        return new long[] {counter, total, min, max};
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("CLI_CONSTANT_LIST_INDEX")
    @Override
    public EntityMeasurements aggregate(final EntityMeasurements mSource) {
        if (mSource instanceof MinMaxAvgRecorder) {
            MinMaxAvgRecorder other = (MinMaxAvgRecorder) mSource;
            long [] measurements = other.getMeasurements();
            synchronized (this) {
                return new MinMaxAvgRecorder(this.info.getMeasuredEntity(), this.info.getUnitOfMeasurement(),
                    counter + measurements[0], total + measurements[1],
                    Math.min(min, measurements[2]), Math.max(max, measurements[3]));
            }
        } else {
           throw new IllegalArgumentException("Cannot aggregate " + this + " with " + mSource);
        }
    }

    @Override
    public synchronized MinMaxAvgRecorder createClone() {
        return new MinMaxAvgRecorder(this.info.getMeasuredEntity(),
                this.info.getUnitOfMeasurement(), counter, total , min, max);
    }

    @Override
    public EntityMeasurements createLike(final Object entity) {
        return new MinMaxAvgRecorder(entity, this.info.getUnitOfMeasurement());
    }

    @Override
    public EntityMeasurementsInfo getInfo() {
        return info;
    }

    @Override
    public synchronized MinMaxAvgRecorder reset() {
        MinMaxAvgRecorder result = createClone();
        counter = 0;
        total = 0;
        min = Long.MAX_VALUE;
        max = Long.MIN_VALUE;
        return result;
    }

    @Override
    public long[] getMeasurementsAndReset() {
        return reset().getMeasurements();
    }
}
