
package org.spf4j.base;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class ArraysTest {


  @Test
  public void testFill() {
    String [] testArray = new String[64];
    java.util.Arrays.fill(testArray, "a");
    String [] a1 = testArray.clone();
    String [] a2 = testArray.clone();
    java.util.Arrays.fill(a1, 50, 64, null);
    Arrays.fill(a2, 50, 64, null);
    Assert.assertArrayEquals(a1, a2);
  }

}
