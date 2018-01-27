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
              Matchers.hasProperty("format", Matchers.equalTo("Booo")));
      LOG.error("Booo", new RuntimeException());
      expect.assertSeen();
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
            Matchers.hasProperty("format", Matchers.containsString("Hello logger")));
    LOG.error("Hello logger", new RuntimeException());
    LOG.error("Hello logger");
    LOG.error("Hello logger {}", 1);
    LOG.error("Hello logger {} {} {}", 1, 2, 3);
    LOG.error("Hello logger {} {} {}", 1, 2, 3, new RuntimeException());
    expect.assertSeen();
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
              Matchers.hasProperty("format", Matchers.containsString("Hello logger")),
              Matchers.hasProperty("marker", Matchers.equalTo(marker))));
    LOG.error(marker, "Hello logger", new RuntimeException());
    LOG.error(marker, "Hello logger");
    LOG.error(marker, "Hello logger {}", 1);
    LOG.error(marker, "Hello logger {} {} {}", 1, 2, 3);
    LOG.error(marker, "Hello logger {} {} {}", 1, 2, 3, new RuntimeException());
    expect.assertSeen();
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
    LogAssert expect = TestLoggers.config().expect("org.spf4j.test", Level.ERROR,
            Matchers.hasProperty("format", Matchers.equalTo("Booo")));
    LOG.error("Booo", new RuntimeException());
    expect.assertNotSeen();
  }

  @Test(expected = AssertionError.class)
  public void testLogging3() {
    LogAssert expect = TestLoggers.config().expect("org.spf4j.test", Level.ERROR,
            Matchers.hasProperty("format", Matchers.equalTo("Booo")));
    expect.assertSeen();
  }

  @Test
  @SuppressFBWarnings("UTAO_JUNIT_ASSERTION_ODDITIES_NO_ASSERT")
  public void testLogging33() {
    LogAssert expect = TestLoggers.config().expect("org.spf4j.test", Level.ERROR,
            Matchers.hasProperty("format", Matchers.equalTo("Booo")));
    expect.assertNotSeen();
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
                    Matchers.hasProperty("throwable", Matchers.equalTo(ex)));
    AsyncObservationAssert ass2 = config
            .expectNoUncaughtException(
                    Matchers.hasProperty("throwable", Matchers.any(IllegalArgumentException.class)));
    thread.start();
    ass2.assertObservation(5, TimeUnit.SECONDS);
    obs.assertObservation(5, TimeUnit.SECONDS);
  }

  @SuppressFBWarnings("UTAO_JUNIT_ASSERTION_ODDITIES_NO_ASSERT")
  @Test
  public void testUncaught2() throws InterruptedException {
    TestLoggers config = TestLoggers.config();
    AsyncObservationAssert obs = config
            .expectUncaughtException(
                    Matchers.hasProperty("throwable", Matchers.equalTo(new IllegalStateException())));
    AsyncObservationAssert ass2 = config
            .expectNoUncaughtException(
                    Matchers.hasProperty("throwable", Matchers.any(IllegalArgumentException.class)));
    ass2.assertObservation(5, TimeUnit.SECONDS);
    try {
      obs.assertObservation(5, TimeUnit.SECONDS);
      Assert.fail();
    } catch (AssertionError ex) {
      // expected
    }
  }

  @Test
  @SuppressFBWarnings("UTAO_JUNIT_ASSERTION_ODDITIES_NO_ASSERT")
  public void testUncaught3() throws InterruptedException {
    try (HandlerRegistration reg = TestLoggers.config().print("", Level.TRACE)) {
      IllegalStateException ex = new IllegalStateException();
      Thread thread = new Thread(() -> {
        throw ex;
      });
      TestLoggers config = TestLoggers.config();
      AsyncObservationAssert obs = config
              .expectUncaughtException(
                      Matchers.hasProperty("throwable", Matchers.equalTo(ex)));
      AsyncObservationAssert ass2 = config
              .expectNoUncaughtException(
                      Matchers.hasProperty("throwable", Matchers.any(IllegalArgumentException.class)));
      thread.start();
      Thread thread2 = new Thread(() -> {
        throw new IllegalArgumentException();
      });
      thread2.start();
      try {
        ass2.assertObservation(5, TimeUnit.SECONDS);
        Assert.fail();
      } catch (AssertionError err) {
        // expected
      }
      obs.assertObservation(5, TimeUnit.SECONDS);
    }  }

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
