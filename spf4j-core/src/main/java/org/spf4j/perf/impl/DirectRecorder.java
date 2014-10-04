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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import org.spf4j.perf.EntityMeasurementsInfo;
import org.spf4j.perf.MeasurementStore;
import org.spf4j.perf.MeasurementRecorder;

/**
 *
 * @author zoly
 */
public final class DirectRecorder implements MeasurementRecorder {
    
    private final EntityMeasurementsInfo info;
    private static final String[] MEASUREMENTS = {"value"};
    private final MeasurementStore measurementStore;
    private final int sampleTimeMillis;

    
    public DirectRecorder(final Object measuredEntity, final String description, final String unitOfMeasurement,
            final int sampleTimeMillis, final MeasurementStore measurementStore) {
        this.info = new EntityMeasurementsInfoImpl(measuredEntity, description,
                MEASUREMENTS, new String[]{unitOfMeasurement});
        this.measurementStore = measurementStore;
        this.sampleTimeMillis = sampleTimeMillis;
    }
    
    public String getUnitOfMeasurement() {
        return info.getMeasurementUnit(0);
    }
    

    @Override
    public void record(final long measurement) {
        record(measurement, System.currentTimeMillis());
    }

    @Override
    @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
    public void record(final long measurement, final long timestampMillis) {
        try {
            measurementStore.saveMeasurements(info, timestampMillis, sampleTimeMillis, measurement);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
}
