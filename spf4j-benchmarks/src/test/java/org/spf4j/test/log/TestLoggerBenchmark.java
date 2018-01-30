/*
 * Copyright 2018 SPF4J.
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
package org.spf4j.test.log;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

/**
 * @author Zoltan Farkas
 */
@State(Scope.Benchmark)
@Fork(2)
@Threads(value = 8)
public class TestLoggerBenchmark {

  private static final LogConfigImpl CFG = new LogConfigImpl(
          ImmutableList.of(new LogPrinter(Level.INFO), new DefaultAsserter(),
                  new LogCollector(Level.DEBUG, 100, false) {
            @Override
            public void close() {
            }
          }), Collections.EMPTY_MAP);

  private static final LogConfig CACHED_CFG = new CachedLogConfig(CFG);

  @Benchmark
  public final int testSimple() {
    return CFG.getLogHandlers("org.spf4j", Level.DEBUG).size();
  }

  @Benchmark
  public final int testCachedSimple() {
    return CACHED_CFG.getLogHandlers("org.spf4j", Level.DEBUG).size();
  }

}
