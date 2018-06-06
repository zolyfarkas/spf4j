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
package org.spf4j.perf.memory;

//CHECKSTYLE:OFF
import com.sun.management.HotSpotDiagnosticMXBean;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
//CHECKSTYLE:ON
import java.io.IOException;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.perf.impl.RecorderFactory;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.management.MBeanServer;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.Registry;
import org.spf4j.perf.CloseableMeasurementRecorder;

/**
 * This class allows you to poll and recordAt to a file the heap committed and heap used for your java process. start
 * data recording by calling the startMemoryUsageSampling method, stop the data recording by calling the method:
 * startMemoryUsageSampling.
 *
 * @author zoly
 */
@SuppressFBWarnings("IICU_INCORRECT_INTERNAL_CLASS_USE")
public final class MemoryUsageSampler {

  private static final String HOTSPOT_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";

  private static final MemoryMXBean MBEAN = ManagementFactory.getMemoryMXBean();

  private static final HotSpotDiagnosticMXBean HOTSPOT_DIAGNOSTIC_INSTANCE = getHotspotMBean();

  private static ScheduledFuture<?> samplingFuture;
  private static AccumulatorRunnable accumulatorRunnable;

  static {
    org.spf4j.base.Runtime.queueHook(2, new AbstractRunnable(true) {
      @Override
      public void doRun() throws IOException {
        stop();
      }
    });
    Registry.export(MemoryUsageSampler.class);
  }

  private static HotSpotDiagnosticMXBean getHotspotMBean() {
    try {
      MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      return ManagementFactory.newPlatformMXBeanProxy(server,
              HOTSPOT_BEAN_NAME, HotSpotDiagnosticMXBean.class);
    } catch (IOException ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  private MemoryUsageSampler() {
  }

  public static synchronized void start(final long sampleTimeMilis) {
    start((int) sampleTimeMilis, (int) sampleTimeMilis * 10);
  }

  public static synchronized void start(final int sampleTimeMilis) {
    start(sampleTimeMilis, sampleTimeMilis * 10);
  }

  @JmxExport
  public static synchronized void start(@JmxExport("sampleTimeMillis") final int sampleTimeMilis,
          @JmxExport("accumulateIntervalMillis") final int accumulateIntervalMillis) {
    if (samplingFuture == null) {
      accumulatorRunnable = new AccumulatorRunnable(accumulateIntervalMillis);
      samplingFuture = DefaultScheduler.INSTANCE.scheduleWithFixedDelay(accumulatorRunnable,
              sampleTimeMilis, sampleTimeMilis, TimeUnit.MILLISECONDS);
    } else {
      throw new IllegalStateException("Memory usage sampling already started " + samplingFuture);
    }
  }

  @JmxExport
  public static synchronized void stop() throws IOException {
    if (samplingFuture != null) {
      samplingFuture.cancel(false);
      accumulatorRunnable.close();
      samplingFuture = null;
    }
  }

  @JmxExport
  public static synchronized boolean isStarted() {
    return samplingFuture != null;
  }

  private static class AccumulatorRunnable extends AbstractRunnable implements AutoCloseable {

    private final CloseableMeasurementRecorder heapCommited;
    private final CloseableMeasurementRecorder heapUsed;

    AccumulatorRunnable(final int accumulationIntervalMillis) {
      heapCommited
              = RecorderFactory.createScalableMinMaxAvgRecorder2("heap-commited", "bytes", accumulationIntervalMillis);
      heapUsed
              = RecorderFactory.createScalableMinMaxAvgRecorder2("heap-used", "bytes", accumulationIntervalMillis);
    }

    @Override
    public void doRun() throws Exception {
      MemoryUsage usage = MBEAN.getHeapMemoryUsage();
      heapCommited.record(usage.getCommitted());
      heapUsed.record(usage.getUsed());
    }

    @Override
    public void close() {
      heapUsed.close();
      heapCommited.close();
    }
  }

  public static void dumpHeap(final String filename, final boolean liveObjectOnly) throws IOException {
    HOTSPOT_DIAGNOSTIC_INSTANCE.dumpHeap(filename, liveObjectOnly);
  }

  public static HotSpotDiagnosticMXBean getHotspotDiagnosticBean() {
    return HOTSPOT_DIAGNOSTIC_INSTANCE;
  }

}
