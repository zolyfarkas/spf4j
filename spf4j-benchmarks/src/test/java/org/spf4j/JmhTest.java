
package org.spf4j;

import java.io.IOException;
import org.junit.Test;
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
public final class JmhTest {
    
    @Test
    public void runJmh() throws RunnerException, IOException {
        Options opt = new OptionsBuilder()
                .include(".*")
                .addProfiler(JmhProfiler.class)
                .addProfiler(JmhFlightRecorderProfiler.class)
                .jvmArgs("-XX:+UnlockCommercialFeatures", "-Djmh.stack.profiles="
                        + System.getProperty("jmh.stack.profiles",
                        org.spf4j.base.Runtime.USER_DIR))
                .warmupIterations(10)
                .measurementIterations(10)
                .forks(1)
                .build();
         new Runner(opt).run();
    }
    
}
