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
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.spf4j.base.Arrays;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.Registry;
import org.spf4j.perf.MeasurementsInfo;
import org.spf4j.perf.MeasurementStore;
import org.spf4j.perf.MultiMeasurementRecorder;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("VO_VOLATILE_REFERENCE_TO_ARRAY")
public final class DirectStoreMultiAccumulator implements MultiMeasurementRecorder, Closeable {

    private final MeasurementsInfo info;
    private final MeasurementStore measurementStore;
    private final long tableId;

    private volatile long[] lastRecorded;



    public DirectStoreMultiAccumulator(final MeasurementsInfo info, final MeasurementStore measurementStore) {
        try {
            this.info = info;
            this.measurementStore = measurementStore;
            this.tableId = measurementStore.alocateMeasurements(info, 0);
            this.lastRecorded = Arrays.EMPTY_LONG_ARRAY;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }



    @Override
    public void record(final long... measurement) {
        recordAt(System.currentTimeMillis(), measurement);
    }

    @Override
    @SuppressFBWarnings({"EI_EXPOSE_REP2", "EXS_EXCEPTION_SOFTENING_NO_CHECKED" })
    public void recordAt(final long timestampMillis, final long... measurement) {
        try {
            measurementStore.saveMeasurements(tableId, timestampMillis, measurement);
        } catch (IOException ex) {
           throw new UncheckedIOException(ex);
        } finally {
            lastRecorded = measurement;
        }
    }

    public void registerJmx() {
        Registry.export("org.spf4j.perf.recorders", info.getMeasuredEntity().toString(), this);
    }

    @JmxExport
    public long[] getLastRecorded() {
        return lastRecorded.clone();
    }

    @JmxExport
    public String getInfo() {
        return info.toString();
    }

    @Override
    public void close() {
        Registry.unregister("org.spf4j.perf.recorders", info.getMeasuredEntity().toString());
    }

    @Override
    public String toString() {
        return "DirectStoreMultiAccumulator{" + "info=" + info + ", measurementStore=" + measurementStore
                + ", tableId=" + tableId + ", lastRecorded=" + java.util.Arrays.toString(lastRecorded) + '}';
    }



}
