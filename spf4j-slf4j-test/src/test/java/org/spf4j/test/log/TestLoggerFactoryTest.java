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

import org.spf4j.test.matchers.LogMatchers;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.Method;
import org.spf4j.io.MimeTypes;
import org.spf4j.test.log.annotations.CollectLogs;
import org.spf4j.test.log.annotations.ExpectLog;
import org.spf4j.test.log.annotations.PrintLogs;
import org.spf4j.test.log.annotations.PrintLogsConfigs;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("LO_INCORRECT_NUMBER_OF_ANCHOR_PARAMETERS")
public class TestLoggerFactoryTest {

  private static final Logger LOG = LoggerFactory.getLogger(TestLoggerFactoryTest.class);

  @Test
  public void testSomeHandler() {
    TestLoggers sys = TestLoggers.sys();
    LogAssert expect = sys.expect("", Level.TRACE, LogMatchers.hasAttachment(LogPrinter.PRINTED));
    sys.print("org.spf4j.test", Level.TRACE);
    LOG.trace("test");
    expect.assertObservation();
  }

  @Test
  @PrintLogs(category = "org.spf4", ideMinLevel = Level.TRACE, minLevel = Level.TRACE)
  public void testSomeHandler2() {
    LogAssert expect = TestLoggers.sys().expect("", Level.TRACE, LogMatchers.hasAttachment(LogPrinter.PRINTED));
    LOG.trace("test");
    expect.assertObservation();
  }


  @Test
  @PrintLogsConfigs(
          {
            @PrintLogs(ideMinLevel = Level.TRACE),
            @PrintLogs(category = "com.sun", ideMinLevel = Level.WARN)
          }
  )
  @CollectLogs(minLevel = Level.TRACE)
  public void testLogging() {
    TestLoggers tLog = TestLoggers.sys();
    logTests();
    logMarkerTests();
    LogAssert expect = tLog.expect("org.spf4j.test", Level.ERROR,
            LogMatchers.hasMatchingFormat(Matchers.equalTo("Booo")));
    LOG.error("Booo", new RuntimeException());
    expect.assertObservation();
  }

  public static void logTests() {
    LOG.trace("Hello logger", new RuntimeException());
    LOG.trace("Hello logger");
    LOG.trace("Hello logger {}", 1);
    LOG.trace("Hello logger {} {} {}", 1, 2, 3);
    LOG.trace("Hello logger {} {} {}", 1, 2, 3, new RuntimeException());
    LOG.debug("Hello logger", new RuntimeException());
    LOG.debug("Hello logger");
    LOG.debug("Hello logger {}", 1);
    LOG.debug("Hello logger {} {} {}", 1, 2, 3);
    LOG.debug("Hello logger {} {} {}", 1, 2, 3, new RuntimeException());
    LOG.info("Hello logger", new RuntimeException());
    LOG.info("Hello logger");
    LOG.info("Hello logger {}", 1);
    LOG.info("Hello logger {} {} {}", 1, 2, 3);
    LOG.info("Hello logger {} {} {}", 1, 2, 3, new RuntimeException());
    LOG.warn("Hello logger", new RuntimeException());
    LOG.warn("Hello logger");
    LOG.warn("Hello logger {}", 1);
    LOG.warn("Hello logger {} {} {}", 1, 2, 3);
    LOG.warn("Hello logger {} {} {}", 1, 2, 3, new RuntimeException());
    LogAssert expect = TestLoggers.sys().expect("org.spf4j.test", Level.ERROR, 5,
            LogMatchers.hasMatchingFormat(Matchers.containsString("Hello logger")));
    LOG.error("Hello logger", new RuntimeException());
    LOG.error("Hello logger");
    LOG.error("Hello logger {}", 1);
    LOG.error("Hello logger {} {} {}", 1, 2, 3);
    LOG.error("Hello logger {} {} {}", 1, 2, 3, new RuntimeException());
    expect.assertObservation();
  }

  public static void logMarkerTests() {
    Marker marker = MarkerFactory.getMarker("TEST");
    Marker marker2 = MarkerFactory.getMarker("TEST2");
    marker2.add(marker);
    marker2.add(MarkerFactory.getMarker("TEST3"));

    LOG.trace(marker, "Hello logger", new RuntimeException());
    LOG.trace(marker, "Hello logger");
    LOG.trace(marker, "Hello logger {}", 1);
    LOG.trace(marker, "Hello logger {} {} {}", 1, 2, 3);
    LOG.trace(marker, "Hello logger {} {} {}", 1, 2, 3, new RuntimeException());
    LOG.debug(marker, "Hello logger", new RuntimeException());
    LOG.debug(marker, "Hello logger");
    LOG.debug(marker, "Hello logger {}", 1);
    LOG.debug(marker, "Hello logger {} {} {}", 1, 2, 3);
    LOG.debug(marker, "Hello logger {} {} {}", 1, 2, 3, new RuntimeException());
    LOG.info(marker, "Hello logger", new RuntimeException());
    LOG.info(marker2, "Hello logger");
    LOG.info(marker2, "Hello logger {}", 1);
    LOG.info(marker2, "Hello logger {} {} {}", 1, 2, 3);
    LOG.info(marker2, "Hello logger {} {} {}", 1, 2, 3, new RuntimeException());
    LOG.warn(marker, "Hello logger", new RuntimeException());
    LOG.warn(marker, "Hello logger");
    LOG.warn(marker, "Hello logger {}", 1);
    LOG.warn(marker, "Hello logger {} {} {}", 1, 2, 3);
    LOG.warn(marker, "Hello logger {} {} {}", 1, 2, 3, new RuntimeException());
    LogAssert expect = TestLoggers.sys().expect("org.spf4j.test", Level.ERROR, 5,
            Matchers.allOf(
                    LogMatchers.hasMatchingFormat(Matchers.containsString("Hello logger")),
                    LogMatchers.hasMarker(marker)));
    LOG.error(marker, "Hello logger", new RuntimeException());
    LOG.error(marker, "Hello logger");
    LOG.error(marker, "Hello logger {}", 1);
    LOG.error(marker, "Hello logger {} {} {}", 1, 2, 3);
    LOG.error(marker, "Hello logger {} {} {}", 1, 2, 3, new RuntimeException());
    expect.assertObservation();
  }

  @Test
  @SuppressFBWarnings("LO_TOSTRING_PARAMETER") // this is on purpose configs change over time.
  public void testIsEnabled() {
    LOG.debug("Log Config: {}", TestLoggers.sys().toString());
    Marker marker = MarkerFactory.getMarker("TEST");
    Assert.assertFalse(LOG.isTraceEnabled());
    Assert.assertFalse(LOG.isTraceEnabled(marker));

    //DEBUG is enabled since debug logs are collected in the background...
    Assert.assertTrue(LOG.isDebugEnabled());
    Assert.assertTrue(LOG.isDebugEnabled(marker));

    Assert.assertTrue(LOG.isInfoEnabled());
    Assert.assertTrue(LOG.isInfoEnabled(marker));
    Assert.assertTrue(LOG.isWarnEnabled());
    Assert.assertTrue(LOG.isWarnEnabled(marker));
    Assert.assertTrue(LOG.isErrorEnabled());
    Assert.assertTrue(LOG.isErrorEnabled(marker));
  }

  @Test(expected = AssertionError.class)
  public void testLogging2() {
    LogAssert expect = TestLoggers.sys().dontExpect("org.spf4j.test", Level.ERROR,
            LogMatchers.hasFormat("Booo"));
    LOG.error("Booo", new RuntimeException());
    expect.assertObservation();
  }

  @Test
  @ExpectLog(level = Level.ERROR)
  @SuppressFBWarnings("UTAO_JUNIT_ASSERTION_ODDITIES_NO_ASSERT")
  public void testLoggingAnnot() {
    LOG.error("Booo", new RuntimeException());
  }

  @Test
  public void testLoggingJul() {
    LogAssert expect = TestLoggers.sys().expect("my.test", Level.DEBUG,
            LogMatchers.hasFormat("Bla Bla"),
            LogMatchers.hasFormat("Boo Boo param"),
            Matchers.allOf(LogMatchers.hasFormat("test source"),
                    LogMatchers.hasExtraArguments(new Method(TestLoggerFactoryTest.class.getName(),
                            "testLoggingJul"))));
    java.util.logging.Logger logger = java.util.logging.Logger.getLogger("my.test");
    logger.info("Bla Bla");
    logger.log(java.util.logging.Level.FINE, "Boo Boo {0}", "param");
    logger.logp(java.util.logging.Level.INFO, TestLoggerFactoryTest.class.getName(), "testLoggingJul", "test source");
    expect.assertObservation();
  }

  @Test
  @CollectLogs(minLevel = Level.TRACE)
  public void testIgnore() {
    TestLoggers config = TestLoggers.sys();
    try (HandlerRegistration ir = config.ignore("org.spf4j.test", Level.DEBUG, Level.ERROR)) {
      LogAssert assrt = config.expect("", Level.TRACE, LogMatchers.hasFormat("trace"));
      LogAssert assrt2 = config.dontExpect("", Level.DEBUG, LogMatchers.hasFormat("Bla bla"));
      LOG.debug("Bla bla");
      LOG.trace("trace");
      assrt.assertObservation();
      assrt2.assertObservation();
    }
  }

  @Test
  @CollectLogs(minLevel = Level.TRACE)
  public void testCollect() {
    TestLoggers config = TestLoggers.sys();
    try (LogCollection<Long> c = config.collect("org.spf4j.test", Level.INFO, true, Collectors.counting())) {
      LOG.info("m1");
      LOG.info("m2");
      Assert.assertEquals(2L, (long) c.get());
    }
  }

  @Test
  public void testLogJson() {
    LogAssert expect = TestLoggers.sys().expect("", Level.INFO,
            Matchers.allOf(LogMatchers.hasFormat("Json Payload"),
                    LogMatchers.hasMatchingExtraArguments(Matchers.arrayContaining(this))));
    LogPrinter.getAppenderSupplier().register(TestLoggerFactoryTest.class,
            MimeTypes.APPLICATION_JSON, (o, a) -> {
              a.append("{\"a\" : \"b\"}");
            });
    LOG.info("Json Payload", this);
    expect.assertObservation();
  }

  @Test(expected = AssertionError.class)
  public void testLogging3() {
    LogAssert expect = TestLoggers.sys().expect("org.spf4j.test", Level.ERROR,
            LogMatchers.hasFormat("Booo"));
    expect.assertObservation();
  }

  @Test
  public void testLogging33() {
    LogAssert expect = TestLoggers.sys().dontExpect("org.spf4j.test", Level.ERROR,
            LogMatchers.hasMatchingFormat(Matchers.equalTo("Booo")));
    expect.assertObservation();
  }

  @Ignore
  @Test
  @CollectLogs(minLevel = Level.TRACE, collectPrinted = true)
  public void testLogging4() {
    LOG.trace("lala");
    LOG.debug("log {}", 1);
    LOG.debug("log {} {}", 1, 2);
    LOG.debug("log {} {} {}", 1, 2, 3);
    LOG.debug("log {} {} {}", 1, 2, 3, 4);
    Assert.fail("booo");
  }

  @Test
  public void testUncaught() throws InterruptedException {
    IllegalStateException ex = new IllegalStateException();
    Thread thread = new Thread(() -> {
      throw ex;
    });
    TestLoggers config = TestLoggers.sys();
    try (LogAssert obs = config
            .expectUncaughtException(5, TimeUnit.SECONDS,
                    UncaughtExceptionDetail.hasThrowable(Matchers.equalTo(ex)))) {
      thread.start();
      obs.assertObservation();
    }
  }

  @SuppressFBWarnings("UTAO_JUNIT_ASSERTION_ODDITIES_NO_ASSERT")
  @Test
  public void testUncaught2() throws InterruptedException {
    TestLoggers config = TestLoggers.sys();
    LogAssert obs = config
            .expectUncaughtException(3, TimeUnit.SECONDS,
                    UncaughtExceptionDetail.hasThrowable(Matchers.equalTo(new IllegalStateException())));
    try {
      obs.assertObservation();
      Assert.fail();
    } catch (AssertionError ex) {
      // expected
    }
  }

  @Test
  public void testLogging5() {
    LogCollection<ArrayDeque<LogRecord>> collect = TestLoggers.sys().collect(Level.DEBUG, 10, true);
    LOG.debug("log {}", 1);
    LOG.debug("log {} {}", 1, 2);
    LOG.debug("log {} {} {}", 1, 2, 3);
    LOG.debug("log {} {} {}", 1, 2, 3, 4);
    Assert.assertEquals(4, collect.get().size());
  }

  @Test
  public void testExecutionContext() {
     Assert.assertNotNull(ExecutionContexts.current());
  }


  @Test
  public void testAsyncLogging() throws InterruptedException {
    LogAssert expect = TestLoggers.sys().expect("org.spf4j.test.log", Level.ERROR, 3, TimeUnit.SECONDS,
            LogMatchers.hasFormat("async"));
    new Thread(() -> {
      LOG.error("async");
    }).start();
    expect.assertObservation();
  }

  @Test(expected = AssertionError.class)
  public void testAsyncLogging2() throws InterruptedException {
    LogAssert expect = TestLoggers.sys().expect("org.spf4j.test.log", Level.ERROR, 3, TimeUnit.SECONDS,
            LogMatchers.hasFormat("async"));
    new Thread(() -> {
      LOG.info("coco");
    }).start();
    expect.assertObservation();
  }

  @Test
  public void testIntercept() throws InterruptedException {
    TestLoggers sys = TestLoggers.sys();
    LogAssert expect = sys.dontExpect("org.spf4j.test.log", Level.INFO, (Matcher) Matchers.anything());
    HandlerRegistration reg = sys.interceptAllLevels("org.spf4j.test.log", (l) -> null);
    LOG.info("bla");
    expect.assertObservation();
    reg.close();
  }


}
