
package org.spf4j.base;

import javax.xml.bind.DatatypeConverter;
import org.junit.Assert;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

/**
 *
 * @author zoly
 */
@State(Scope.Benchmark)
@Fork(2)
@Threads(value = 8)
public class Base64Benchmark {


    public static byte [] generateTestArray(final int nrBytes) {
        final IntMath.XorShift32 random = new IntMath.XorShift32();
        byte [] result = new byte[nrBytes];
        StringBuilder sb = new StringBuilder(nrBytes);
        for (int i = 0; i < nrBytes; i++) {
            result[i] = ((byte) Math.abs(random.nextInt() % 256));
        }
        return result;
    }

    private static final byte [] TEST_ARRAY = generateTestArray(2048);

    @Benchmark
    public void testSpf4jBase64() {
        String encodeBase64 = Base64.encodeBase64(TEST_ARRAY);
        byte[] parseBase64 = Base64.decodeBase64(encodeBase64);
        Assert.assertArrayEquals(TEST_ARRAY, parseBase64);
    }


    @Benchmark
    public void testJdkBase64() {
        String encodeBase64 = DatatypeConverter.printBase64Binary(TEST_ARRAY);
        byte[] parseBase64 = DatatypeConverter.parseBase64Binary(encodeBase64);
        Assert.assertArrayEquals(TEST_ARRAY, parseBase64);
    }


}
