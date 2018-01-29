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
import java.util.Arrays;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

/**
 *
 * @author Zoltan Farkas
 */
abstract class LogMatchingHandler implements LogHandler, LogAssert {

  private final Level minLevel;

  private final Matcher<LogRecord>[] matchers;

  private int at;

  private final boolean assertSeen;

  LogMatchingHandler(final boolean assertSeen, final Level minLevel, final Matcher<LogRecord>... matchers) {
    if (matchers.length < 1) {
      throw new IllegalArgumentException("You need to provide at least a matcher " + Arrays.toString(matchers));
    }
    this.matchers = matchers;
    this.at = 0;
    this.minLevel = minLevel;
    this.assertSeen = assertSeen;
  }

  abstract void unregister();


  @Override
  public Handling handles(final Level level) {
    return level.ordinal() >= minLevel.ordinal() ? Handling.HANDLE_PASS : Handling.NONE;
  }

  @Override
  @SuppressFBWarnings("CFS_CONFUSING_FUNCTION_SEMANTICS")
  public LogRecord handle(final LogRecord record) {
    if (at < matchers.length && matchers[at].matches(record)) {
      at++;
    }
    record.attach(DefaultAsserter.ASSERTED);
    return record;
  }

  @Override
  public void assertObservation() {
    if (assertSeen) {
      assertSeen();
    } else {
      assertNotSeen();
    }
  }


  /**
   * Assert that a sequence of leg messages has been seen.
   */
  @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
  public void assertSeen() {
    unregister();
    if (at < matchers.length) {
      Description description = new StringDescription();
      description.appendText("Expected:\n");
      matchers[0].describeTo(description);
      for (int i = 1; i < matchers.length; i++) {
        description.appendText("\n");
        matchers[i].describeTo(description);
      }
      throw new AssertionError(description.toString());
    }
  }

  /**
   * Assert that a sequence of messages has not been seen.
   */
  @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
  public void assertNotSeen() {
    unregister();
    if (at >= matchers.length) {
      Description description = new StringDescription();
      description.appendText("Not expected:\n");
      matchers[0].describeTo(description);
      for (int i = 1; i < matchers.length; i++) {
        description.appendText("\n");
        matchers[i].describeTo(description);
      }
      throw new AssertionError(description.toString());
    }
  }

  @Override
  public String toString() {
    return "LogMatchingHandler{" + "minLevel=" + minLevel + ", matchers=" + Arrays.toString(matchers)
            + ", at=" + at + '}';
  }


}
