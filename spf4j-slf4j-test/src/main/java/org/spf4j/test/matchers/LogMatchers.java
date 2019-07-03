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
package org.spf4j.test.matchers;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.slf4j.Marker;
import org.spf4j.log.Level;
import org.spf4j.test.log.TestLogRecord;

/**
 * Utility class to create LogRecord matchers.
 * @author Zoltan Farkas
 */
public final class LogMatchers {

  private LogMatchers() { }

  public static Matcher<TestLogRecord> hasMatchingFormat(final Matcher<String> tMatcher) {
    return Matchers.hasProperty("messageFormat", tMatcher);
  }

  public static Matcher<TestLogRecord> hasFormat(final String format) {
    return Matchers.hasProperty("messageFormat", Matchers.equalTo(format));
  }

  public static Matcher<TestLogRecord> hasFormatWithPattern(final String formatPattern) {
    return Matchers.hasProperty("messageFormat", PatternMatcher.matchesPattern(formatPattern));
  }

  public static Matcher<TestLogRecord> hasMatchingMarker(final Matcher<Marker> tMatcher) {
    return Matchers.hasProperty("marker", tMatcher);
  }

  public static Matcher<TestLogRecord> hasMarker(final Marker marker) {
    return Matchers.hasProperty("marker", Matchers.equalTo(marker));
  }

  public static Matcher<TestLogRecord> hasMatchingMessage(final Matcher<String> tMatcher) {
    return Matchers.hasProperty("message", tMatcher);
  }

  public static Matcher<TestLogRecord> hasMessage(final String message) {
    return Matchers.hasProperty("message", Matchers.equalTo(message));
  }

  public static Matcher<TestLogRecord> hasMessageWithPattern(final String messagePattern) {
    return Matchers.hasProperty("message", PatternMatcher.matchesPattern(messagePattern));
  }

  public static Matcher<TestLogRecord> hasLevel(final Level level) {
    return Matchers.hasProperty("level", Matchers.equalTo(level));
  }

  public static Matcher<TestLogRecord> hasNotLoggers(final Set<String> loggers) {
    Iterable<Matcher<String>> matchers = loggers.stream()
            .map((x)  -> Matchers.startsWith(x)).collect(Collectors.toList());
    return Matchers.not(Matchers.hasProperty("loggerName", Matchers.anyOf((Iterable) matchers)));
  }



  public static Matcher<TestLogRecord> hasMatchingArguments(final Matcher<Object[]> matcher) {
     return Matchers.hasProperty("arguments", matcher);
  }

  public static Matcher<TestLogRecord> hasArguments(final Object... objects) {
     return Matchers.hasProperty("arguments", Matchers.arrayContaining(objects));
  }

  public static Matcher<TestLogRecord> hasArgumentAt(final int idx, final Object object) {
     return new BaseMatcher<TestLogRecord>() {
       @Override
       public boolean matches(final Object item) {
         if (item instanceof TestLogRecord) {
           TestLogRecord lr = (TestLogRecord) item;
           Object[] arguments = lr.getArguments();
           return idx < arguments.length && Objects.equals(arguments[idx], object);
         } else {
           return false;
         }
       }

       @Override
       public void describeTo(final Description description) {
         description.appendText("Message argument [").appendValue(idx).appendText("] is ").appendValue(object);
       }
     };
  }

  public static Matcher<TestLogRecord> hasMatchingArgumentAt(final int idx, final Matcher<Object> matcher) {
     return new BaseMatcher<TestLogRecord>() {
       @Override
       public boolean matches(final Object item) {
         if (item instanceof TestLogRecord) {
           TestLogRecord lr = (TestLogRecord) item;
           Object[] arguments = lr.getArguments();
           return idx < arguments.length && matcher.matches(arguments[idx]);
         } else {
           return false;
         }
       }

       @Override
       public void describeTo(final Description description) {
         description.appendText("Message argument [").appendValue(idx).appendText("] matches ");
         matcher.describeTo(description);
       }
     };
  }

  public static Matcher<TestLogRecord> hasAttachment(final String attachment) {
     return Matchers.hasProperty("attachments", Matchers.hasItem(attachment));
  }

  public static Matcher<TestLogRecord> noAttachment(final String attachment) {
     return Matchers.not(Matchers.hasProperty("attachments", Matchers.hasItem(attachment)));
  }

  public static Matcher<TestLogRecord> hasExtraArguments(final Object... objects) {
     return Matchers.hasProperty("extraArguments", Matchers.arrayContaining(objects));
  }

    public static Matcher<TestLogRecord> hasExtraArgumentAt(final int idx, final Object object) {
     return new BaseMatcher<TestLogRecord>() {
       @Override
       public boolean matches(final Object item) {
         if (item instanceof TestLogRecord) {
           TestLogRecord lr = (TestLogRecord) item;
           Object[] arguments = lr.getExtraArguments();
           return idx < arguments.length && Objects.equals(arguments[idx], object);
         } else {
           return false;
         }
       }

       @Override
       public void describeTo(final Description description) {
         description.appendText("Log extra argument [").appendValue(idx).appendText("] is ").appendValue(object);
       }
     };
  }

    public static Matcher<TestLogRecord> hasExtraArgument(final Object object) {
     return new BaseMatcher<TestLogRecord>() {
       @Override
       public boolean matches(final Object item) {
         if (item instanceof TestLogRecord) {
           TestLogRecord lr = (TestLogRecord) item;
           Object[] arguments = lr.getExtraArguments();
           for (Object arg : arguments) {
             if (Objects.equals(arg, object)) {
               return true;
             }
           }
         }
         return false;
       }

       @Override
       public void describeTo(final Description description) {
         description.appendText("Log extra argument is ").appendValue(object);
       }
     };
  }


  public static Matcher<TestLogRecord> hasMatchingExtraArgumentsContaining(final Matcher<Object>... matcher) {
     return Matchers.hasProperty("extraArguments", Matchers.arrayContaining(matcher));
  }

  public static Matcher<TestLogRecord> hasMatchingExtraArguments(final Matcher<Object[]> matcher) {
     return Matchers.hasProperty("extraArguments", matcher);
  }

  public static Matcher<TestLogRecord> hasMatchingExtraThrowable(final Matcher<Throwable> matcher) {
     return Matchers.hasProperty("extraThrowable", matcher);
  }

  public static Matcher<TestLogRecord> hasMatchingExtraThrowableChain(final Matcher<Iterable<Throwable>> matcher) {
     return Matchers.hasProperty("extraThrowableChain", matcher);
  }

}
