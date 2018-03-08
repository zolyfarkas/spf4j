/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
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
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.perf.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.annotation.Nullable;
import org.spf4j.perf.MeasurementAccumulator;
import org.spf4j.perf.MeasurementsInfo;

/**
 *
 * @author zoly
 */
public final class MinMaxAvgAccumulator
    extends AbstractMeasurementAccumulator {

    private static final String[] MEASUREMENTS = {"count", "total", "min", "max"};

    private long counter;
    private long total;
    private long min;
    private long max;
    private final MeasurementsInfo info;

    private MinMaxAvgAccumulator(final Object measuredEntity, final String description, final String unitOfMeasurement,
            final long counter, final long total, final long min, final long max) {
        this.info = new MeasurementsInfoImpl(measuredEntity, description,
                MEASUREMENTS, new String[] {"count", unitOfMeasurement, unitOfMeasurement, unitOfMeasurement});
        this.counter = counter;
        this.total = total;
        this.min = min;
        this.max = max;
    }

    public MinMaxAvgAccumulator(final Object measuredEntity, final String description, final String unitOfMeasurement) {
        this(measuredEntity, description, unitOfMeasurement, 0, 0, Long.MAX_VALUE, Long.MIN_VALUE);
    }


    public String getUnitOfMeasurement() {
        return info.getMeasurementUnit(1);
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
    @SuppressFBWarnings("PZLA_PREFER_ZERO_LENGTH_ARRAYS")
    @Nullable
    public synchronized long[] get() {
        if (counter == 0) {
            return null;
        } else {
            return new long[] {counter, total, min, max};
        }
    }

    @SuppressFBWarnings({"CLI_CONSTANT_LIST_INDEX", "NOS_NON_OWNED_SYNCHRONIZATION" })
    @Override
    public MeasurementAccumulator aggregate(final MeasurementAccumulator mSource) {
        if (mSource instanceof MinMaxAvgAccumulator) {
            MinMaxAvgAccumulator other = (MinMaxAvgAccumulator) mSource;
            long[] measurements = other.get();
            if (measurements != null) {
                synchronized (this) {
                return new MinMaxAvgAccumulator(this.info.getMeasuredEntity(), this.info.getDescription(),
                    getUnitOfMeasurement(),
                    counter + measurements[0], total + measurements[1],
                    Math.min(min, measurements[2]), Math.max(max, measurements[3]));
                }
            } else {
                return this.createClone();
            }
        } else {
           throw new IllegalArgumentException("Cannot aggregate " + this + " with " + mSource);
        }
    }

    @Override
    public synchronized MinMaxAvgAccumulator createClone() {
        return new MinMaxAvgAccumulator(this.info.getMeasuredEntity(),
                this.info.getDescription(), getUnitOfMeasurement(), counter, total, min, max);
    }

    @Override
    public MeasurementAccumulator createLike(final Object entity) {
        return new MinMaxAvgAccumulator(entity, this.info.getDescription(), getUnitOfMeasurement());
    }

    @Override
    public MeasurementsInfo getInfo() {
        return info;
    }

    @Override
    @Nullable
    public synchronized MinMaxAvgAccumulator reset() {
        if (counter == 0) {
            return null;
        } else {
            MinMaxAvgAccumulator result = createClone();
            counter = 0;
            total = 0;
            min = Long.MAX_VALUE;
            max = Long.MIN_VALUE;
            return result;
        }
    }

    @Override
    @SuppressFBWarnings("PZLA_PREFER_ZERO_LENGTH_ARRAYS")
    @Nullable
    public long[] getThenReset() {
        final MinMaxAvgAccumulator vals = reset();
        if (vals == null) {
            return null;
        } else {
            return vals.get();
        }
    }

    @Override
    public String toString() {
        return "MinMaxAvgAccumulator{" + "counter=" + counter + ", total=" + total + ", min=" + min
                + ", max=" + max + ", info=" + info + '}';
    }

}
