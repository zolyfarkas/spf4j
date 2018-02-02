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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Zoltan Farkas
 */
@State(Scope.Benchmark)
@Fork(2)
@Threads(value = 8)
public class TestLoggerBenchmark {

  @State(Scope.Benchmark)
  public static class Lazy {

    private final Logger log = LoggerFactory.getLogger("test.log");

  }

  @Benchmark
  public final boolean testDefaultImpl(final Lazy lazy) {
    lazy.log.debug("Some message with some arg {}", "arg");
    return lazy.log.isTraceEnabled();
  }

}
