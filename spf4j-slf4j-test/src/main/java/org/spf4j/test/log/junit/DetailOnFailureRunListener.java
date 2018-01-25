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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.test.log.Level;
import org.spf4j.test.log.LogCollectionHandler;
import org.spf4j.test.log.LogPrinter;
import org.spf4j.test.log.TestLoggers;
import org.spf4j.test.log.UncaughtExceptionDetail;

/**
 *
 * @author Zoltan Farkas
 */
public final class DetailOnFailureRunListener extends RunListener {

  private static final Logger LOG = LoggerFactory.getLogger(DetailOnFailureRunListener.class);

  private final Level minLogLevel;

  private final int maxDebugLogsCollected;

  private final Map<Description, LogCollectionHandler> collections;

  private final boolean collectPrinted;

  private final List<UncaughtExceptionDetail> uncaughtExceptions;

  public DetailOnFailureRunListener() {
    minLogLevel = Level.valueOf(System.getProperty("spf4j.test.log.collectMinLevel", "DEBUG"));
    maxDebugLogsCollected = Integer.getInteger("spf4j.test.log.collectmaxLogs", 100);
    collectPrinted = Boolean.getBoolean("spf4j.test.log.collectPrintedLogs");
    collections = new ConcurrentHashMap<>();
    uncaughtExceptions = new CopyOnWriteArrayList<>();
  }

  @Override
  public void testRunFinished(final Result result) {
    List<UncaughtExceptionDetail> exceptions = new ArrayList<>(uncaughtExceptions);
    if (!exceptions.isEmpty()) {
      for (UncaughtExceptionDetail ex : uncaughtExceptions) {
        LOG.error("Uncaught exceptions during {} in thread {}", result, ex.getThread(), ex.getThrowable());
      }
      throw new AssertionError("Uncaught exceptions encountered " + exceptions);
    }
  }

  @Override
  public void testRunStarted(final Description description)  {
    synchronized (Thread.class) {
      final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
      Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(final Thread t, final Throwable e) {
          if (defaultHandler != null) {
            defaultHandler.uncaughtException(t, e);
          }
          uncaughtExceptions.add(new UncaughtExceptionDetail(t, e));
        }
      });
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
    LOG.info("Test {} failed", description, failure);
    LOG.info("Dumping last {} unprinted logs for {}", maxDebugLogsCollected, description);
    final String annotation = description.toString() + ' ';
    handler.forEach((record) -> {
      LogPrinter.printToStderr(record, annotation);
    });
    LOG.info("End dump for {}", description);
  }

  @Override
  public synchronized void testFinished(final Description description) {
    LogCollectionHandler handler = collections.remove(description);
    handler.close();
    List<UncaughtExceptionDetail> exceptions = new ArrayList<>(uncaughtExceptions);
    if (!exceptions.isEmpty()) {
      for (UncaughtExceptionDetail ex : uncaughtExceptions) {
        LOG.info("Uncaught exceptions during {} in thread {}", description, ex.getThread(), ex.getThrowable());
      }
      dumpDebugInfo(handler, description, null);
      throw new AssertionError("Uncaught exceptions encountered " + exceptions);
    }
  }

  @Override
  public void testStarted(final Description description) throws Exception {
    collections.put(description, TestLoggers.config().collect(minLogLevel, maxDebugLogsCollected, collectPrinted));
    super.testStarted(description);
  }

  @Override
  public String toString() {
    return "DetailOnFailureRunListener{" + "minLogLevel=" + minLogLevel
            + ", maxDebugLogsCollected=" + maxDebugLogsCollected + ", collections="
            + collections + ", collectPrinted=" + collectPrinted + '}';
  }

}
