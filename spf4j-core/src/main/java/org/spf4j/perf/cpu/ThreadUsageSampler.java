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
package org.spf4j.perf.cpu;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.base.Threads;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.io.ByteArrayBuilder;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.Registry;
import org.spf4j.perf.MeasurementRecorder;
import org.spf4j.perf.impl.RecorderFactory;

/**
 *
 * @author zoly
 */
public final class ThreadUsageSampler {

  private static final ThreadMXBean TH_BEAN = ManagementFactory.getThreadMXBean();

  private static ScheduledFuture<?> samplingFuture;

  private static final List<String> PEAK_THREAD_NAMES = new ArrayList<>();
  private static final List<StackTraceElement[]> PEAK_THREAD_TRACES = new ArrayList<>();
  private static final BitSet PEAK_THREAD_DAEMON = new BitSet();

  static {
    org.spf4j.base.Runtime.queueHook(2, new AbstractRunnable(true) {
      @Override
      public void doRun() {
        stop();
        String pto = System.getProperty("spf4j.threadUsageSampler.peakThreadsOnShutdown", "out");
        switch (pto) {
          case "out":
          case "info":
            logPeakThreadInfo(Level.INFO);
            break;
          case "err":
          case "warn":
            logPeakThreadInfo(Level.WARNING);
            break;
          case "none":
            break;
          default:
            throw new IllegalArgumentException("Invalida settign for spf4j.threadUsageSampler.peakThreadsOnShutdown: "
                    + pto);
        }
      }

    });
    Registry.export(ThreadUsageSampler.class);
  }

  private ThreadUsageSampler() {
  }

  public static void writePeakThreadInfo(final PrintStream out) {
    if (!PEAK_THREAD_NAMES.isEmpty()) {
      out.println("Peak Threads:");
      int i = 0;
      boolean haveStacktraces = !PEAK_THREAD_TRACES.isEmpty();
      for (String tname : PEAK_THREAD_NAMES) {
        out.print(tname);
        out.print(", daemon =");
        out.print(PEAK_THREAD_DAEMON.get(i));
        out.print(',');
        if (haveStacktraces) {
          out.print(' ');
          out.print(Arrays.toString(PEAK_THREAD_TRACES.get(i)));
        }
        out.println();
        i++;
      }
    }
  }

  public static void logPeakThreadInfo(final Level level) {
    if (!PEAK_THREAD_NAMES.isEmpty()) {
      Logger logger = Logger.getLogger(ThreadUsageSampler.class.getName());
      int i = 0;
      for (String tname : PEAK_THREAD_NAMES) {
        logger.log(level, "PeakThread({0}), daemon={1}, trace -> {2}",
                new Object[] {tname, PEAK_THREAD_DAEMON.get(i), Arrays.toString(PEAK_THREAD_TRACES.get(i))});
        i++;
      }
    }
  }


  @JmxExport
  @SuppressFBWarnings({"DM_DEFAULT_ENCODING", "NP_LOAD_OF_KNOWN_NULL_VALUE"})
  public static String getPeakThreadInfo() {
    try (ByteArrayBuilder bab = new ByteArrayBuilder()) {
      PrintStream ps = new PrintStream(bab);
      writePeakThreadInfo(ps);
      return bab.toString(Charset.defaultCharset());
    }
  }

  @JmxExport
  public static String getCurrentAliveThreadInfo() {
    StringBuilder sb = new StringBuilder(2048);
    Thread[] threads = Threads.getThreads();
    StackTraceElement[][] stackTraces = Threads.getStackTraces(threads);
    for (int i = 0; i < threads.length; i++) {
      Thread t = threads[i];
      if (t.isAlive()) {
        sb.append(t.getId());
        sb.append(",\t").append(t.getName());
        sb.append(",\t state =").append(t.getState());
        sb.append(",\t daemon =").append(t.isDaemon());
        sb.append(",\t");
        StackTraceElement[] straces = stackTraces[i];
        if (straces != null && straces.length > 0) {
          sb.append(' ');
          sb.append(Arrays.toString(straces));
        }
        sb.append('\n');
      }
    }
    return sb.toString();
  }


  public static synchronized void start(final int sampleTime) {
    start(sampleTime, true);
  }

  @JmxExport
  public static synchronized void start(@JmxExport("sampleTimeMillis") final int sampleTime,
          @JmxExport("withStackTraces") final boolean withStackTraces) {
    if (samplingFuture == null) {
      samplingFuture = DefaultScheduler.INSTANCE.scheduleWithFixedDelay(
              new ThreadStateRecorder(sampleTime, withStackTraces), sampleTime, sampleTime, TimeUnit.MILLISECONDS);
    } else {
      throw new IllegalStateException("Thread sampling already started " + samplingFuture);
    }
  }

  @JmxExport
  public static synchronized void stop() {
    if (samplingFuture != null) {
      samplingFuture.cancel(false);
      samplingFuture = null;
    }
  }

  @JmxExport
  public static synchronized boolean isStarted() {
    return samplingFuture != null;
  }

  private static final class ThreadStateRecorder extends AbstractRunnable {

    private final MeasurementRecorder cpuUsage;
    private final boolean withStackTraces;

    ThreadStateRecorder(final int sampleTime, final boolean withStackTraces) {
      this.cpuUsage
              = RecorderFactory.createDirectRecorder("peak-thread-count", "count", sampleTime);
      this.withStackTraces = withStackTraces;
    }
    private int maxThreadsNr = 0;

    @Override
    public void doRun() {
      final int peakThreadCount = TH_BEAN.getPeakThreadCount();
      cpuUsage.record(peakThreadCount);
      if (peakThreadCount > maxThreadsNr) {
        Thread[] ths = Threads.getThreads();
        if (ths.length > PEAK_THREAD_NAMES.size()) {
          if (withStackTraces) {
            StackTraceElement[][] stackTraces = Threads.getStackTraces(ths);
            PEAK_THREAD_TRACES.clear();
            PEAK_THREAD_TRACES.addAll(Arrays.asList(stackTraces));
          }

          PEAK_THREAD_NAMES.clear();
          int i = 0;
          for (Thread th : ths) {
            PEAK_THREAD_NAMES.add(th.getName());
            if (th.isDaemon()) {
              PEAK_THREAD_DAEMON.set(i);
            } else {
              PEAK_THREAD_DAEMON.clear(i);
            }
            i++;
          }
        }
        maxThreadsNr = peakThreadCount;
      }
      TH_BEAN.resetPeakThreadCount();
    }
  }

}
