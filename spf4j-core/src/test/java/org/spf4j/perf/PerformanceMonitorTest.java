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

import java.util.concurrent.Callable;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.perf.annotations.RecorderSourceInstance;

/**
 *
 * @author zoly
 */
public final class PerformanceMonitorTest {
    


    @Test
    public void testSomeMethod() throws Exception {
        String result = PerformanceMonitor.callAndMonitor(RecorderSourceInstance.RsNop.INSTANCE,
                1, 2, new Callable<String>() {

            @Override
            public String call() throws Exception {
                System.out.println("testing");
                Thread.sleep(3);
                return "test";
            }
        }, "test callable");
        Assert.assertEquals("test", result);
    }
}