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
public final class CountingRecorder
        implements MeasurementProcessor {

    private long counter;
    private long total;
    private final EntityMeasurementsInfo info;
    private static final String[] MEASUREMENTS = {"count", "total"};

    private CountingRecorder(final Object measuredEntity, final String unitOfMeasurement,
            final long counter, final long total) {
        this.info = new EntityMeasurementsInfoImpl(measuredEntity, unitOfMeasurement,
                MEASUREMENTS, new String[]{"count", unitOfMeasurement});
        this.counter = counter;
        this.total = total;
    }

    public CountingRecorder(final Object measuredEntity, final String unitOfMeasurement) {
        this(measuredEntity, unitOfMeasurement, 0, 0);
    }

    @Override
    public synchronized void record(final long measurement) {
        total += measurement;
        counter++;
    }

    @Override
    public synchronized long[] getMeasurements() {
        return new long[]{counter, total};
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("CLI_CONSTANT_LIST_INDEX")
    @Override
    public EntityMeasurements aggregate(final EntityMeasurements mSource) {
        if (mSource instanceof CountingRecorder) {
            CountingRecorder other = (CountingRecorder) mSource;
            long[] measurements = other.getMeasurements();
            synchronized (this) {
                return new CountingRecorder(this.info.getMeasuredEntity(), this.info.getUnitOfMeasurement(),
                        counter + measurements[0], total + measurements[1]);
            }
        } else {
            throw new IllegalArgumentException("Cannot aggregate " + this + " with " + mSource);
        }
    }

    @Override
    public synchronized CountingRecorder createClone() {
        return new CountingRecorder(this.info.getMeasuredEntity(),
                this.info.getUnitOfMeasurement(), counter, total);
    }

    @Override
    public CountingRecorder createLike(final Object entity) {
        return new CountingRecorder(entity, this.info.getUnitOfMeasurement());
    }

    @Override
    public EntityMeasurementsInfo getInfo() {
        return info;
    }

    @Override
    public synchronized EntityMeasurements reset() {
        EntityMeasurements result = this.createClone();
        counter = 0;
        total = 0;
        return result;
    }

    @Override
    public long[] getMeasurementsAndReset() {
        return reset().getMeasurements();
    }
}
