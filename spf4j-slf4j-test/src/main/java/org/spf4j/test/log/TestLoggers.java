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
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import javax.annotation.CheckReturnValue;
import javax.annotation.concurrent.GuardedBy;
import org.hamcrest.Matcher;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * @author Zoltan Farkas
 */
public final class TestLoggers implements ILoggerFactory {

  private static final TestLoggers INSTANCE = new TestLoggers();

  public static TestLoggers config() {
    return INSTANCE;
  }

  private final ConcurrentMap<String, Logger> loggerMap;

  private final Object sync;

  @GuardedBy("sync")
  private volatile LogConfigImpl config;

  private final Function<String, Logger> computer;

  private TestLoggers() {
    loggerMap = new ConcurrentHashMap<String, Logger>();
    Level rootPrintLevel = TestUtils.isExecutedFromIDE()
            ? Level.valueOf(System.getProperty("spf4j.testLog.rootPrintLevelIDE", "DEBUG"))
            : Level.valueOf(System.getProperty("spf4j.testLog.rootPrintLevel", "INFO"));
    config = new LogConfigImpl(Arrays.asList(new LogPrinter(rootPrintLevel), new DefaultAsserter()),
            Collections.EMPTY_MAP);
    computer = (k) -> new TestLogger(k, TestLoggers.this::getConfig);
    sync = new Object();
  }

  public LogConfig getConfig() {
    return config;
  }

  /**
   * Print logs above a category and log level.
   * @param category the log category.
   * @param level the log level.
   * @return a handler that allows this printing to stop (when calling close).
   */
  @CheckReturnValue
  public HandlerRegistration print(final String category, final Level level) {
    LogPrinter logPrinter = new LogPrinter(level);
    synchronized (sync) {
      config = config.add(category, logPrinter);
    }
    return () -> {
      synchronized (sync) {
        config = config.remove(category, logPrinter);
      }
    };
  }

  /**
   * Create an log expectation that can be asserted like:
   *
   * LogAssert expect = TestLoggers.expect("org.spf4j.test", Level.ERROR, Matchers.hasProperty("format",
   * Matchers.equalTo("Booo"))); LOG.error("Booo", new RuntimeException()); expect.assertSeen();
   *
   *
   * @param category the category under which we should expect theese messages.
   * @param minimumLogLevel minimum log level of expected log messages
   * @param matchers a succession of LogMessages with each matching a Matcher is expected.
   * @return
   */
  @CheckReturnValue
  public LogAssert expect(final String category, final Level minimumLogLevel,
          final Matcher<LogRecord>... matchers) {
    LogMatchingHandler handler = new LogMatchingHandler(minimumLogLevel, matchers) {

      private boolean isClosed = false;

      @Override
      void unregister() {
        synchronized (sync) {
          if (!isClosed) {
            config = config.remove(category, this);
            isClosed = true;
          }
        }
      }


    };
    synchronized (sync) {
      config = config.add(category, handler);
    }
    return handler;
  }

  /**
   * Ability to assert is you expect a sequence of logs to be repeated.
   * @param category
   * @param minimumLogLevel
   * @param nrTimes
   * @param matchers
   * @return
   */
  public LogAssert expect(final String category, final Level minimumLogLevel,
          final int nrTimes, final Matcher<LogRecord>... matchers) {
    Matcher<LogRecord>[] newMatchers = new Matcher[matchers.length * nrTimes];
    for (int i = 0, j = 0; i < nrTimes; i++) {
      for (Matcher<LogRecord> m : matchers) {
        newMatchers[j++] = m;
      }
    }
    return expect(category, minimumLogLevel, newMatchers);
  }

  /**
   * register a log collector that collects up to maxNrLogs unprinted logs with a minimumLogLevel log level
   * @param minimumLogLevel
   * @param maxNrLogs
   * @return a handler that allows you to access the collected logs and stop this collection.
   */
  public LogCollectionHandler collect(final Level minimumLogLevel, final int maxNrLogs, final boolean collectPrinted) {
    DebugLogCollector handler = new DebugLogCollector(minimumLogLevel, maxNrLogs, collectPrinted) {

      private boolean isClosed = false;

      @Override
      public void close() {
        synchronized (sync) {
          if (!isClosed) {
            config = config.remove("", this);
            isClosed = true;
          }
        }
      }
    };
    synchronized (sync) {
      config = config.add("", handler);
    }
    return handler;
  }

  /**
   * Return an appropriate {@link SimpleLogger} instance by name.
   */
  public Logger getLogger(final String name) {
    return loggerMap.computeIfAbsent(name, computer);
  }

  @Override
  public String toString() {
    return "TestLoggers{ config=" + config + ", loggerMap=" + loggerMap + '}';
  }

}
