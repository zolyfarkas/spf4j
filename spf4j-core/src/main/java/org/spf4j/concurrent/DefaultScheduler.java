
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

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.spf4j.base.AbstractRunnable;
import static org.spf4j.base.Runtime.WAIT_FOR_SHUTDOWN_MILLIS;


/**
 *
 * @author zoly
 */
public final class DefaultScheduler {

    private DefaultScheduler() { }


    public static final ScheduledExecutorService INSTANCE =
            new ScheduledThreadPoolExecutor(Integer.getInteger("defaultScheduler.coreThreads", 2),
            new CustomThreadFactory("DefaultScheduler", Boolean.getBoolean("defaultScheduler.daemon"),
            Integer.getInteger("defaultScheduler.priority", Thread.NORM_PRIORITY)));

    public static final ListeningScheduledExecutorService LISTENABLE_INSTANCE =
            MoreExecutors.listeningDecorator(INSTANCE);


    static {
        org.spf4j.base.Runtime.queueHookAtEnd(new AbstractRunnable(true) {

                @Override
                public void doRun() throws InterruptedException {
                    INSTANCE.shutdown();
                    INSTANCE.awaitTermination(WAIT_FOR_SHUTDOWN_MILLIS, TimeUnit.MILLISECONDS);
                    List<Runnable> remaining = INSTANCE.shutdownNow();
                    if (remaining.size() > 0) {
                        System.err.println("Remaining tasks: " + remaining);
                    }
                }
        });
    }

    private static final long HOUR_MILLIS = 3600000;

    private static final long DAY_MILLIS = HOUR_MILLIS * 24;

    /**
     * this will schedule a runnable aligned to the hour or day at a fixed rate.
     * @param command - the Runnable to execute.
     * @param millisInterval - the schedule interval in milliseconds.
     * @return - Future that allows to cancel the schedule.
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
