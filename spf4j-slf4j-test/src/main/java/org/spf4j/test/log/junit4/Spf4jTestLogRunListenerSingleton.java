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
package org.spf4j.test.log.junit4;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.WillNotClose;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Closeables;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.TestTimeSource;
import org.spf4j.test.log.ExceptionHandoverRegistry;
import org.spf4j.test.log.Level;
import org.spf4j.test.log.LogCollection;
import org.spf4j.test.log.LogPrinter;
import org.spf4j.test.log.LogRecord;
import org.spf4j.test.log.TestLoggers;
import org.spf4j.test.log.UncaughtExceptionDetail;
import org.spf4j.test.log.annotations.CollectLogs;
import org.spf4j.test.log.annotations.PrintLogs;
import org.spf4j.test.log.annotations.PrintLogsConfigs;

/**
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY")
public final class Spf4jTestLogRunListenerSingleton extends RunListener {

  private static final Logger LOG = LoggerFactory.getLogger(Spf4jTestLogRunListenerSingleton.class);

  private static final Spf4jTestLogRunListenerSingleton INSTANCE = new Spf4jTestLogRunListenerSingleton();

  static {
    System.setProperty("spf4j.timeSource", TestTimeSource.class.getName());
  }

  private final Level minLogLevel;

  private final int maxDebugLogsCollected;

  private final Map<Description, LogCollection<ArrayDeque<LogRecord>>> collections;

  private final Map<Description, ExecutionContext> ctxts;

  private final boolean collectPrinted;

  private final ExceptionAsserterUncaughtExceptionHandler uncaughtExceptionHandler;

  private Spf4jTestLogRunListenerSingleton() {
    minLogLevel = Level.valueOf(System.getProperty("spf4j.test.log.collectMinLevel", "DEBUG"));
    maxDebugLogsCollected = Integer.getInteger("spf4j.test.log.collectmaxLogs", 100);
    collectPrinted = Boolean.getBoolean("spf4j.test.log.collectPrintedLogs");
    collections = new ConcurrentHashMap<>();
    ctxts = new ConcurrentHashMap<>();
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
    LogCollection<ArrayDeque<LogRecord>> handler = collections.get(description);
    if (handler != null) { // will Happen when a Uncaught Exception causes a test to fail.
      try (LogCollection<ArrayDeque<LogRecord>> h = handler) {
        dumpDebugInfo(h.get(), description);
      }
    }
  }

  private void dumpDebugInfo(final Collection<LogRecord> logs,
          final Description description) {
    synchronized (System.out) { // do not interleave other stuff.
      boolean first = true;
      if (!logs.isEmpty()) {
        for (LogRecord record : logs) {
          if (first) {
            LOG.info("Dumping last {} logs collected for debug for {}", maxDebugLogsCollected, description);
            first = false;
          }
          LogPrinter.printTo(System.out, record, "");
        }
        LOG.info("End dump for {}", description);
      }
    }

  }

  @Override
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public void testStarted(final Description description) throws Exception {
    Test ta = description.getAnnotation(Test.class);
    ExecutionContext ctx;
    if (ta != null && ta.timeout() > 0) {
      ctx = ExecutionContexts.start(description.getDisplayName(), ta.timeout(), TimeUnit.MILLISECONDS);
    } else {
      ctx = ExecutionContexts.start(description.getDisplayName());
    }
    CollectLogs ca = description.getAnnotation(CollectLogs.class);
    Level mll = ca == null ? minLogLevel : ca.minLevel();
    boolean clp = ca == null ? collectPrinted : ca.collectPrinted();
    TestLoggers sysTest = TestLoggers.sys();
    collections.put(description, sysTest.collect(mll, maxDebugLogsCollected, clp));
    PrintLogsConfigs prtAnnots = description.getAnnotation(PrintLogsConfigs.class);
    if (prtAnnots != null) {
      PrintLogs[] value = prtAnnots.value();
      for (PrintLogs prtAnnot : value) {
        sysTest.print(prtAnnot.category(), TestLoggers.EXECUTED_FROM_IDE
              ? prtAnnot.ideMinLevel() : prtAnnot.minLevel(), prtAnnot.greedy());
      }
    } else {
      PrintLogs prtAnnot = description.getAnnotation(PrintLogs.class);
      if (prtAnnot != null) {
        sysTest.print(prtAnnot.category(), TestLoggers.EXECUTED_FROM_IDE
              ? prtAnnot.ideMinLevel() : prtAnnot.minLevel(), prtAnnot.greedy());
      }
    }
    ctxts.put(description, ctx);
    super.testStarted(description);
  }


  @Override
  public synchronized void testFinished(final Description description) {
    TestTimeSource.clear();
    LogCollection<ArrayDeque<LogRecord>> handler = collections.remove(description);
    try (LogCollection<ArrayDeque<LogRecord>> h = handler) {
      handleUncaughtExceptions(description, h.get());
    }
    ExecutionContext ctx = ctxts.remove(description);
    ExecutionContext currentThreadContext = ExecutionContexts.current();
    if (ctx == currentThreadContext) {
      ctx.close();
      List<AutoCloseable> closeables = (List<AutoCloseable>) ctx.get(AutoCloseable.class);
      if (closeables != null) {
        Exception ex = Closeables.closeAll(closeables);
        if (ex != null) {
          throw new IllegalStateException("cannot close " + closeables, ex);
        }
      }
    } else {
      throw new IllegalStateException("JUnit Threading model not as expected " + ctx + " != "
              + currentThreadContext);
    }
  }

  private void handleUncaughtExceptions(final Description description,
          @WillNotClose final Collection<LogRecord> logs) {
    List<UncaughtExceptionDetail> exceptions = uncaughtExceptionHandler.getUncaughtExceptions();
    if (!exceptions.isEmpty()) {
      AssertionError assertionError = new AssertionError("Uncaught exceptions encountered " + exceptions);
      for (UncaughtExceptionDetail ex : exceptions) {
        Throwable throwable = ex.getThrowable();
        LOG.info("Uncaught exceptions during {} in thread {}", description, ex.getThread(), ex.getThrowable());
        assertionError.addSuppressed(throwable);
      }
      dumpDebugInfo(logs, description);
      throw assertionError;
    }
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
