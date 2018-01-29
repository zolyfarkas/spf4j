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
package org.spf4j.test.log.junit;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.test.log.ExceptionHandoverRegistry;
import org.spf4j.test.log.Level;
import org.spf4j.test.log.LogCollectionHandler;
import org.spf4j.test.log.LogPrinter;
import org.spf4j.test.log.LogRecord;
import org.spf4j.test.log.TestLoggers;
import org.spf4j.test.log.UncaughtExceptionDetail;

/**
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY")
public final class Spf4jTestLogRunListenerSingleton extends RunListener {

  private static final Logger LOG = LoggerFactory.getLogger(Spf4jTestLogRunListenerSingleton.class);

  private static final Spf4jTestLogRunListenerSingleton INSTANCE = new Spf4jTestLogRunListenerSingleton();

  private final Level minLogLevel;

  private final int maxDebugLogsCollected;

  private final Map<Description, LogCollectionHandler> collections;

  private final boolean collectPrinted;

  private final ExceptionAsserterUncaughtExceptionHandler uncaughtExceptionHandler;

  private Spf4jTestLogRunListenerSingleton() {
    minLogLevel = Level.valueOf(System.getProperty("spf4j.test.log.collectMinLevel", "DEBUG"));
    maxDebugLogsCollected = Integer.getInteger("spf4j.test.log.collectmaxLogs", 100);
    collectPrinted = Boolean.getBoolean("spf4j.test.log.collectPrintedLogs");
    collections = new ConcurrentHashMap<>();
    synchronized (Thread.class) {
      final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
      uncaughtExceptionHandler = new ExceptionAsserterUncaughtExceptionHandler(defaultHandler);
      Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
    }
  }

  public static Spf4jTestLogRunListenerSingleton getInstance() {
    return INSTANCE;
  }



  @Override
  public void testRunFinished(final Result result) {
    List<UncaughtExceptionDetail> exceptions = uncaughtExceptionHandler.getUncaughtExceptions();
    if (!exceptions.isEmpty()) {
      AssertionError assertionError = new AssertionError("Uncaught exceptions encountered " + exceptions);
      for (UncaughtExceptionDetail ex : exceptions) {
        Throwable throwable = ex.getThrowable();
        assertionError.addSuppressed(throwable);
        LOG.info("Uncaught exceptions, failures = {} in thread {}", result.getFailures(),
                ex.getThread(), throwable);
      }
      throw assertionError;
    }
  }

  @Override
  public void testFailure(final Failure failure) {
    Description description = failure.getDescription();
    LogCollectionHandler handler = collections.get(description);
    if (handler != null) { // will Happen when a Uncaught Exception causes a test to fail.
      dumpDebugInfo(handler, description, failure.getException());
    }
  }

  public void dumpDebugInfo(final LogCollectionHandler handler, final Description description,
          final Throwable failure) {
    handler.close();
    final String annotation = description.toString() + ' ';
    int nr = handler.forEach(new Consumer<LogRecord>() {

      private boolean first = true;

      @Override
      public void accept(final LogRecord record) {
        if (first) {
          LOG.info("Test {} failed", description, failure);
          LOG.info("Dumping last {} unprinted logs for {}", maxDebugLogsCollected, description);
          first = false;
        }
        LogPrinter.printToStderr(record, annotation);
      }
    });
    if (nr > 0) {
      LOG.info("End dump for {}", description);
    }
  }

  @Override
  public synchronized void testFinished(final Description description) {
    LogCollectionHandler handler = collections.remove(description);
    handler.close();
    handleUncaughtExceptions(description, handler);
  }

  public void handleUncaughtExceptions(final Description description, final LogCollectionHandler handler) {
    List<UncaughtExceptionDetail> exceptions = uncaughtExceptionHandler.getUncaughtExceptions();
    if (!exceptions.isEmpty()) {
      AssertionError assertionError = new AssertionError("Uncaught exceptions encountered " + exceptions);
      for (UncaughtExceptionDetail ex : exceptions) {
        Throwable throwable = ex.getThrowable();
        LOG.info("Uncaught exceptions during {} in thread {}", description, ex.getThread(), ex.getThrowable());
        assertionError.addSuppressed(throwable);
      }
      dumpDebugInfo(handler, description, null);
      throw assertionError;
    }
  }

  @Override
  public void testStarted(final Description description) throws Exception {
    collections.put(description, TestLoggers.config().collect(minLogLevel, maxDebugLogsCollected, collectPrinted));
    super.testStarted(description);
  }

  public ExceptionHandoverRegistry getUncaughtExceptionHandler() {
    return uncaughtExceptionHandler;
  }

  @Override
  public String toString() {
    return "DetailOnFailureRunListener{" + "minLogLevel=" + minLogLevel
            + ", maxDebugLogsCollected=" + maxDebugLogsCollected + ", collections="
            + collections + ", collectPrinted=" + collectPrinted + '}';
  }

}
