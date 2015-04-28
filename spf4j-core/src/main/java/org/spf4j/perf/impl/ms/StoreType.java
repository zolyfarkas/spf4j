
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
import java.net.URISyntaxException;
import org.spf4j.jmx.Registry;
import org.spf4j.perf.MeasurementStore;
import org.spf4j.perf.impl.NopMeasurementStore;
import org.spf4j.perf.impl.ms.graphite.GraphiteTcpStore;
import org.spf4j.perf.impl.ms.graphite.GraphiteUdpStore;
import org.spf4j.perf.impl.ms.tsdb.TSDBMeasurementStore;
import org.spf4j.perf.impl.ms.tsdb.TSDBTxtMeasurementStore;
import org.spf4j.recyclable.ObjectCreationException;

/**
 *
 * @author zoly
 */
public enum StoreType {
    TSDB(new StoreFactory() {
        @Override
        public MeasurementStore create(final String config) throws IOException {
            return new TSDBMeasurementStore(config);
        }
    }),
    TSDB_TXT(new StoreFactory() {
        @Override
        public MeasurementStore create(final String config) throws IOException {
            return new TSDBTxtMeasurementStore(config);
        }
    }),
    GRAPHITE_UDP(new StoreFactory() {
        @Override
        public MeasurementStore create(final String config) throws IOException, ObjectCreationException {
            try {
                return new GraphiteUdpStore(config);
            } catch (URISyntaxException ex) {
                throw new IllegalArgumentException("Invalid configuration " + config, ex);
            }
        }
    }),
    GRAPHITE_TCP(new StoreFactory() {
        @Override
        public MeasurementStore create(final String config) throws IOException, ObjectCreationException {
            try {
                return new GraphiteTcpStore(config);
            } catch (URISyntaxException ex) {
                throw new IllegalArgumentException("Invalid configuration " + config, ex);
            }
        }
    }),
    NOP_STORE(new StoreFactory() {

        @Override
        public MeasurementStore create(final String config) throws IOException, ObjectCreationException {
            return new NopMeasurementStore();
        }
    });
    private final StoreFactory factory;

    private StoreType(final StoreFactory factory) {
        this.factory = factory;
    }

    public MeasurementStore create(final String configuration) throws IOException, ObjectCreationException {
        MeasurementStore store =  factory.create(configuration);
        Registry.export(store.getClass().getName(),
                    store.toString(), store);
        return store;
    }

}
