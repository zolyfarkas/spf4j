/*
 * Copyright 2020 SPF4J.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.concurrent.ThreadSafe;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

/**
 * @author Zoltan Farkas
 */
@ThreadSafe
public final class ExactLogStreamMatcher implements LogStreamMatcher {

  private  final Matcher<TestLogRecord>[] matchers;

  private final List<TestLogRecord> matched;

  private int at;

  private final boolean assertSeen;

  private final Object sync;

  public ExactLogStreamMatcher(final boolean assertSeen, final Matcher<TestLogRecord>... matchers) {
    if (matchers.length < 1) {
      throw new IllegalArgumentException("You need to provide at least a matcher " + Arrays.toString(matchers));
    }
    this.matchers = matchers;
    this.matched = new ArrayList<>(matchers.length);
    this.at = 0;
    this.assertSeen = assertSeen;
    this.sync = new Object();
  }


  @Override
  public boolean isMatched() {
    if (assertSeen) {
      synchronized (sync) {
        return at >= matchers.length;
      }
    } else {
      synchronized (sync) {
        return at < matchers.length;
      }
    }
  }

  @Override
  public void accept(final TestLogRecord record) {
    synchronized (sync) {
      if (at < matchers.length && matchers[at].matches(record)) {
        at++;
        record.attach(Attachments.ASSERTED);
        matched.add(record);
      }
    }
  }

  @Override
  public void describeTo(final Description description) {
    if (assertSeen) {
      description.appendText("Expected: ");
    } else {
      description.appendText("Not expected: ");
    }
    matchers[0].describeTo(description);
    for (int i = 1; i < matchers.length; i++) {
      description.appendText("\n");
      matchers[i].describeTo(description);
    }
    description.appendText("\n");
    description.appendValueList("Matched:\n", ",", " logs", matched);
  }

  @Override
  public String toString() {
    StringDescription sd = new StringDescription();
    describeTo(sd);
    return sd.toString();
  }

}
