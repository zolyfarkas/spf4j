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
package org.spf4j.perf.impl.ms;

import java.io.IOException;
import java.util.Arrays;
import org.spf4j.base.Throwables;
import org.spf4j.perf.EntityMeasurementsInfo;
import org.spf4j.perf.MeasurementStore;

/**
 *
 * @author zoly
 */
public final class MultiStore implements MeasurementStore {

    private final MeasurementStore [] stores;
    
    public MultiStore(final MeasurementStore ... stores) {
        if (stores.length <= 1) {
            throw new IllegalArgumentException("You need to supply more than 1 store, not " + Arrays.toString(stores));
        }
        this.stores = stores;
    }
    
    
    @Override
    public void alocateMeasurements(final EntityMeasurementsInfo measurement,
            final int sampleTimeMillis) throws IOException {
        IOException ex = null;
        for (MeasurementStore store : stores) {
            try {
                store.alocateMeasurements(measurement, sampleTimeMillis);
            } catch (IOException e) {
                if (ex == null) {
                    ex = e;
                } else {
                    ex = Throwables.suppress(e, ex);
                }
            }
        }
        if (ex != null) {
            throw ex;
        }
    }

    @Override
    public void saveMeasurements(final EntityMeasurementsInfo measurementInfo,
            final long timeStampMillis, final int sampleTimeMillis, final long... measurements) throws IOException {
                IOException ex = null;
        for (MeasurementStore store : stores) {
            try {
                store.saveMeasurements(measurementInfo, timeStampMillis, sampleTimeMillis, measurements);
            } catch (IOException e) {
                if (ex == null) {
                    ex = e;
                } else {
                    ex = Throwables.suppress(e, ex);
                }
            }
        }
        if (ex != null) {
            throw ex;
        }
    }

    @Override
    public void flush() throws IOException {
                IOException ex = null;
        for (MeasurementStore store : stores) {
            try {
                store.flush();
            } catch (IOException e) {
                if (ex == null) {
                    ex = e;
                } else {
                    ex = Throwables.suppress(e, ex);
                }
            }
        }
        if (ex != null) {
            throw ex;
        }
    }

    @Override
    public void close() throws IOException {
                IOException ex = null;
        for (MeasurementStore store : stores) {
            try {
                store.close();
            } catch (IOException e) {
                if (ex == null) {
                    ex = e;
                } else {
                    ex = Throwables.suppress(e, ex);
                }
            }
        }
        if (ex != null) {
            throw ex;
        }
    }
    
}
