
package org.spf4j.text;

import org.spf4j.text.MessageFormat;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class FastMessageFormatTest {
  


  @Test
  public void testFormatter() throws IOException {
    StringBuilder sb = new StringBuilder();
    MessageFormat format = new MessageFormat("{0}, {1}");
    format.format(new Object [] {"a", "b"}, sb, null);
    Assert.assertEquals("a, b", sb.toString());
  }
  
  @Test
  public void testFormatter2() throws IOException {
    StringBuilder sb = new StringBuilder();
    MessageFormat format = new MessageFormat("bla bla bla");
    format.format(null, sb, null);
    Assert.assertEquals("bla bla bla", sb.toString());
  }  
  
  @Test
  public void testFormatter3() throws IOException {
    StringBuilder sb = new StringBuilder();
    MessageFormat format = new MessageFormat("pre {1}, {0} suf");
    format.format(new Object [] {"a", "b"}, sb, null);
    Assert.assertEquals("pre b, a suf", sb.toString());
  }  
  
  
}
