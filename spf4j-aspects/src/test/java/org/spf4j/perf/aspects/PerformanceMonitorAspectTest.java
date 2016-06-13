
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
package org.spf4j.perf.aspects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.spf4j.annotations.PerformanceMonitor;
import org.junit.Test;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.Registry;

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
        for (int i = 0; i < 10; i++) {
            somethingTomeasure(i, "Test");
        }
    }

    @PerformanceMonitor(warnThresholdMillis = 1)
    @JmxExport
    public void somethingTomeasure(final int arg1, final String arg2) throws InterruptedException {
        Thread.sleep(10);
    }

}
