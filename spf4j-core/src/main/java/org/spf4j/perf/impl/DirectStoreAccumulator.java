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
import java.beans.ConstructorProperties;
import java.io.Closeable;
import java.io.IOException;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.Registry;
import org.spf4j.perf.MeasurementsInfo;
import org.spf4j.perf.MeasurementStore;
import org.spf4j.perf.MeasurementRecorder;

/**
 *
 * @author zoly
 */
public final class DirectStoreAccumulator implements MeasurementRecorder, Closeable {

    private final MeasurementsInfo info;
    private static final String[] MEASUREMENTS = {"value"};
    private final MeasurementStore measurementStore;
    private final long tableId;

    private volatile long lastRecordedTS;

    private volatile long lastRecordedValue;

    public DirectStoreAccumulator(final Object measuredEntity, final String description, final String unitOfMeasurement,
            final int sampleTimeMillis, final MeasurementStore measurementStore) {
        this.info = new MeasurementsInfoImpl(measuredEntity, description,
                MEASUREMENTS, new String[]{unitOfMeasurement});
        this.measurementStore = measurementStore;
        try {
            tableId = measurementStore.alocateMeasurements(info, sampleTimeMillis);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @JmxExport
    public String getUnitOfMeasurement() {
        return info.getMeasurementUnit(0);
    }


    @Override
    public void record(final long measurement) {
        recordAt(System.currentTimeMillis(), measurement);
    }

    @Override
    @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
    public synchronized void recordAt(final long timestampMillis, final long measurement) {
        lastRecordedValue = measurement;
        lastRecordedTS = timestampMillis;
        try {
            measurementStore.saveMeasurements(tableId, timestampMillis, measurement);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void registerJmx() {
        Registry.export("org.spf4j.perf.recorders", info.getMeasuredEntity().toString(), this);
    }

    @Override
    public void close() throws IOException {
        Registry.unregister("org.spf4j.perf.recorders", info.getMeasuredEntity().toString());
    }

    public static final class RecordedValue {
        private final long ts;
        private final long value;

        @ConstructorProperties({"ts", "value" })
        public RecordedValue(final long ts, final long value) {
            this.ts = ts;
            this.value = value;
        }

        public long getTs() {
            return ts;
        }

        public long getValue() {
            return value;
        }
    }

    @JmxExport(description = "Last recorded value")
    public RecordedValue getLastRecorded() {
        return new RecordedValue(lastRecordedTS, lastRecordedValue);
    }

    @JmxExport
    @SuppressFBWarnings("SPP_NON_USEFUL_TOSTRING")
    public String getInfo() {
        return info.toString();
    }



}
