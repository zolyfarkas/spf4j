
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

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.jmx.Registry;
import org.spf4j.perf.MeasurementStore;

public final class Flusher {
   
    private Flusher() { }
    
    public static void flushEvery(final int intervalMillis, final MeasurementStore store) {
        final ScheduledFuture<?> future = DefaultScheduler.INSTANCE.scheduleAtFixedRate(new AbstractRunnable(false) {
            @Override
            public void doRun() throws Exception {
                store.flush();
            }
        }, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(new AbstractRunnable(false) {
            @Override
            public void doRun() throws Exception {
                try {
                    future.cancel(false);
                } finally {
                    store.close();
                }
            }
        }, "MS " + store + " shutdown"));
        Registry.export(store.getClass().getName(),
                    store.toString(), store);
    }
}
