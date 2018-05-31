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
package org.spf4j.concurrent;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.spf4j.base.AbstractRunnable;

/**
 *
 * @author zoly
 */
public final class DefaultScheduler {

  private static final long HOUR_MILLIS = 3600000;

  private static final long DAY_MILLIS = HOUR_MILLIS * 24;

  public static final ScheduledExecutorService INSTANCE
          = new ScheduledThreadPoolExecutor(
                  Integer.getInteger("spf4j.executors.defaultScheduler.coreThreads", 2),
                  new CustomThreadFactory("DefaultScheduler",
                          Boolean.getBoolean("spf4j.executors.defaultScheduler.daemon"),
                          Integer.getInteger("spf4j.executors.defaultScheduler.priority", Thread.NORM_PRIORITY)));

  public static final ListeningScheduledExecutorService LISTENABLE_INSTANCE
          = MoreExecutors.listeningDecorator(INSTANCE);

  static {
    org.spf4j.base.Runtime.queueHookAtEnd(new AbstractRunnable(true) {

      @Override
      public void doRun() throws InterruptedException {
        INSTANCE.shutdown();
        INSTANCE.awaitTermination(org.spf4j.base.Runtime.WAIT_FOR_SHUTDOWN_NANOS, TimeUnit.NANOSECONDS);
        List<Runnable> remaining = INSTANCE.shutdownNow();
        if (remaining.size() > 0) {
          org.spf4j.base.Runtime.error("Remaining tasks: " + remaining);
        }
      }
    });
  }

  public static ScheduledExecutorService instance() {
    return INSTANCE;
  }

  public static ListeningScheduledExecutorService listenableInstance() {
    return LISTENABLE_INSTANCE;
  }

  private DefaultScheduler() {
  }

  /**
   * this will schedule a runnable aligned to the hour or day at a fixed rate.
   *
   * @param command - the Runnable to execute.
   * @param millisInterval - the schedule interval in milliseconds.
   * @return - Future that allows to cancel the schedule.
   */
  public static ScheduledFuture<?> scheduleAllignedAtFixedRateMillis(
          final Runnable command, final long millisInterval) {
    long currentTime = System.currentTimeMillis();
    long nextScheduleDelay;
    if (millisInterval < HOUR_MILLIS) {
      long millisPastHour = currentTime % HOUR_MILLIS;
      nextScheduleDelay =  millisInterval - millisPastHour % millisInterval;
    } else {
      long millisPastDay = currentTime % DAY_MILLIS;
      nextScheduleDelay =  millisInterval - millisPastDay % millisInterval;
    }
    return INSTANCE.scheduleAtFixedRate(
            command, nextScheduleDelay, millisInterval, TimeUnit.MILLISECONDS);
  }
}
