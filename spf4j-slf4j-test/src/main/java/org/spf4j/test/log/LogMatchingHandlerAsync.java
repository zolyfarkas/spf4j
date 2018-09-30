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

import java.util.concurrent.TimeUnit;
import org.hamcrest.Matcher;
import org.spf4j.base.TimeSource;

/**
 *
 * @author Zoltan Farkas
 */
abstract class LogMatchingHandlerAsync extends LogMatchingHandler {

  private final long timeout;
  private final TimeUnit tu;

  LogMatchingHandlerAsync(final boolean assertSeen, final String category,
          final Level minLevel, final long timeout, final TimeUnit tu,  final Matcher<LogRecord>... matchers) {
    super(assertSeen, category, minLevel, matchers);
    this.timeout = timeout;
    this.tu = tu;
  }

  public abstract void close();

  @Override
  public void matched() {
    sync.notifyAll();
  }

  @Override
  public void assertObservation() {
    long deadline = TimeSource.nanoTime() + tu.toNanos(timeout);
    try {
      synchronized (sync) {
        while (assertSeen ? at < matchers.length : at >= matchers.length) {
          long nanosToDeadline = deadline - TimeSource.nanoTime();
          if (nanosToDeadline <= 0) {
            if (assertSeen) {
              throw new AssertionError(seenDescription().toString());
            } else {
              throw new AssertionError(notSeenDescription().toString());
            }
          }
          TimeUnit.NANOSECONDS.timedWait(sync, nanosToDeadline);
        }
      }
    } catch (InterruptedException ex) {
      throw new AssertionError("Interrupted " + this, ex);
    }
  }

  @Override
  public String toString() {
    return "LogMatchingHandlerAsync{" + "timeout=" + timeout + ", tu=" + tu + ", super = " + super.toString() + '}';
  }

}
