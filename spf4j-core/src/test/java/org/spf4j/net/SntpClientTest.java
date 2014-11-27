
package org.spf4j.net;

import java.io.IOException;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class SntpClientTest {
    

    @Test
    public void test() throws IOException, InterruptedException {
        try {
            Timing requestTime = SntpClient.requestTimeHA("us.pool.ntp.org", 60000);
            long currentTimeMachine = System.currentTimeMillis();
            long currentTimeNtp = requestTime.getTime();
            System.out.println("Current time machine = " + currentTimeMachine + " " + new DateTime(currentTimeMachine));
            System.out.println("Current time ntp = " + currentTimeNtp + " " + new DateTime(currentTimeNtp));
            Assert.assertTrue(Math.abs(currentTimeNtp - currentTimeMachine) < 1000);
        } catch (IOException ex) {
            ex.printStackTrace();
            throw ex;
        }
    }
    
}
