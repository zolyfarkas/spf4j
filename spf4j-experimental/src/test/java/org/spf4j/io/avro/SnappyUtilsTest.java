
package org.spf4j.io.avro;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class SnappyUtilsTest {


  @Test
  public void testCompressUncompress() throws IOException {
    String testStr = "jhsfgajdhgfjhsgdfjhagkdjfhgasjdhfur89ewfh4898run2dw9b2cyrufscerbhc87w5hbeghrgobrcshbf8woh4o8";
    byte[] testBytes = testStr.getBytes(StandardCharsets.UTF_8);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    SnappyFrameUtils.writeCompressedFrame(bos, testBytes, 0, testBytes.length);
    SnappyFrameUtils.Frame readFrame =
            SnappyFrameUtils.readFrame(new ByteArrayInputStream(bos.toByteArray()), (int size) -> new byte[size]);
    Assert.assertArrayEquals(testBytes, readFrame.getData());
  }

}
