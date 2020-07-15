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
import org.spf4j.base.TimeSource;
import org.spf4j.log.Level;

/**
 *
 * @author Zoltan Farkas
 */
abstract class LogMatchingHandlerAsync extends LogMatchingHandler {

  private final long timeout;
  private final TimeUnit tu;
  private final Object sync;

  LogMatchingHandlerAsync(
          final Level minLevel, final long timeout, final TimeUnit tu, final LogStreamMatcher streamMatcher) {
    super(minLevel, streamMatcher);
    this.timeout = timeout;
    this.tu = tu;
    this.sync = new Object();
  }

  public abstract void close();


  @Override
  public TestLogRecord handle(final TestLogRecord record) {
    TestLogRecord result = super.handle(record);
    synchronized (sync) {
      sync.notifyAll();
    }
    return result;
  }



  @Override
  public void assertObservation() {
    long deadline = TimeSource.nanoTime() + tu.toNanos(timeout);
    try {
      synchronized (sync) {
        while (!streamMatcher.isMatched()) {
          long nanosToDeadline = deadline - TimeSource.nanoTime();
          if (nanosToDeadline <= 0) {
              throw new AssertionError(this);
          }
          TimeUnit.NANOSECONDS.timedWait(sync, nanosToDeadline);
        }
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().isInterrupted();
    }
  }

  @Override
  public String toString() {
    return "LogMatchingHandlerAsync{" + "timeout=" + timeout + ", tu=" + tu + ", super = " + super.toString() + '}';
  }

}
