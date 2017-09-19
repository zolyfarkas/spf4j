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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.os.OperatingSystem;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.Registry;
import org.spf4j.perf.MeasurementRecorder;
import org.spf4j.perf.impl.RecorderFactory;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("IICU_INCORRECT_INTERNAL_CLASS_USE")
public final class CpuUsageSampler {

  private static ScheduledFuture<?> samplingFuture;

  static {
    Registry.export(CpuUsageSampler.class);
    if (OperatingSystem.getSunJdkOSMBean() != null) {
      org.spf4j.base.Runtime.queueHook(2, new AbstractRunnable(true) {
        @Override
        public void doRun() {
          stop();
        }
      });
    }
  }

  private CpuUsageSampler() {
  }


  public static long getProcessCpuTimeNanos() {
    com.sun.management.OperatingSystemMXBean sunJdkOSMBean = OperatingSystem.getSunJdkOSMBean();
    if (sunJdkOSMBean != null) {
      return sunJdkOSMBean.getProcessCpuTime();
    } else {
      return -1;
    }
  }

  @JmxExport
  public static synchronized void start(@JmxExport("sampleTimeMillis") final int sampleTime) {
    if (samplingFuture == null) {
      final MeasurementRecorder cpuUsage
              = RecorderFactory.createDirectRecorder("cpu-time", "ns", sampleTime);
      samplingFuture = DefaultScheduler.INSTANCE.scheduleWithFixedDelay(new AbstractRunnable() {

        private long lastValue = 0;

        @Override
        public void doRun() {
          long currTime = getProcessCpuTimeNanos();
          cpuUsage.record(currTime - lastValue);
          lastValue = currTime;
        }
      }, sampleTime, sampleTime, TimeUnit.MILLISECONDS);
    } else {
      throw new IllegalStateException("Cpu time Sampling already started " + samplingFuture);
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

}
