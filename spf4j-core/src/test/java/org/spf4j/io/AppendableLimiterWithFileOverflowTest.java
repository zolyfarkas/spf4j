
package org.spf4j.io;

import com.google.common.io.CharStreams;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class AppendableLimiterWithFileOverflowTest {
  

  @Test
  public void testOverflow() throws IOException {
    File ovflow = File.createTempFile("overflow", ".txt");
    System.out.println();
    StringBuilder destination = new StringBuilder();
    final String testStr = "012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";    
    
    try(AppendableLimiterWithFileOverflow limiter =
            new AppendableLimiterWithFileOverflow(50, ovflow, "...@", destination)) {
      limiter.append(testStr.subSequence(0, 45));
      limiter.append(testStr.charAt(45));
      limiter.append(testStr.subSequence(46, testStr.length()));
    }
    
    System.out.println(destination);
    Assert.assertEquals(50, destination.length());
    System.out.println(destination);
    String oContent = CharStreams.toString(new InputStreamReader(new FileInputStream(ovflow)));
    System.out.println(oContent);
    Assert.assertEquals(testStr, oContent);
  }
  
}
