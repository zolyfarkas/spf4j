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
package org.spf4j.perf.impl.ms;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
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
        @SuppressFBWarnings("PATH_TRAVERSAL_IN") // not supplied by user
        public MeasurementStore create(final String pconfig) throws IOException {
            String config;
            if (!pconfig.endsWith("tsdb2"))  {
                config = pconfig + ".tsdb2";
            } else {
                config = pconfig;
            }
            return new TSDBMeasurementStore(new File(config));
        }
    }),
    TSDB_TXT(new StoreFactory() {
        @Override
        @SuppressFBWarnings("PATH_TRAVERSAL_IN") // not supplied by user
        public MeasurementStore create(final String config) throws IOException {
            return new TSDBTxtMeasurementStore(new File(config));
        }
    }),
    GRAPHITE_UDP(new StoreFactory() {
        @Override
        public MeasurementStore create(final String config) throws ObjectCreationException {
            try {
                return new GraphiteUdpStore(config);
            } catch (URISyntaxException ex) {
                throw new IllegalArgumentException("Invalid configuration " + config, ex);
            }
        }
    }),
    GRAPHITE_TCP(new StoreFactory() {
        @Override
        public MeasurementStore create(final String config) throws ObjectCreationException {
            try {
                return new GraphiteTcpStore(config);
            } catch (URISyntaxException ex) {
                throw new IllegalArgumentException("Invalid configuration " + config, ex);
            }
        }
    }),
    NOP_STORE(new StoreFactory() {

        @Override
        public MeasurementStore create(final String config) {
            return new NopMeasurementStore();
        }
    });
    private final StoreFactory factory;

    StoreType(final StoreFactory factory) {
        this.factory = factory;
    }

    public MeasurementStore create(final String configuration) throws IOException, ObjectCreationException {
        MeasurementStore store =  factory.create(configuration);
        Registry.exportIfNeeded(store.getClass().getName(),
                    store.toString(), store);
        return store;
    }

}
