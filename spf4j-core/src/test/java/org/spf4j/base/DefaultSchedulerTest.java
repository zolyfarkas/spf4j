package org.spf4j.base;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.spf4j.concurrent.DefaultScheduler;
import java.util.concurrent.ScheduledFuture;
import org.junit.Assert;
import org.junit.Test;

/**
 *
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
                if (!(time % 1000 < 50)) {
                    notAligned = true;
                }
                System.out.println(time);
                if (first) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
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
