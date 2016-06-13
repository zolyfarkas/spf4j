
package org.spf4j.base;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class CharSequencesTest {


  @Test
  public void testLineNumbering() {
    CharSequence lineNumbered = CharSequences.toLineNumbered(0, "a\nbla\nc");
    Assert.assertEquals("/* 0 */ a\n/* 1 */ bla\n/* 2 */ c", lineNumbered.toString());
  }

}
