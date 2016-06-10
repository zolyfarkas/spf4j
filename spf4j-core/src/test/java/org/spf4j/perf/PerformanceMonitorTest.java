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
package org.spf4j.perf;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.Callable;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
public final class PerformanceMonitorTest {



    @Test
    public void testSomeMethod() throws Exception {
        String result = PerformanceMonitor.callAndMonitor(
                1, 2, new Callable<String>() {

            @Override
            @SuppressFBWarnings("MDM_THREAD_YIELD")
            public String call() throws Exception {
                System.out.println("testing");
                Thread.sleep(3);
                return "test";
            }

            @Override
            public String toString() {
                return "test";
            }

        });
        // TODO assert logging, will need to create my test logger component...
        Assert.assertEquals("test", result);
    }
}