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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.spf4j.base.EscapeJsonStringAppendableWrapper;

/**
 *
 * @author Zoltan Farkas
 */
abstract class LogMatchingHandler implements LogHandler, LogAssert {

  private final Level minLevel;

  private final Matcher<LogRecord>[] matchers;

  private final List<LogRecord> seen;

  private int at;

  LogMatchingHandler(final Level minLevel, final Matcher<LogRecord>... matchers) {
    if (matchers.length < 1) {
      throw new IllegalArgumentException("You need to provide at least a matcher " + matchers);
    }
    this.matchers = matchers;
    this.at = 0;
    this.minLevel = minLevel;
    this.seen = new ArrayList<>();
  }

  abstract void unregister();


  @Override
  public boolean handles(final Level level) {
    return level.ordinal() >= minLevel.ordinal();
  }

  @Override
  public LogRecord handle(final LogRecord record) {
    if (at < matchers.length && matchers[at].matches(record)) {
      at++;
    }
    seen.add(record);
    record.attach(DefaultAsserter.ASSERTED);
    return record;
  }

  /**
   * Assert that a sequence of leg messages has been seen.
   */
  @Override
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
      description.appendText("\n but have seen:\n");
      StringBuilder result = new StringBuilder(seen.size() * 120);
      EscapeJsonStringAppendableWrapper wrapper = new EscapeJsonStringAppendableWrapper(result);
      for (LogRecord record : seen) {
        try {
          LogPrinter.print(record, result, wrapper, "");
        } catch (IOException ex) {
          throw new UncheckedIOException(ex);
        }
      }
      description.appendText(result.toString());
      throw new AssertionError(description.toString());
    }
  }

  /**
   * Assert that a sequence of messages has not been seen.
   */
  @Override
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
      description.appendText("\n but have seen:\n");
      StringBuilder result = new StringBuilder(seen.size() * 120);
      EscapeJsonStringAppendableWrapper wrapper = new EscapeJsonStringAppendableWrapper(result);
      for (LogRecord record : seen) {
        try {
          LogPrinter.print(record, result, wrapper, "");
        } catch (IOException ex) {
          throw new UncheckedIOException(ex);
        }
      }
      description.appendText(result.toString());
      throw new AssertionError(description.toString());
    }
  }

  @Override
  public String toString() {
    return "LogMatchingHandler{" + "minLevel=" + minLevel + ", matchers=" + matchers
            + ", seen=" + seen + ", at=" + at + '}';
  }


}
