
package org.spf4j.io.compress;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class CompressTest {

  @Test
  public void testZip() throws IOException {
    File file = File.createTempFile(".bla", ".tmp");
    String testStr = "skjfghskjdhgfjgishfgksjhgjkhdskghsfdkjhg";
    Files.write(testStr, file, Charsets.UTF_8);
    Path zip = Compress.zip(file.toPath());
    Assert.assertThat(zip.getFileName().toString(), Matchers.endsWith(".zip"));
    Assert.assertTrue(java.nio.file.Files.exists(zip));
    File tmpFir = Files.createTempDir();
    List<Path> unzip = Compress.unzip(zip, tmpFir.toPath());
    Assert.assertEquals(1, unzip.size());
    Assert.assertEquals(testStr, Files.toString(unzip.get(0).toFile(), Charsets.UTF_8));
  }

}
