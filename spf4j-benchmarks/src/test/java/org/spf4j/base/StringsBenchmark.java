
package org.spf4j.base;

import java.io.UnsupportedEncodingException;
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
@Threads(value = 4)
public class StringsBenchmark {

    public static final String TEST_STRING;

    static {
        IntMath.XorShift32 rnd = new IntMath.XorShift32();
        StringBuilder builder = new StringBuilder(2200);
        for (int i = 0; i < 2200; i++) {
            builder.append('A' + Math.abs(rnd.nextInt()) % 22);
        }
        builder.append("cw==");
        TEST_STRING = builder.toString();
    }

    @Benchmark
    public final byte [] optimizedSubStringDecode() throws UnsupportedEncodingException {
        return Base64.decodeBase64(CharSequences.subSequence(TEST_STRING, 10, TEST_STRING.length()));
    }

    @Benchmark
    public final byte [] subStringDecode() {
        return Base64.decodeBase64(TEST_STRING.subSequence(10, TEST_STRING.length()));
    }



}
