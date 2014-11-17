
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
package org.spf4j.concurrent;

import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 *
 * @author zoly
 */
public final class DefaultScheduler {

    private DefaultScheduler() { }
    
    
    public static final ScheduledExecutorService INSTANCE =
            MoreExecutors.getExitingScheduledExecutorService(
            new ScheduledThreadPoolExecutor(Integer.getInteger("default.scheduler.coreThreads", 2),
            new CustomThreadFactory("DefaultScheduler", false)));

    
    private static final long HOUR_MILLIS = 3600000;
    
    private static final long DAY_MILLIS = HOUR_MILLIS * 24;
    
    /**
     * this will schedule a runnable aligned to the hour or day.
     * @param command
     * @param millisInterval
     * @return Future that allows to cancel the schedule.
     */
    public static ScheduledFuture<?> scheduleAllignedAtFixedRateMillis(
            final Runnable command, final long millisInterval) {
       long currentTime = System.currentTimeMillis();
       long nextScheduleTime;
       if (millisInterval < HOUR_MILLIS) {
            long millisPastHour = currentTime % HOUR_MILLIS;
            nextScheduleTime = (millisPastHour / millisInterval + 1) * millisInterval + currentTime - millisPastHour;
       } else {
           long millisPastDay = currentTime % DAY_MILLIS;
           nextScheduleTime = (millisPastDay / millisInterval + 1) * millisInterval + currentTime - millisPastDay;
       }
       return INSTANCE.scheduleAtFixedRate(
               command, nextScheduleTime - currentTime, millisInterval, TimeUnit.MILLISECONDS);
    }
}
