
package org.spf4j.base;

import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import static org.spf4j.base.Runtime.Ulimit.runUlimit;

/**
 *
 * @author zoly
 */
public class UnixResourcesTest {

  @Test
  public void testUlimit() throws IOException {
    long softLimit = UnixResources.RLIMIT_NOFILE.getSoftLimit();
    Assert.assertEquals(softLimit,
            (long) runUlimit("-Sn"));

//    UnixResources.RLIMIT_NOFILE.setSoftLimit(softLimit + 1);
//    Assert.assertEquals(softLimit + 1, UnixResources.RLIMIT_NOFILE.getSoftLimit());
  }


}
