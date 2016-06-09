package org.spf4j.net;

import java.io.IOException;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author zoly
 */
@Ignore
public final class SntpClientTest {

  @Test
  public void test() throws IOException, InterruptedException {
    Timing requestTime = SntpClient.requestTimeHA(60000, "us.pool.ntp.org");
    long currentTimeMachine = System.currentTimeMillis();
    long currentTimeNtp = requestTime.getTime();
    System.out.println("Current time machine = " + currentTimeMachine + " " + new DateTime(currentTimeMachine));
    System.out.println("Current time ntp = " + currentTimeNtp + " " + new DateTime(currentTimeNtp));
    Assert.assertTrue(Math.abs(currentTimeNtp - currentTimeMachine) < 1000);
  }

}
