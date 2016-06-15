
package org.spf4j;

import java.io.IOException;
import org.junit.Test;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.spf4j.stackmonitor.JmhFlightRecorderProfiler;
import org.spf4j.stackmonitor.JmhProfiler;

/**
 *
 * @author zoly
 */
//@Ignore
public final class JmhTest {

    @Test
    public void runJmh() throws RunnerException, IOException {
        final String destinationFolder = System.getProperty("basedir",
                org.spf4j.base.Runtime.USER_DIR) + "/target";
        final String profile = System.getProperty("basedir",
                org.spf4j.base.Runtime.USER_DIR) + "/src/main/jfc/profile.jfc";
        Options opt = new OptionsBuilder()
                .include(".*Appendable.*")
//                .include(".*")
                .addProfiler(JmhProfiler.class)
//                .addProfiler(CompilerProfiler.class)
                .addProfiler(JmhFlightRecorderProfiler.class)
//                .addProfiler(GCProfiler.class)
                //"-XX:+PrintCompilation", "-XX:+UseG1GC",
                .jvmArgs("-XX:+UseG1GC", "-Xmx256m", "-Xms256m", "-XX:+UnlockCommercialFeatures",
                        "-Djmh.stack.profiles=" + destinationFolder,
                        "-Dspf4j.executors.defaultExecutor.daemon=true", "-Djmh.executor=FJP",
                        "-Djmh.fr.options=defaultrecording=true,settings=" + profile)
                .result(destinationFolder + "/" + "benchmarkResults.csv")
                .resultFormat(ResultFormatType.CSV)
                .warmupIterations(10)
                .measurementIterations(10)
                .forks(1)
                .build();
         new Runner(opt).run();
    }

}
