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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("LO_INCORRECT_NUMBER_OF_ANCHOR_PARAMETERS")
public class TestLoggerFactoryTest {

  private static final Logger LOG = LoggerFactory.getLogger(TestLoggerFactoryTest.class);

  @Test
  @SuppressFBWarnings({"PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS", "UTAO_JUNIT_ASSERTION_ODDITIES_NO_ASSERT"})
  public void testLogging() {
    try (HandlerRegistration printer = TestLoggers.config().print("org.spf4j.test", Level.TRACE)) {
      logTests();
      logMarkerTests();
      LogAssert expect = TestLoggers.config().expect("org.spf4j.test", Level.ERROR,
              LogMatchers.hasMatchingFormat(Matchers.equalTo("Booo")));
      LOG.error("Booo", new RuntimeException());
      expect.assertObservation();
    }
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
    LogAssert expect = TestLoggers.config().expect("org.spf4j.test", Level.ERROR, 4,
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
    LogAssert expect = TestLoggers.config().expect("org.spf4j.test", Level.ERROR, 4,
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
    LOG.debug("Log Config: {}", TestLoggers.config().toString());
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
    LogAssert expect = TestLoggers.config().dontExpect("org.spf4j.test", Level.ERROR,
            LogMatchers.hasFormat("Booo"));
    LOG.error("Booo", new RuntimeException());
    expect.assertObservation();
  }

  @Test
  @SuppressFBWarnings("UTAO_JUNIT_ASSERTION_ODDITIES_NO_ASSERT")
  public void testLoggingJul() {
    LogAssert expect = TestLoggers.config().expect("my.test", Level.DEBUG,
            LogMatchers.hasFormat("Bla Bla"),
            LogMatchers.hasFormat("Boo Boo param"));
    java.util.logging.Logger logger = java.util.logging.Logger.getLogger("my.test");
    logger.info("Bla Bla");
    logger.log(java.util.logging.Level.FINE, "Boo Boo {0}", "param");
    expect.assertObservation();
  }


  @Test
  @SuppressFBWarnings("UTAO_JUNIT_ASSERTION_ODDITIES_NO_ASSERT")
  public void testIgnore() {
    TestLoggers config = TestLoggers.config();
    try (HandlerRegistration ir = config.ignore("org.spf4j.test", Level.DEBUG, Level.ERROR)) {
      LogAssert assrt = config.expect("", Level.TRACE, LogMatchers.hasFormat("trace"));
      LogAssert assrt2 = config.dontExpect("", Level.DEBUG, LogMatchers.hasFormat("Bla bla"));
      LOG.debug("Bla bla");
      LOG.trace("trace");
      assrt.assertObservation();
      assrt2.assertObservation();
    }
  }

  @Test(expected = AssertionError.class)
  public void testLogging3() {
    LogAssert expect = TestLoggers.config().expect("org.spf4j.test", Level.ERROR,
            LogMatchers.hasFormat("Booo"));
    expect.assertObservation();
  }

  @Test
  @SuppressFBWarnings("UTAO_JUNIT_ASSERTION_ODDITIES_NO_ASSERT")
  public void testLogging33() {
    LogAssert expect = TestLoggers.config().dontExpect("org.spf4j.test", Level.ERROR,
            LogMatchers.hasMatchingFormat(Matchers.equalTo("Booo")));
    expect.assertObservation();
  }

  @Ignore
  @Test
  public void testLogging4() {
    LOG.debug("log {}", 1);
    LOG.debug("log {} {}", 1, 2);
    LOG.debug("log {} {} {}", 1, 2, 3);
    LOG.debug("log {} {} {}", 1, 2, 3, 4);
    Assert.fail("booo");
  }

  @Test
  @SuppressFBWarnings("UTAO_JUNIT_ASSERTION_ODDITIES_NO_ASSERT")
  public void testUncaught() throws InterruptedException {
    IllegalStateException ex = new IllegalStateException();
    Thread thread = new Thread(() -> {
      throw ex;
    });
    TestLoggers config = TestLoggers.config();
    AsyncObservationAssert obs = config
            .expectUncaughtException(
                    UncaughtExceptionDetail.hasThrowable(Matchers.equalTo(ex)));
    thread.start();
    obs.assertObservation(5, TimeUnit.SECONDS);
  }

  @SuppressFBWarnings("UTAO_JUNIT_ASSERTION_ODDITIES_NO_ASSERT")
  @Test
  public void testUncaught2() throws InterruptedException {
    TestLoggers config = TestLoggers.config();
    AsyncObservationAssert obs = config
            .expectUncaughtException(
                    UncaughtExceptionDetail.hasThrowable(Matchers.equalTo(new IllegalStateException())));
    try {
      obs.assertObservation(5, TimeUnit.SECONDS);
      Assert.fail();
    } catch (AssertionError ex) {
      // expected
    }
  }

  @Test
  public void testLogging5() {
    LogCollectionHandler collect = TestLoggers.config().collect(Level.DEBUG, 10, true);
    LOG.debug("log {}", 1);
    LOG.debug("log {} {}", 1, 2);
    LOG.debug("log {} {} {}", 1, 2, 3);
    LOG.debug("log {} {} {}", 1, 2, 3, 4);
    AtomicInteger count = new AtomicInteger();
    collect.forEach((r) -> {
      count.incrementAndGet();
    });
    Assert.assertEquals(4, count.get());
  }

}
