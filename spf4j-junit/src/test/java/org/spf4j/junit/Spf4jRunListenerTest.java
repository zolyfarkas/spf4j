
package org.spf4j.junit;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.Description;

/**
 * @author zoly
 */
public class Spf4jRunListenerTest {

  @Test
  @SuppressFBWarnings("MDM_THREAD_YIELD")
  public void testProfilerRunListener() throws IOException, InterruptedException {
    Spf4jRunListener listener = new Spf4jRunListener();
    listener.testStarted(Description.EMPTY);
    Thread.sleep(500);
    listener.testFinished(Description.EMPTY);
    Assert.assertNotNull(listener.getLastWrittenFile());
  }

}
