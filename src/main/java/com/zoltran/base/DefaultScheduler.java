/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.base;

import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author zoly
 */
public final class DefaultScheduler {

    private DefaultScheduler() {}
    
    
    /**
     * The default thread factory
     */
    static class DefaultThreadFactory implements ThreadFactory {
        static final AtomicInteger poolNumber = new AtomicInteger(1);
        final ThreadGroup group;
        final AtomicInteger threadNumber = new AtomicInteger(1);
        final String namePrefix;

        DefaultThreadFactory(String name) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null)? s.getThreadGroup() :
                                 Thread.currentThread().getThreadGroup();
            namePrefix = name +
                          poolNumber.getAndIncrement() +
                         "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                                  namePrefix + threadNumber.getAndIncrement(),
                                  0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
    
    
    public static final ScheduledExecutorService INSTANCE = 
            MoreExecutors.getExitingScheduledExecutorService(
            new ScheduledThreadPoolExecutor(2,
            new DefaultThreadFactory("DefaultScheduler")))
            
            ;

    
    private static final long HOUR_MILLIS = 3600000;
    
    private static final long DAY_MILLIS = HOUR_MILLIS *24 ;
    
    /**
     * this will schedule a runnable aligned to the hour or day.
     * @param command
     * @param millisInterval
     * @return 
     */
    public static ScheduledFuture<?> scheduleAllignedAtFixedRateMillis(Runnable command,long millisInterval) {
       long currentTime = System.currentTimeMillis();
       long nextScheduleTime;
       if (millisInterval < HOUR_MILLIS) {
            long millisPastHour = currentTime % HOUR_MILLIS;
            nextScheduleTime = (millisPastHour / millisInterval + 1)* millisInterval + currentTime - millisPastHour;
       } else {
           long millisPastDay = currentTime % DAY_MILLIS;
           nextScheduleTime = (millisPastDay / millisInterval + 1)* millisInterval + currentTime - millisPastDay;
       }
       return INSTANCE.scheduleAtFixedRate(command, nextScheduleTime - currentTime, millisInterval, TimeUnit.MILLISECONDS);
    }
}
