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
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("LO_INCORRECT_NUMBER_OF_ANCHOR_PARAMETERS")
public class TestLoggerFactoryTest {

  private static final Logger LOG = LoggerFactory.getLogger(TestLoggerFactoryTest.class);

  @Test
  @SuppressFBWarnings({ "PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS", "UTAO_JUNIT_ASSERTION_ODDITIES_NO_ASSERT" })
  public void testLogging() {
    try (HandlerRegistration printer = TestLoggers.config().print("org.spf4j.test", Level.TRACE)) {
      LOG.trace("Hello logger");
      LOG.trace("Hello logger {}", 1);
      LOG.trace("Hello logger {} {} {}", 1, 2, 3);
      LOG.trace("Hello logger {} {} {}", 1, 2, 3, new RuntimeException());
      LOG.debug("Hello logger");
      LOG.debug("Hello logger {}", 1);
      LOG.debug("Hello logger {} {} {}", 1, 2, 3);
      LOG.debug("Hello logger {} {} {}", 1, 2, 3, new RuntimeException());
      LOG.info("Hello logger");
      LOG.info("Hello logger {}", 1);
      LOG.info("Hello logger {} {} {}", 1, 2, 3);
      LOG.info("Hello logger {} {} {}", 1, 2, 3, new RuntimeException());
      LOG.warn("Hello logger");
      LOG.warn("Hello logger {}", 1);
      LOG.warn("Hello logger {} {} {}", 1, 2, 3);
      LOG.warn("Hello logger {} {} {}", 1, 2, 3, new RuntimeException());
      LogAssert expect = TestLoggers.config().expect("org.spf4j.test", Level.ERROR,
              Matchers.hasProperty("format", Matchers.equalTo("Booo")));
      LOG.error("Booo", new RuntimeException());
      expect.assertSeen();
    }
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
