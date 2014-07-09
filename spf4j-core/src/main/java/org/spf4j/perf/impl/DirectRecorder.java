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
    private final MeasurementStore database;
    private final int sampleTimeMillis;

    
    public DirectRecorder(final Object measuredEntity, final String unitOfMeasurement,
            final int sampleTimeMillis, final MeasurementStore database) {
        this.info = new EntityMeasurementsInfoImpl(measuredEntity, unitOfMeasurement,
                MEASUREMENTS, new String[]{unitOfMeasurement});
        this.database = database;
        this.sampleTimeMillis = sampleTimeMillis;
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
    public void record(final long measurement) {
        try {
            database.saveMeasurements(info, new long [] {measurement}, System.currentTimeMillis(), sampleTimeMillis);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
}
