/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.base;

import java.util.concurrent.ScheduledFuture;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class DefaultSchedulerTest {

    public DefaultSchedulerTest() {
    }

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
                System.out.println(System.currentTimeMillis());
                if (first) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                    first = false;
                }
            }
        };
        long millisInterval = 1000L;
        ScheduledFuture expResult = null;
        ScheduledFuture result = DefaultScheduler.scheduleAllignedAtFixedRateMillis(command, millisInterval);
        //assertEquals(expResult, result);
        Thread.sleep(10000);
    }
}
