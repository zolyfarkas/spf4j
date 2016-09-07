/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.base;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class ArrayBuilderTest {

  @Test
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public void testBuilder() {
    ArrayBuilder<String> builder = new ArrayBuilder(10, String.class);
    Assert.assertEquals(0, builder.getSize());
    builder.add("1");
    Assert.assertEquals(1, builder.getSize());
    Assert.assertEquals("1", builder.getArray()[0]);
    for (int i = 0; i < 100; i++) {
      builder.add("b" + i);
    }
    Assert.assertEquals(101, builder.getSize());
    Assert.assertEquals("b99", builder.getArray()[100]);
  }

}
