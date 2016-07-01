
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
@Fork(2)
@Threads(value = 4)
public class EncodingBenchmark {


    @State(Scope.Benchmark)
    public static class TestData {

    private final String testString =
            "dfjgdjshfgsjdhgfskhdfkdshf\ndfs@#$%^&*($%^&*()(*&^%$#@!>::><>?{PLKJHGFDEWSDFGh"
            + "dfhgsjhdgfjadghfkjasdklghkjdshgkadhskgjhslfdhgkjadhgjkhalkjdshgfkjadshkfgjalhdskjg"
            + "kdfjhgakjdshglsdhfgkahdlghjadfjklghkjlsdfhgksdlfhlgkhjsdkfljhgkdjsfhsgkjsdfhklgj";

    private final byte [] testBytes;
      {
        testBytes = testString.getBytes(StandardCharsets.UTF_8);
      }
    }

    @Benchmark
    public final byte [] stringEncode(final TestData testData) throws UnsupportedEncodingException {
        return testData.testString.getBytes("UTF-8");
    }

    @Benchmark
    public final byte [] fastStringEncode(final TestData testData) {
        return Strings.toUtf8(testData.testString);
    }

    @Benchmark
    public final String stringDecode(final TestData testData) throws UnsupportedEncodingException {
       return new String(testData.testBytes, "UTF8");
    }

    @Benchmark
    public final String fastStringDecode(final TestData testData) {
        return Strings.fromUtf8(testData.testBytes);
    }



}
