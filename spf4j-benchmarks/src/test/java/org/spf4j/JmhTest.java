/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j;

import java.io.IOException;
import org.junit.Test;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.stackmonitor.JmhFlightRecorderProfiler;
import org.spf4j.stackmonitor.Spf4jJmhProfiler;
import org.spf4j.test.log.TestUtils;

/**
 * @author zoly
 */
public final class JmhTest {

  private static final Logger LOG = LoggerFactory.getLogger(JmhTest.class);

  @Test
  public void runJmh() throws RunnerException, IOException {
    if (TestUtils.isExecutedInCI()
            || "true".equalsIgnoreCase(System.getProperty("is.release", "false"))
            || !System.getProperty("project.version", "").contains("SNAPSHOT")) {
      LOG.info("Benchmarks disabled in travis, not enough resources for this...");
      return;
    }
    final String destinationFolder = System.getProperty("basedir",
            org.spf4j.base.Runtime.USER_DIR) + "/target";
    final String profile = System.getProperty("basedir",
            org.spf4j.base.Runtime.USER_DIR) + "/src/main/jfc/profile.jfc";
    Options opt = new OptionsBuilder()
            //               .include(".*StringsBenchmark")
            //                .include(".*Reflections.*")
            //                .addProfiler(JmhProfiler.class)
            //                .addProfiler(CompilerProfiler.class)
            .addProfiler(JmhFlightRecorderProfiler.class)
            .addProfiler(Spf4jJmhProfiler.class)
            //                .addProfiler(GCProfiler.class)
            //"-XX:+PrintCompilation", "-XX:+UseG1GC", "-XX:MinTLABSize=1m", "-XX:MaxInlineLevel=12"
            // "-XX:+PrintInlining", "-XX:+TraceDeoptimization", "-XX:+DebugDeoptimization", "-XX:+LogEvents"
            //"-XX:+UnlockDiagnosticVMOptions", "-XX:+LogEvents", "-XX:+PrintCodeCache", "-XX:MaxInlineLevel=12",
            //         "-XX:+PrintInlining", "-XX:+PrintCompilation", "-XX:+LogCompilation"
            .jvmArgs("-XX:MaxInlineLevel=12", "-XX:-RestrictContended",
                    "-XX:BiasedLockingStartupDelay=0",
                    "-Xmx256m", "-Xms256m", "-XX:+UnlockCommercialFeatures",
                    "-Djmh.stack.profiles=" + destinationFolder,
//                    "-Dspf4j.timeSource=systemTime",
                    "-Dspf4j.executors.defaultExecutor.daemon=true",
                  "-Djmh.executor=FJP",
                    "-Djmh.fr.options=defaultrecording=true,settings=" + profile)
            .result(destinationFolder + "/" + "benchmarkResults.csv")
            .resultFormat(ResultFormatType.CSV)
            .warmupIterations(5)
            .measurementIterations(10)
            .forks(1)
            .build();
    new Runner(opt).run();
  }

}
