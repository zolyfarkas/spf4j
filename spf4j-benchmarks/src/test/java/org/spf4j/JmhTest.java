package org.spf4j;

import java.io.IOException;
import org.junit.Test;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.spf4j.stackmonitor.JmhFlightRecorderProfiler;
import org.spf4j.stackmonitor.Spf4jJmhProfiler;

/**
 *
 * @author zoly
 */
public final class JmhTest {

  @Test
  public void runJmh() throws RunnerException, IOException {
    if ("true".equalsIgnoreCase(System.getenv("TRAVIS"))) {
      System.err.println("Benchmarks disabled in travis, not enough resources for this...");
      return;
    }
    final String destinationFolder = System.getProperty("basedir",
            org.spf4j.base.Runtime.USER_DIR) + "/target";
    final String profile = System.getProperty("basedir",
            org.spf4j.base.Runtime.USER_DIR) + "/src/main/jfc/profile.jfc";
    Options opt = new OptionsBuilder()
            //.include(".*Either.*")
                            .include(".*")
            //                .addProfiler(JmhProfiler.class)
            //                .addProfiler(CompilerProfiler.class)
            .addProfiler(JmhFlightRecorderProfiler.class)
            .addProfiler(Spf4jJmhProfiler.class)
            //                .addProfiler(GCProfiler.class)
            //"-XX:+PrintCompilation", "-XX:+UseG1GC", "-XX:MinTLABSize=1m", "-XX:MaxInlineLevel=12"
            // "-XX:+PrintInlining", "-XX:+TraceDeoptimization", "-XX:+DebugDeoptimization", "-XX:+LogEvents"
            //"-XX:+UnlockDiagnosticVMOptions", "-XX:+LogEvents", "-XX:+PrintCodeCache", "-XX:MaxInlineLevel=12",
            //         "-XX:+PrintInlining", "-XX:+PrintCompilation", "-XX:+LogCompilation"
            .jvmArgs("-XX:MaxInlineLevel=12", "-Xmx256m", "-Xms256m", "-XX:+UnlockCommercialFeatures",
                    "-Djmh.stack.profiles=" + destinationFolder,
                    "-Dspf4j.executors.defaultExecutor.daemon=true",
                 // "-Djmh.executor=FJP",
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
