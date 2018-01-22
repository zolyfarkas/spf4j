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

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * @author Zoltan Farkas
 */
public final  class TestLoggers implements ILoggerFactory {

  private final ConcurrentMap<String, Logger> loggerMap;

  private volatile LogConfig config;

  private final Function<String, Logger> computer;

  public TestLoggers() {
    loggerMap = new ConcurrentHashMap<String, Logger>();
    config = new LogConfigImpl(Arrays.asList(new LogPrinter(Level.INFO), new DefaultAsserter()));
    computer = (k) -> new TestLogger(k, TestLoggers.this::getConfig);
  }

  public LogConfig getConfig() {
    return config;
  }

  /**
   * Return an appropriate {@link SimpleLogger} instance by name.
   */
  public Logger getLogger(final String name) {
    return loggerMap.computeIfAbsent(name, computer);
  }

}
