
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
package org.spf4j.perf.memory;

import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class GCUsageSamplerTest {

    public GCUsageSamplerTest() {
    }

    @Test
    public void testSomeMethod() throws InterruptedException {
        System.setProperty("perf.memory.sampleAggMillis", "1000");
        GCUsageSampler.start(100);
        MemoryUsageSampler.start(100);
        for (int i = 0; i < 100000; i++) {
            String str = Integer.toString(i);
        }
        Thread.sleep(1000);
        MemoryUsageSampler.stop();
        GCUsageSampler.stop();
    }
}