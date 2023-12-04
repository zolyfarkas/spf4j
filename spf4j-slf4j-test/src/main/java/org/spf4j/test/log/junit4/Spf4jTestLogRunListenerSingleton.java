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

import com.google.common.util.concurrent.MoreExecutors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.ThreadSafe;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.TestTimeSource;
import org.spf4j.base.Threads;
import org.spf4j.concurrent.CustomThreadFactory;
import org.spf4j.io.Csv;
import org.spf4j.test.log.Attachments;
import org.spf4j.test.log.ExceptionHandoverRegistry;
import org.spf4j.log.Level;
import org.spf4j.test.log.LogAssert;
import org.spf4j.test.log.LogCollection;
import org.spf4j.test.log.LogPrinter;
import org.spf4j.test.log.TestLogRecordImpl;
import org.spf4j.test.log.TestLogRecord;
import org.spf4j.test.log.TestLoggers;
import org.spf4j.test.log.TestUtils;
import org.spf4j.test.log.UncaughtExceptionDetail;
import org.spf4j.test.log.ValidationUtils;
import org.spf4j.test.log.annotations.CollectLogs;
import org.spf4j.test.log.annotations.ExpectLog;
import org.spf4j.test.log.annotations.ExpectLogs;
import org.spf4j.test.log.annotations.PrintLogs;
import org.spf4j.test.log.annotations.PrintLogsConfigs;
import org.spf4j.test.matchers.LogMatchers;

/**
 *
 */
@SuppressFBWarnings({"FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY", "CE_CLASS_ENVY"})
@ThreadSafe
public final class Spf4jTestLogRunListenerSingleton extends RunListener {

  public static final ExecutionContext.SimpleTag<TestBaggage> BAG_TAG
          = new ExecutionContext.SimpleTag<TestBaggage>() {

    @Override
    public String toString() {
      return "BaggageTag";
    }

  };


  private static final ScheduledExecutorService SCHEDULER  =
          MoreExecutors.getExitingScheduledExecutorService(new ScheduledThreadPoolExecutor(
                  Integer.getInteger("spf4j.executors.defaultScheduler.coreThreads", 2),
                  new CustomThreadFactory("SPF4j-JUNIT",
                          Boolean.getBoolean("spf4j.executors.defaultScheduler.daemon"),
                          Integer.getInteger("spf4j.executors.defaultScheduler.priority", Thread.NORM_PRIORITY))));


  private static volatile Spf4jTestLogRunListenerSingleton instance;

  static {
    System.setProperty("spf4j.timeSource", TestTimeSource.class.getName());
    System.setProperty("spf4j.dumpNonDaemonThreadInfoOnShutdown", "true");
  }

  private static class Lazy {
    private static final Logger LOG = LoggerFactory.getLogger(Lazy.class);
  }

  private final Level minLogLevel;

  private final int maxDebugLogsCollected;

  private final Map<Description, TestBaggage> baggages;

  private final boolean collectPrinted;

  private final ExceptionAsserterUncaughtExceptionHandler uncaughtExceptionHandler;

  private  final long defaultTestTimeoutMillis;

  private final String[] excludeLogsFromCollection;

  public interface TimeoutSupplier {
    long getTimeoutMillis(Description desc, long defaultTestTimeoutMillis);
  }

  private final TimeoutSupplier timeoutSupplier;

  private Spf4jTestLogRunListenerSingleton() {
    this(Spf4jTestLogRunListenerSingleton::getTimeoutMillis);
  }

  public static long getTimeoutMillis(final Description desc, final long defaultTestTimeoutMillis) {
              Test ta = desc.getAnnotation(Test.class);
    if (ta != null && ta.timeout() > 0) {
      return ta.timeout();
    } else {
      return defaultTestTimeoutMillis;
    }
  }

  private Spf4jTestLogRunListenerSingleton(final TimeoutSupplier timeoutSupplier) {
    minLogLevel = Level.valueOf(System.getProperty("spf4j.test.log.collectMinLevel", "DEBUG"));
    maxDebugLogsCollected = Integer.getInteger("spf4j.test.log.collectmaxLogs", 100);
    defaultTestTimeoutMillis =  Long.getLong("spf4j.test.log.defaultTestTimeoutMillis", 120000);
    collectPrinted = Boolean.getBoolean("spf4j.test.log.collectPrintedLogs");
    excludeLogsFromCollection = Csv.readSystemProperty("spf4j.test.log.collectExclusions");
    baggages = new ConcurrentHashMap<>();
    this.timeoutSupplier = timeoutSupplier;
    synchronized (Thread.class) {
      final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
      uncaughtExceptionHandler = new ExceptionAsserterUncaughtExceptionHandler(defaultHandler);
      Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
    }
  }

  public static Spf4jTestLogRunListenerSingleton getOrCreateListenerInstance() {
    return getOrCreateListenerInstance(null);
  }

  public static Spf4jTestLogRunListenerSingleton getOrCreateListenerInstance(
          @Nullable final TimeoutSupplier timeoutSupplier) {
    Spf4jTestLogRunListenerSingleton res = instance;
    if (res == null) {
      synchronized (Spf4jTestLogRunListenerSingleton.class) {
        res = instance;
        if (res == null) {
          if (timeoutSupplier == null) {
            res = new Spf4jTestLogRunListenerSingleton();
          } else {
            res = new Spf4jTestLogRunListenerSingleton(timeoutSupplier);
          }
          instance = res;
        }
      }
    }
    return res;
  }

  @Override
  public void testRunStarted(final Description description) {
    ValidationUtils.validateLogger(Lazy.LOG);
  }

  @SuppressFBWarnings({"WEM_WEAK_EXCEPTION_MESSAGING", "MS_EXPOSE_REP"})
  public static Spf4jTestLogRunListenerSingleton getListenerInstance() {
    Spf4jTestLogRunListenerSingleton res = instance;
    if (res == null) {
      throw new RuntimeException("Spf4jTestLogRunListener not registered, you can register it like:"
              + "      <plugin>\n"
              + "        <groupId>org.apache.maven.plugins</groupId>\n"
              + "        <artifactId>maven-surefire-plugin</artifactId>\n"
              + "        <configuration>\n"
              + "          <properties>\n"
              + "            <property>\n"
              + "              <name>listener</name>\n"
              + "              <value>org.spf4j.test.log.junit4.Spf4jTestLogRunListener</value>\n"
              + "            </property>\n"
              + "          </properties>\n"
              + "        </configuration>\n"
              + "      </plugin>");
    }
    return res;
  }

  @Override
  public synchronized void testRunFinished(final Result result) {
    List<UncaughtExceptionDetail> exceptions = uncaughtExceptionHandler.getUncaughtExceptions();
    if (!exceptions.isEmpty()) {
      AssertionError assertionError = new AssertionError("Uncaught exceptions encountered " + exceptions);
      for (UncaughtExceptionDetail ex : exceptions) {
        Throwable throwable = ex.getThrowable();
        assertionError.addSuppressed(throwable);
        Lazy.LOG.info("Uncaught exceptions, failures = {} in thread {}", result.getFailures(),
                ex.getThread(), throwable);
      }
      throw assertionError;
    }
  }

  private static void dumpDebugInfo(final Collection<TestLogRecord> logs,
          final Description description, final int maxDebugLogsCollected) {
    Iterator<TestLogRecord> iterator = logs.iterator();
    if (iterator.hasNext()) {
      synchronized (System.out) { // do not interleave other stuff.
        LogPrinter.PRINTER.printTo(System.out, new TestLogRecordImpl("test", Level.INFO,
                "Dumping last {} logs collected for debug for {}", maxDebugLogsCollected, description), "");
        TestLogRecord record = iterator.next();
        LogPrinter.PRINTER.printTo(System.out, record, "");
        while (iterator.hasNext()) {
          record = iterator.next();
          LogPrinter.PRINTER.printTo(System.out, record, "");
        }
        LogPrinter.PRINTER.printTo(System.out, new TestLogRecordImpl("test", Level.INFO,
                "End debug log dump for {}", description), "");
      }
    }

  }

  private void registerDeadlockLogger(final Description description,
          final ExecutionContext ctx, final long delay, final TimeUnit tu) {
    final long delayMillis = Math.max(tu.toMillis(delay) - 10,  0);
    ScheduledFuture<?> future = SCHEDULER.schedule(() -> {
      Lazy.LOG.info("Unit test  {} did not finish after {} {}, dumping thread stacks", description, delay, tu);
      Threads.dumpToPrintStream(System.err);
    }, delayMillis, TimeUnit.MILLISECONDS);
    ctx.addCloseable(() -> {
          future.cancel(true);
        });
  }

  @Override
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public synchronized void testStarted(final Description description) throws Exception {
    ExecutionContext ctx;
    if (TestUtils.isExecutedWithDebuggerAgent()) {
      ctx = ExecutionContexts.start(description.getDisplayName());
    } else {
      long timeoutMillis = timeoutSupplier.getTimeoutMillis(description, defaultTestTimeoutMillis);
      ctx = ExecutionContexts.start(description.getDisplayName(), timeoutMillis, TimeUnit.MILLISECONDS);
      registerDeadlockLogger(description, ctx, timeoutMillis, TimeUnit.MILLISECONDS);
    }
    TestLoggers sysTest = TestLoggers.sys();
    LogCollection<ArrayDeque<TestLogRecord>> collectLogs = handleLogCollections(description, sysTest);
    List<LogAssert> logExpectations = setUpUnitTestLogExpectations(description, sysTest);
    TestBaggage testBaggage = new TestBaggage(ctx, collectLogs, logExpectations);
    baggages.put(description, testBaggage);
    ctx.putToRootParent(BAG_TAG, testBaggage);
    handlePrintLogAnnotations(description, sysTest);
    super.testStarted(description);
  }

  private List<LogAssert> setUpUnitTestLogExpectations(final Description description,
          final TestLoggers sysTest) {
    List<LogAssert> assertions = new ArrayList<>(2);
    assertions.add(
            sysTest.dontExpect("", Level.ERROR,
                    Matchers.allOf(LogMatchers.noAttachment(Attachments.ASSERTED),
                            LogMatchers.hasNotLogger(sysTest::isInExpectingErrorCategories),
                            LogMatchers.hasLevel(Level.ERROR))));
    ExpectLogs expectLogs = description.getAnnotation(ExpectLogs.class);
    if (expectLogs != null) {
      ExpectLog[] value = expectLogs.value();
      for (ExpectLog expect : value) {
        assertions.add(
                sysTest.expect(ExpectLog.Util.effectiveCategory(expect), expect.level(), expect.nrTimes(),
                        expect.expectationTimeout(), expect.timeUnit(),
                        Matchers.allOf(LogMatchers.hasMessageWithPattern(expect.messageRegexp()),
                                LogMatchers.hasLevel(expect.level()))));
      }
    } else {
      ExpectLog expect = description.getAnnotation(ExpectLog.class);
      if (expect != null) {
        assertions.add(
                sysTest.expect(ExpectLog.Util.effectiveCategory(expect), expect.level(), expect.nrTimes(),
                        expect.expectationTimeout(), expect.timeUnit(),
                        Matchers.allOf(LogMatchers.hasMessageWithPattern(expect.messageRegexp()),
                                LogMatchers.hasLevel(expect.level()))));
      }
    }
    return assertions;
  }

  private LogCollection<ArrayDeque<TestLogRecord>> handleLogCollections(final Description description,
          final TestLoggers sysTest) {
    CollectLogs ca = description.getAnnotation(CollectLogs.class);
    Level mll;
    boolean clp;
    int nrLogs;
    String include;
    String[] exclude;
    if (ca == null) {
      mll = minLogLevel;
      clp = collectPrinted;
      nrLogs = maxDebugLogsCollected;
      include = "";
      exclude = excludeLogsFromCollection;
    } else {
      mll = ca.minLevel();
      clp = ca.collectPrinted();
      nrLogs = ca.nrLogs();
      include = ca.includeLogs();
      exclude = ca.excludeLogs();
    }
    return sysTest.collect(mll, nrLogs, clp, include, exclude);
  }

  private void handlePrintLogAnnotations(final Description description, final TestLoggers sysTest) {
    PrintLogsConfigs prtAnnots = description.getAnnotation(PrintLogsConfigs.class);
    if (prtAnnots != null) {
      for (PrintLogs prtAnnot : prtAnnots.value()) {
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
  }

  public void assertionsAfterTestExecution(final Description description) {
    TestBaggage baggage = baggages.remove(description);
    assertionsAfterTestExecution(description, baggage);
  }

  @SuppressFBWarnings("AFBR_ABNORMAL_FINALLY_BLOCK_RETURN")
  public  void assertionsAfterTestExecution(final Description description, final TestBaggage baggage) {
    TestTimeSource.clear();
    try (LogCollection<ArrayDeque<TestLogRecord>> h = baggage.getLogCollection()) {
      handleUncaughtExceptions(description, h.get());
    } finally {
      for (LogAssert assertion : baggage.getAssertions()) {
        try {
          assertion.assertObservation();
        } catch (AssertionError ae) {
          throw new AssertionError("Failed test " + description + ", " + ae.getMessage(), ae);
        }
      }
    }
  }

  public static void cleanupAfterTestFinish(final Description description, final TestBaggage baggage) {
    if (baggage == null) {
      throw new IllegalStateException("Test baggage for " + description + "missing."
           + "please consult documentattion for correct usage and/or report this issue.");
    }
    closeAllContextCloseables(baggage);
  }

  @Override
  public synchronized void testFinished(final Description description) {
    TestBaggage baggage = baggages.remove(description);
    try {
      assertionsAfterTestExecution(description, baggage);
    } finally {
      cleanupAfterTestFinish(description, baggage);
    }
  }

  private static void closeAllContextCloseables(final TestBaggage baggage) {
    ExecutionContext ctx = baggage.getCtx();
    ExecutionContext currentThreadContext = ExecutionContexts.current();
    if (ctx == currentThreadContext) {
      ctx.close();
    } else {
      throw new IllegalStateException("JUnit Threading model not as expected " + ctx + " != "
              + currentThreadContext);
    }
  }

  @Override
  public synchronized void testFailure(final Failure failure) {
    Description description = failure.getDescription();
    TestBaggage bg = baggages.get(description);
    dumpDebugInfoOnFailure(bg, description, maxDebugLogsCollected);
  }

  public static void dumpDebugInfoOnFailure(final TestBaggage bg, final Description description,
          final int maxDebugLogsCollected) {
    if (bg != null) { // will Happen when a Uncaught Exception causes a test to fail.
      LogCollection<ArrayDeque<TestLogRecord>> handler = bg.getLogCollection();
      try (LogCollection<ArrayDeque<TestLogRecord>> h = handler) {
        dumpDebugInfo(h.get(), description, maxDebugLogsCollected);
      }
    }
  }

  private void handleUncaughtExceptions(final Description description,
          @WillNotClose final Collection<TestLogRecord> logs) {
    List<UncaughtExceptionDetail> exceptions = uncaughtExceptionHandler.getUncaughtExceptions();
    if (!exceptions.isEmpty()) {
      AssertionError assertionError = new AssertionError("Uncaught exceptions encountered " + exceptions);
      for (UncaughtExceptionDetail ex : exceptions) {
        Throwable throwable = ex.getThrowable();
        Lazy.LOG.info("Uncaught exceptions during {} in thread {}", description, ex.getThread(), ex.getThrowable());
        assertionError.addSuppressed(throwable);
      }
      dumpDebugInfo(logs, description, maxDebugLogsCollected);
      throw assertionError;
    }
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public ExceptionHandoverRegistry getUncaughtExceptionHandler() {
    return uncaughtExceptionHandler;
  }

  @Override
  public String toString() {
    return "DetailOnFailureRunListener{" + "minLogLevel=" + minLogLevel
            + ", maxDebugLogsCollected=" + maxDebugLogsCollected + ", baggages="
            + baggages + ", collectPrinted=" + collectPrinted + '}';
  }

}
