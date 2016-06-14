
package org.spf4j;

import java.io.IOException;
import org.junit.Test;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.spf4j.stackmonitor.JmhFlightRecorderProfiler;

/**
 *
 * @author zoly
 */
//@Ignore
public final class JmhTest {

    @Test
    public void runJmh() throws RunnerException, IOException {
        final String destinationFolder = System.getProperty("jmh.stack.profiles",
                org.spf4j.base.Runtime.USER_DIR);
        Options opt = new OptionsBuilder()
                .include(".*spf4jAppendable.*")
//                .include(".*")
//                .addProfiler(JmhProfiler.class)
                .addProfiler(JmhFlightRecorderProfiler.class)
                //"-XX:+PrintCompilation","-Djmh.executor=FJP"
                .jvmArgs("-XX:+UnlockCommercialFeatures", "-Djmh.stack.profiles=" + destinationFolder,
                        "-Dspf4j.executors.defaultExecutor.daemon=true")
                .result(destinationFolder + "/" + "benchmarkResults.csv")
                .resultFormat(ResultFormatType.CSV)
                //.measurementTime(TimeValue.seconds(5))
//                .warmupBatchSize(100)
//                .measurementBatchSize(100)
//                .mode(Mode.All)
                .warmupIterations(10)
                .measurementIterations(10)
                .forks(1)
                .build();
         new Runner(opt).run();
    }

}
