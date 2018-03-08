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
public final class CountingAccumulator
        extends AbstractMeasurementAccumulator {

    private static final String[] MEASUREMENTS = {"count", "total"};

    private long counter;
    private long total;
    private final MeasurementsInfo info;

    private CountingAccumulator(final Object measuredEntity, final String description,
            final String unitOfMeasurement, final long counter, final long total) {
        this.info = new MeasurementsInfoImpl(measuredEntity, description,
                MEASUREMENTS, new String[]{"count", unitOfMeasurement});
        this.counter = counter;
        this.total = total;
    }

    public CountingAccumulator(final Object measuredEntity, final String description, final String unitOfMeasurement) {
        this(measuredEntity, description, unitOfMeasurement, 0, 0);
    }

    public String getUnitOfMeasurement() {
        return info.getMeasurementUnit(1);
    }

    @Override
    public synchronized void record(final long measurement) {
        total += measurement;
        counter++;
    }

    @Override
    @SuppressFBWarnings("PZLA_PREFER_ZERO_LENGTH_ARRAYS")
    @Nullable
    public synchronized long[] get() {
        if (counter == 0) {
            return null;
        } else {
            return new long[]{counter, total};
        }
    }

    @SuppressFBWarnings({"CLI_CONSTANT_LIST_INDEX", "NOS_NON_OWNED_SYNCHRONIZATION" })
    @Override
    public MeasurementAccumulator aggregate(final MeasurementAccumulator mSource) {
        if (mSource instanceof CountingAccumulator) {
            CountingAccumulator other = (CountingAccumulator) mSource;
            long[] measurements = other.get();
            if (measurements != null) {
              synchronized (this) {
                  return new CountingAccumulator(this.info.getMeasuredEntity(), this.info.getDescription(),
                          getUnitOfMeasurement(), counter + measurements[0], total + measurements[1]);
              }
            } else {
              return this.createClone();
            }
        } else {
            throw new IllegalArgumentException("Cannot aggregate " + this + " with " + mSource);
        }
    }

    @Override
    public synchronized CountingAccumulator createClone() {
        return new CountingAccumulator(this.info.getMeasuredEntity(),
                this.info.getDescription(), getUnitOfMeasurement(), counter, total);
    }

    @Override
    public CountingAccumulator createLike(final Object entity) {
        return new CountingAccumulator(entity, this.info.getDescription(), getUnitOfMeasurement());
    }

    @Override
    public MeasurementsInfo getInfo() {
        return info;
    }

    @Override
    @Nullable
    public synchronized MeasurementAccumulator reset() {
        if (counter == 0) {
            return null;
        } else {
            MeasurementAccumulator result = this.createClone();
            counter = 0;
            total = 0;
            return result;
        }
    }

    @Override
    @SuppressFBWarnings("PZLA_PREFER_ZERO_LENGTH_ARRAYS")
    @Nullable
    public long[] getThenReset() {
        final MeasurementAccumulator vals = reset();
        if (vals == null) {
            return null;
        } else {
            return vals.get();
        }
    }

    @Override
    public String toString() {
        return "CountingAccumulator{" + "counter=" + counter + ", total=" + total + ", info=" + info + '}';
    }

}
