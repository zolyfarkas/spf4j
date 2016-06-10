
package org.spf4j.io;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
    final String testStr =
        "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";

    try(AppendableLimiterWithOverflow limiter =
            new AppendableLimiterWithOverflow(90, ovflow, "...@", Charsets.UTF_8, destination)) {
      limiter.append(testStr.subSequence(0, 45));
      limiter.append(testStr.charAt(45));
      limiter.append(testStr.subSequence(46, testStr.length()));
    }

    System.out.println(destination);
    Assert.assertEquals(90, destination.length());
    System.out.println(destination);
    String oContent = CharStreams.toString(new InputStreamReader(new FileInputStream(ovflow), StandardCharsets.UTF_8));
    System.out.println(oContent);
    Assert.assertEquals(testStr, oContent);
  }

  @Test
  public void testOverflowX() throws IOException {
    File ovflow = File.createTempFile("overflow", ".txt");
    System.out.println();
    StringBuilder destination = new StringBuilder();
    final String testStr =
        "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
    int nr = ovflow.getPath().length() + 4;
    try(AppendableLimiterWithOverflow limiter =
            new AppendableLimiterWithOverflow(90, ovflow, "...@", Charsets.UTF_8, destination)) {
      limiter.append(testStr.subSequence(0, nr));
      limiter.append(testStr.charAt(nr));
      limiter.append(testStr.subSequence(nr + 1, testStr.length()));
    }

    System.out.println(destination);
    Assert.assertEquals(90, destination.length());
    System.out.println(destination);
    String oContent = CharStreams.toString(new InputStreamReader(new FileInputStream(ovflow), StandardCharsets.UTF_8));
    System.out.println(oContent);
    Assert.assertEquals(testStr, oContent);
  }


  @Test
  public void testOverflow2() throws IOException {
    File ovflow = File.createTempFile("overflow", ".txt");
    System.out.println();
    StringBuilder destination = new StringBuilder();
    final String testStr =
    "012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
    try(AppendableLimiterWithOverflow limiter =
            new AppendableLimiterWithOverflow(90, ovflow, "...@", Charsets.UTF_8, destination)) {
      limiter.append(testStr.subSequence(0, 45));
      limiter.append(testStr.charAt(45));
      limiter.append(testStr.subSequence(46, testStr.length()));
    }

    System.out.println(destination);
    Assert.assertEquals(90, destination.length());
    Assert.assertEquals(testStr, destination.toString());
    System.out.println(destination);
    String oContent = CharStreams.toString(new InputStreamReader(new FileInputStream(ovflow), StandardCharsets.UTF_8));
    System.out.println(oContent);
    Assert.assertEquals("", oContent);
  }


}
