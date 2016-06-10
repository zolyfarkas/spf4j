package org.spf4j.base;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class VersionTest {

    @Test
    @SuppressFBWarnings("CLI_CONSTANT_LIST_INDEX")
    public void testVersion() {
        Version version1 = new Version("1.u1.3");
        Version version2 = new Version("1.u10.3");
        Assert.assertTrue(version1.compareTo(version2) < 0);
        System.out.println("version1" + version1);
        Assert.assertEquals(Integer.valueOf(3), version1.getComponents()[3]);
        Version javaVersion = new Version(org.spf4j.base.Runtime.JAVA_VERSION);
        System.out.println("version1" + javaVersion + ", " + javaVersion.getImage());
        Assert.assertTrue(javaVersion.compareTo(new Version ("1.6.0_1")) > 0);
    }

    @Test
    public void testVersion2() {
        Version version1 = new Version("1.1");
        Version version2 = new Version("1.1.2");
        Assert.assertTrue(version1.compareTo(version2) < 0);
    }

    @Test
    public void testVersion3() {
        Version version1 = new Version("1.8.1");
        Version version2 = new Version("1.8.0.25p");
        Assert.assertTrue(version1.compareTo(version2) > 0);
    }


}
