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
package org.spf4j.perf.io;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.perf.impl.RecorderFactory;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.spf4j.os.OperatingSystem;
import org.spf4j.base.Runtime;
import org.spf4j.base.SysExits;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.Registry;
import org.spf4j.perf.MeasurementRecorder;
import org.spf4j.tsdb2.avro.MeasurementType;
import org.spf4j.unix.Lsof;

/**
 * This class allows you to poll and recordAt to a file the heap commitxted and heap used for your java process. start
 * data recording by calling the startMemoryUsageSampling method, stop the data recording by calling the method:
 * startMemoryUsageSampling.
 *
 * @author zoly
 */
public final class OpenFilesSampler {


  private static ScheduledFuture<?> samplingFuture;
  private static AccumulatorRunnable accumulator;

  private static volatile CharSequence lastWarnLsof = "";

  private static volatile Instant lastWarnLsofTime;

  static {
    Runtime.queueHook(2, new AbstractRunnable(true) {
      @Override
      public void doRun() {
        stop();
      }
    });
    Registry.export(OpenFilesSampler.class);
  }

  private OpenFilesSampler() {
  }

  public static void start(final long sampleTimeMillis) {
    long maxFileDescriptorCount = OperatingSystem.getMaxFileDescriptorCount();
    start(sampleTimeMillis, maxFileDescriptorCount - maxFileDescriptorCount / 10,
            maxFileDescriptorCount, true);
  }

  @JmxExport
  public static void start(@JmxExport("sampleTimeMillis") final long sampleTimeMillis,
          @JmxExport("shutdownOnError") final boolean shutdownOnError) {
    long maxFileDescriptorCount = OperatingSystem.getMaxFileDescriptorCount();
    start(sampleTimeMillis, maxFileDescriptorCount - maxFileDescriptorCount / 10,
            maxFileDescriptorCount, shutdownOnError);
  }

  @JmxExport
  public static String getWarnLsofDetail() {
    return lastWarnLsof.toString();
  }

  @JmxExport
  @Nullable
  public static synchronized String getWarnLsofTime() {
    Instant lwt = lastWarnLsofTime;
    if (lwt == null) {
      return null;
    } else {
      return lwt.toString();
    }
  }


  public static synchronized void start(final long sampleTimeMillis,
          final long warnThreshold, final long errorThreshold, final boolean shutdownOnError) {
    if (samplingFuture == null) {
      accumulator = new AccumulatorRunnable(errorThreshold, shutdownOnError,
              warnThreshold, (int) sampleTimeMillis);
      samplingFuture = DefaultScheduler.INSTANCE.scheduleWithFixedDelay(accumulator,
              sampleTimeMillis, sampleTimeMillis, TimeUnit.MILLISECONDS);
    } else {
      throw new IllegalStateException("Open file usage sampling already started " + samplingFuture);
    }
  }

  @JmxExport
  public static synchronized void stop()  {
    if (samplingFuture != null) {
      samplingFuture.cancel(false);
      samplingFuture = null;
      accumulator = null;
    }
  }

  @JmxExport
  public static synchronized boolean isStarted() {
    return samplingFuture != null;
  }

  @JmxExport
  public static String getLsof() {
    CharSequence lsofOutput = Lsof.getLsofOutput();
    return lsofOutput == null ? "unable to obtain lsof" : lsofOutput.toString();
  }

  @Deprecated
  public static long getMaxNrOpenFiles() {
    return OperatingSystem.getMaxFileDescriptorCount();
  }

  @Deprecated
  public static long getNrOpenFiles() {
    return OperatingSystem.getOpenFileDescriptorCount();
  }

  @JmxExport
  public static long getWarnThreshold() {
    if (accumulator == null) {
      return -1;
    }
    return accumulator.getWarnThreshold();
  }

  @JmxExport
  public static long getErrorThreshold() {
    if (accumulator == null) {
      return -1;
    }
    return accumulator.getErrorThreshold();
  }


  private static class AccumulatorRunnable extends AbstractRunnable {

    private final long errorThreshold;
    private final boolean shutdownOnError;
    private final long warnThreshold;
    private final MeasurementRecorder nrOpenFiles;

    AccumulatorRunnable(final long errorThreshold, final boolean shutdownOnError,
            final long warnThreshold, final int sampleMillis) {
      this.errorThreshold = errorThreshold;
      this.shutdownOnError = shutdownOnError;
      this.warnThreshold = warnThreshold;
      this.nrOpenFiles = RecorderFactory.createDirectRecorder("process.nr_open_files", "count",
              sampleMillis, MeasurementType.GAUGE);
    }

    @Override
    @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
    public void doRun() {
      long time = System.currentTimeMillis();
      long nrOf = OperatingSystem.getOpenFileDescriptorCount();
      if (nrOf > errorThreshold) {
        lastWarnLsof = Lsof.getLsofOutput();
        lastWarnLsofTime = Instant.now();
        Logger log = Logger.getLogger(OpenFilesSampler.class.getName());
        log.log(Level.SEVERE, "Nr open files is {0} and exceeds error threshold {1}, detail:\n{2}",
                new Object[] {nrOf, errorThreshold, lastWarnLsof});
        if (shutdownOnError) {
          Runtime.goDownWithError(null, SysExits.EX_IOERR);
        }
      } else if (nrOf > warnThreshold) {
        lastWarnLsof = Lsof.getLsofOutput();
        lastWarnLsofTime = Instant.now();
        Logger log = Logger.getLogger(OpenFilesSampler.class.getName());
        log.log(Level.WARNING, "Nr open files is {0} and exceeds warn threshold {1}, detail:\n{2} ",
                new Object[] {nrOf, warnThreshold, lastWarnLsof});
        if (!Runtime.gc(60000)) {
          log.warning("Unable to trigger GC although running low on file resources");
        } else {
          log.log(Level.WARNING, "gc executed nr open files reduced by {0} files",
                  nrOf - OperatingSystem.getOpenFileDescriptorCount());
        }
      }
      this.nrOpenFiles.recordAt(time, nrOf);
    }

    public long getErrorThreshold() {
      return errorThreshold;
    }

    public boolean isShutdownOnError() {
      return shutdownOnError;
    }

    public long getWarnThreshold() {
      return warnThreshold;
    }




  }

}
