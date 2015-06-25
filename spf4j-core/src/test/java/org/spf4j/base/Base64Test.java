
package org.spf4j.base;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class Base64Test {

    public static byte [] generateTestArray(final int nrBytes) {
        final IntMath.XorShift32 random = new IntMath.XorShift32();
        byte [] result = new byte[nrBytes];
        for (int i = 0; i < nrBytes; i++) {
            result[i] = ((byte) Math.abs(random.nextInt() % 256));
        }
        return result;
    }


    @Test
    public void testSomeMethod() {
        byte [] testArray = generateTestArray(2048);
        String encodeBase64 = Base64.encodeBase64(testArray);
        byte[] parseBase64 = Base64.decodeBase64(encodeBase64);
        Assert.assertArrayEquals(testArray, parseBase64);
    }

}
