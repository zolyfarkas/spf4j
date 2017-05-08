
package org.spf4j.base;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import static org.spf4j.base.Runtime.Ulimit.runUlimit;

/**
 *
 * @author zoly
 */
public class UnixResourcesTest {

  @Test
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public void testUlimit() throws IOException {
    Assume.assumeTrue(Runtime.isMacOsx());
    long softLimit = UnixResources.RLIMIT_NOFILE.getSoftLimit();
    Assert.assertEquals(softLimit,
            (long) runUlimit("-Sn"));
    UnixResources.RLIMIT_NOFILE.setSoftLimit(softLimit - 1);
    Assert.assertEquals(softLimit - 1, UnixResources.RLIMIT_NOFILE.getSoftLimit());
  }


}
