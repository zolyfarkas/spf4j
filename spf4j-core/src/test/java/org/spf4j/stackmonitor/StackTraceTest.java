package org.spf4j.stackmonitor;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class StackTraceTest {

  @Test
  public void testSomeMethod() {
    StackTraceElement[] stack = Thread.currentThread().getStackTrace();
    StackTrace st = new StackTrace(stack, 1);
    System.out.println(st);
    Assert.assertFalse(st.toString().contains("getStackTrace"));
  }
}
