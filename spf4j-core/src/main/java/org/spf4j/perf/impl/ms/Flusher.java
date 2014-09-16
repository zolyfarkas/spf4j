
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
