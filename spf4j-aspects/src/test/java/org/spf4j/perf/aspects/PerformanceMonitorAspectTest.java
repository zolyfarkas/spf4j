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
package org.spf4j.perf.aspects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.spf4j.annotations.PerformanceMonitor;
import org.junit.Test;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.Registry;
import org.spf4j.test.log.Level;
import org.spf4j.test.log.LogAssert;
import org.spf4j.test.log.TestLoggers;
import org.spf4j.test.matchers.LogMatchers;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("MDM_THREAD_YIELD")
public final class PerformanceMonitorAspectTest {

  /**
   * Test of performanceMonitoredMethod method, of class PerformanceMonitorAspect.
   */
  @Test
  public void testPerformanceMonitoredMethod() throws Exception {
    Registry.export(this);
    LogAssert expect = TestLoggers.sys().expect("org.spf4j.perf.aspects.PerformanceMonitorAspect", Level.WARN,
            LogMatchers.hasFormat("Execution time  {} ms for {} exceeds warning threshold of {} ms, arguments {}"));
    for (int i = 0; i < 10; i++) {
      somethingTomeasure(i, "Test");
    }
    expect.assertObservation();
  }

  @PerformanceMonitor(warnThresholdMillis = 1)
  @JmxExport
  public void somethingTomeasure(final int arg1, final String arg2) throws InterruptedException {
    Thread.sleep(10);
  }

}
