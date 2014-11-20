
package org.spf4j.base;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
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
@Threads(value = 1)
public class EncodingBenchmark {
    
    private static final String TEST_STRING =
            "dfjgdjshfgsjdhgfskhdfkdshf\ndfs@#$%^&*($%^&*()(*&^%$#@!>::><>?{PLKJHGFDEWSDFG";

    private static final byte [] TEST_BYTES;
    static {
        TEST_BYTES = TEST_STRING.getBytes(StandardCharsets.UTF_8);
    }
    
    @Benchmark
    public final byte [] stringEncode() throws UnsupportedEncodingException {
        return TEST_STRING.getBytes(StandardCharsets.UTF_8);
    }
    
    @Benchmark
    public final byte [] fastStringEncode() {
        return Strings.toUtf8(TEST_STRING);
    }
    
    @Benchmark
    public final String stringDecode() throws UnsupportedEncodingException {
       return new String(TEST_BYTES, StandardCharsets.UTF_8);
    }
    
    @Benchmark
    public final String fastStringDecode() {
        return Strings.fromUtf8(TEST_BYTES);
    }
    
    
    
}
