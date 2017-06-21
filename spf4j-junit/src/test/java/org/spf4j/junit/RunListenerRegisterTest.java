package org.spf4j.junit;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.notification.RunListener;


/**
 * @author zoly
 */
public class RunListenerRegisterTest {


  @Test
  @SuppressFBWarnings("SEC_SIDE_EFFECT_CONSTRUCTOR") // this is how RunListenerRegister works..
  public void testRunListenerRegister() {
    new RunListenerRegister();
    RunListener runListener = new RunListener();
    RunListenerRegister.addRunListener(runListener, true);
    Assert.assertTrue(RunListenerRegister.removeRunListener(runListener));
    Assert.assertFalse(RunListenerRegister.removeRunListener(runListener));
    try {
      new RunListenerRegister();
      Assert.fail();
    } catch (ExceptionInInitializerError ex) {
      // expected
    }
  }



}
