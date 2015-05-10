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
package org.spf4j.perf;

import java.io.Closeable;
import java.io.IOException;

/**
 *
 * @author zoly
 */
public interface MeasurementStore extends Closeable {

    /**
     * Make any allocations necessary for the following measurements.
     * @param measurement
     * @param sampleTimeMillis
     * @throws IOException
     */
    long alocateMeasurements(MeasurementsInfo measurement, int sampleTimeMillis)
            throws IOException;

    /**
     * Save measurements.
     * @param measurementInfo
     * @param timeStampMillis
     * @param sampleTimeMillis
     * @param measurements
     * @throws IOException
     */
    void saveMeasurements(long tableId, long timeStampMillis, long ... measurements)
            throws IOException;

    /**
     * flush all data that might be buffered by this store.
     * @throws IOException
     */
    void flush() throws IOException;

}
