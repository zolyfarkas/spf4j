package org.spf4j.perf.impl;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.spf4j.base.Pair;
import org.spf4j.concurrent.UnboundedLoadingCache;
import org.spf4j.perf.MeasurementRecorder;
import org.spf4j.perf.MeasurementRecorderSource;
import org.spf4j.perf.MeasurementStore;

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

public final class DirectRecorderSource implements MeasurementRecorderSource {


    private final LoadingCache<Object, MeasurementRecorder> recorders;

    public DirectRecorderSource(final Object forWhat, final String description,
            final String uom, final int sampleTimeMillis, final MeasurementStore store) {
        recorders = new UnboundedLoadingCache(16,
                new CreateDirectRecorder(forWhat, description, uom, sampleTimeMillis, store));
    }

    @Override
    public MeasurementRecorder getRecorder(final Object forWhat) {
        return recorders.getUnchecked(forWhat);
    }

    @Override
    public void close() {
    }

    private static final class CreateDirectRecorder extends CacheLoader<Object, MeasurementRecorder> {

        private final Object forWhat;
        private final String description;
        private final String uom;
        private final int sampleTimeMillis;
        private final MeasurementStore store;

        CreateDirectRecorder(final Object forWhat, final String description,
                final String uom, final int sampleTimeMillis, final MeasurementStore store) {
            this.forWhat = forWhat;
            this.description = description;
            this.uom = uom;
            this.sampleTimeMillis = sampleTimeMillis;
            this.store = store;
        }

        @Override
        public MeasurementRecorder load(final Object key) {
            return new DirectStoreAccumulator(Pair.of(forWhat, key), description,
                    uom, sampleTimeMillis, store);
        }
    }

    @Override
    public String toString() {
        return "DirectRecorderSource{" + "recorders=" + recorders + '}';
    }

}
