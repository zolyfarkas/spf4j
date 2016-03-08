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

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.spf4j.base.Throwables;
import org.spf4j.perf.MeasurementsInfo;
import org.spf4j.perf.MeasurementStore;

/**
 *
 * @author zoly
 */
public final class MultiStore implements MeasurementStore {

    private final MeasurementStore[] stores;
    private final TLongObjectMap<long[]> idToIds;
    private final TObjectLongMap<MeasurementsInfo> infoToId;
    private long idSeq;

    public MultiStore(final MeasurementStore ... stores) {
        if (stores.length <= 1) {
            throw new IllegalArgumentException("You need to supply more than 1 store, not " + Arrays.toString(stores));
        }
        this.stores = stores;
        this.idToIds = new TLongObjectHashMap<>();
        this.infoToId = new TObjectLongHashMap<>();
        this.idSeq = 1L;
    }


    @Override
    public long alocateMeasurements(final MeasurementsInfo measurement,
            final int sampleTimeMillis) throws IOException {
        IOException ex = null;
        synchronized (idToIds) {
            long id = infoToId.get(measurement);
            if (id <= 0) {
                long[] ids = new long[stores.length];
                int i = 0;
                for (MeasurementStore store : stores) {
                    try {
                        ids[i++] = store.alocateMeasurements(measurement, sampleTimeMillis);
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
                id = idSeq++;
                infoToId.put(measurement, id);
                idToIds.put(id, ids);
            }
            return id;
        }
    }

    @Override
    public void saveMeasurements(final long tableId,
            final long timeStampMillis,  final long... measurements) throws IOException {
                IOException ex = null;
        long[] ids;
        synchronized (idToIds) {
            ids = idToIds.get(tableId);
        }
        if (ids == null) {
            throw new IOException("Table id is invalid " + tableId);
        }
        int i = 0;
        for (MeasurementStore store : stores) {
            try {
                store.saveMeasurements(ids[i++], timeStampMillis, measurements);
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

    public List<MeasurementStore> getStores() {
        return Collections.unmodifiableList(Arrays.asList(stores));
    }

    @Override
    public String toString() {
        return "MultiStore{" + "stores=" + Arrays.toString(stores) + '}';
    }

}
