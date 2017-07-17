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
package org.spf4j.base;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.spf4j.concurrent.DefaultScheduler;
import java.util.concurrent.ScheduledFuture;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author zoly
 */
@SuppressFBWarnings("MDM_THREAD_YIELD")
public final class DefaultSchedulerTest {


    private volatile boolean notAligned = false;
    /**
     * Test of scheduleAllignedAtFixedRateMillis method, of class DefaultScheduler.
     */
    @Test
    public void testScheduleAllignedAtFixedRateMillis() throws InterruptedException {
        System.out.println(100.123456789012345);
        System.out.println(0.123456789012345678);

        System.out.println(0.123456789012345678
                + 0.123456789012345678
                + 100.123456789012345);

        System.out.println(100.123456789012345
                + 0.123456789012345678
                + 0.123456789012345678);


        Runnable command = new Runnable() {
            private boolean first = true;

            @Override
            public void run() {
                long time = System.currentTimeMillis();
                if (time % 1000 >= 50) {
                    notAligned = true;
                }
                System.out.println(time);
                if (first) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        return;
                    }
                    first = false;
                }
            }
        };
        long millisInterval = 1000L;
        ScheduledFuture result = DefaultScheduler.scheduleAllignedAtFixedRateMillis(command, millisInterval);
        Thread.sleep(10000);
        result.cancel(true);
        if (notAligned) {
            Assert.fail("Scheduled tasks not alligned");
        }
    }
}
