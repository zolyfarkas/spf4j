/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.base;

import java.util.concurrent.ScheduledFuture;
import org.bouncycastle.crypto.RuntimeCryptoException;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class DefaultSchedulerTest {

    
    private volatile RuntimeException ex;
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
                if (!(time % 1000 < 10)) {
                    ex = new RuntimeCryptoException("Schedule needs to be alligned");
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
        if (ex != null) {
            throw ex;
        }
    }
}
