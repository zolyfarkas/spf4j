
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
package org.spf4j.perf.io;

import org.spf4j.base.AbstractRunnable;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.perf.MeasurementRecorder;
import org.spf4j.perf.impl.RecorderFactory;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Runtime;

/**
 * This class allows you to poll and record to a file the heap commited and heap used
 * for your java process.
 *  start data recording by calling the startMemoryUsageSampling method,
 *  stop the data recording by calling the method: startMemoryUsageSampling.
 *
 * @author zoly
 */
public final class OpenFilesSampler {

    private OpenFilesSampler() { }

    private static final int AGG_INTERVAL =
            Integer.parseInt(System.getProperty("perf.io.openFiles.sampleAggMillis", "600000"));


    private static final MeasurementRecorder NR_OPEN_FILES =
            RecorderFactory.createScalableMinMaxAvgRecorder("nr-open-files", "count", AGG_INTERVAL);

    private static ScheduledFuture<?> samplingFuture;

    private static final Logger LOG = LoggerFactory.getLogger(OpenFilesSampler.class);


    static {
        java.lang.Runtime.getRuntime().addShutdownHook(new Thread(new AbstractRunnable(true) {
            @Override
            public void doRun() throws Exception {
                stopFileUsageSampling();
            }
        }, "shutdown-memory-sampler"));
    }

    public static void startFileUsageSampling(final long sampleTimeMillis) {
        startFileUsageSampling(sampleTimeMillis, Runtime.Ulimit.MAX_NR_OPENFILES - Runtime.Ulimit.MAX_NR_OPENFILES / 10,
                Runtime.Ulimit.MAX_NR_OPENFILES, true);
    }

    public static void startFileUsageSampling(final long sampleTimeMillis, final boolean shutdownOnError) {
        startFileUsageSampling(sampleTimeMillis, Runtime.Ulimit.MAX_NR_OPENFILES - Runtime.Ulimit.MAX_NR_OPENFILES / 10,
                Runtime.Ulimit.MAX_NR_OPENFILES, shutdownOnError);
    }

    public static synchronized void startFileUsageSampling(final long sampleTimeMillis,
            final int warnThreshold, final int errorThreshold, final boolean shutdownOnError) {
        if (samplingFuture == null) {
            samplingFuture = DefaultScheduler.INSTANCE.scheduleWithFixedDelay(new AbstractRunnable() {

                @Override
                public void doRun() throws Exception {
                    int nrOpenFiles = Runtime.getNrOpenFiles();
                    if (nrOpenFiles > errorThreshold) {
                        LOG.error("Nr open files is {} and exceeds error threshold {}, detail:\n{}",
                                nrOpenFiles, errorThreshold, Runtime.getLsofOutput());
                        if (shutdownOnError) {
                            Runtime.goDownWithError(null, 666);
                        }
                    } else if (nrOpenFiles > warnThreshold) {
                        LOG.warn("Nr open files is {} and exceeds warn threshold {}, detail:\n{} ",
                                nrOpenFiles, warnThreshold, Runtime.getLsofOutput());
                        Runtime.gc(60000); // this is to ameliorate a leak.
                    }
                    NR_OPEN_FILES.record(nrOpenFiles);
                }
            }, sampleTimeMillis, sampleTimeMillis, TimeUnit.MILLISECONDS);
        }
    }

    public static synchronized void stopFileUsageSampling() {
         if (samplingFuture != null) {
             samplingFuture.cancel(false);
             samplingFuture = null;
         }
    }



}
