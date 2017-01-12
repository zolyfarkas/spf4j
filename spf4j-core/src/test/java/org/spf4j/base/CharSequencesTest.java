
package org.spf4j.base;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("LSC_LITERAL_STRING_COMPARISON")
public class CharSequencesTest {


  @Test
  public void testLineNumbering() {
    CharSequence lineNumbered = CharSequences.toLineNumbered(0, "a\nbla\nc");
    Assert.assertEquals("/* 0 */ a\n/* 1 */ bla\n/* 2 */ c", lineNumbered.toString());
  }


  @Test
  public void testCompare() {
    Assert.assertEquals(0, CharSequences.compare("blabla", 6, "blabla/cucu", 6));
  }

  @Test
  public void testCompare2() {
    Assert.assertEquals(0, CharSequences.compare("cacablabla", 4, 6, "ablabla/cucu", 1, 6));
  }

  @Test
  public void testCompare3() {
    Assert.assertEquals("blabla123".compareTo("blabla"),
            CharSequences.compare("cacablabla123", 4, 9, "ablabla/cucu", 1, 6));
  }

  @Test
  public void testCompare4() {
    Assert.assertEquals("bla".compareTo("lab"),
            CharSequences.compare("cacablabla123", 4, 3, "ablabla/cucu", 2, 3));
  }

  @Test
  public void testCompare5() {
    Assert.assertEquals("bla".compareTo("labl"),
            CharSequences.compare("cacablabla123", 4, 3, "ablabla/cucu", 2, 4));
  }

}
