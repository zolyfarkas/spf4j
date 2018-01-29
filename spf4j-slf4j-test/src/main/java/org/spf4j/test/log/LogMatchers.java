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

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.slf4j.Marker;

/**
 * Utility class to create LogRecord matchers.
 * @author Zoltan Farkas
 */
public final class LogMatchers {

  private LogMatchers() { }

  public static Matcher<LogRecord> hasFormat(final Matcher<String> tMatcher) {
    return Matchers.hasProperty("format", tMatcher);
  }

  public static Matcher<LogRecord> hasMarker(final Matcher<Marker> tMatcher) {
    return Matchers.hasProperty("marker", tMatcher);
  }

  public static Matcher<LogRecord> hasMessage(final Matcher<String> tMatcher) {
    return Matchers.hasProperty("message", tMatcher);
  }

  public static Matcher<LogRecord> hasLevel(final Level level) {
    return Matchers.hasProperty("level", Matchers.equalTo(level));
  }

  public static Matcher<LogRecord> hasArguments(final Matcher<Object[]> matcher) {
     return Matchers.hasProperty("arguments", matcher);
  }

  public static Matcher<LogRecord> hasExtraArguments(final Matcher<Object[]> matcher) {
     return Matchers.hasProperty("extraArguments", matcher);
  }

  public static Matcher<LogRecord> hasExtraThrowable(final Matcher<Throwable> matcher) {
     return Matchers.hasProperty("firstExtraThrowable", matcher);
  }

}
