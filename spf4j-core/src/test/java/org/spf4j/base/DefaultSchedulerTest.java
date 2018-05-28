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
import java.time.Instant;
import org.spf4j.concurrent.DefaultScheduler;
import java.util.concurrent.ScheduledFuture;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zoly
 */
@SuppressFBWarnings("MDM_THREAD_YIELD")
public final class DefaultSchedulerTest {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultSchedulerTest.class);

  private volatile boolean notAligned = false;

  /**
   * Test of scheduleAllignedAtFixedRateMillis method, of class DefaultScheduler.
   */
  @Test
  public void testScheduleAllignedAtFixedRateMillis() throws InterruptedException {

    Runnable command = new Runnable() {
      private volatile boolean first = true;

      @Override
      public void run() {
        long time = System.currentTimeMillis();
        if (time % 1000 >= 100) {
          notAligned = true;
        }
        LOG.debug("scheduled at {}", Instant.ofEpochMilli(time));
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
