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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.test.log.Level;
import org.spf4j.test.log.LogCollectionHandler;
import org.spf4j.test.log.LogPrinter;
import org.spf4j.test.log.TestLoggers;

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

  public DetailOnFailureRunListener() {
    minLogLevel = Level.valueOf(System.getProperty("spf4j.test.log.collectMinLevel", "DEBUG"));
    maxDebugLogsCollected = Integer.getInteger("spf4j.test.log.collectmaxLogs", 100);
    collectPrinted = Boolean.getBoolean("spf4j.test.log.collectPrintedLogs");
    collections = new ConcurrentHashMap<>();
  }


  @Override
  public void testFailure(final Failure failure) throws Exception {
    LogCollectionHandler handler = collections.get(failure.getDescription());
    handler.close();
    Description description = failure.getDescription();
    LOG.info("Test {} failed", description, failure.getException());
    LOG.info("Dumping last {} unprinted logs for {description}", maxDebugLogsCollected);
    final String annotation = description.toString() + ' ';
    handler.forEach((record) -> {
      LogPrinter.printToStderr(record, annotation);
    });
  }

  @Override
  public void testFinished(final Description description) throws Exception {
    LogCollectionHandler handler = collections.remove(description);
    handler.close();
  }

  @Override
  public void testStarted(final Description description) throws Exception {
    collections.put(description, TestLoggers.config().collect(minLogLevel, maxDebugLogsCollected, collectPrinted));
    super.testStarted(description);
  }

}
