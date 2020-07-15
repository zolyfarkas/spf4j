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
import org.spf4j.log.Level;

/**
 *
 * @author Zoltan Farkas
 */
@SuppressWarnings("checkstyle:VisibilityModifier")
abstract class LogMatchingHandler implements LogHandler, LogAssert {

  private final Level minLevel;

  final LogStreamMatcher streamMatcher;

  LogMatchingHandler(final Level minLevel, final LogStreamMatcher streamMatcher) {
    this.streamMatcher = streamMatcher;
    this.minLevel = minLevel;
  }

  public abstract void close();


  @Override
  public Handling handles(final Level level) {
    return level.ordinal() >= minLevel.ordinal() ? Handling.HANDLE_PASS : Handling.NONE;
  }

  @Override
  @SuppressFBWarnings("CFS_CONFUSING_FUNCTION_SEMANTICS")
  public TestLogRecord handle(final TestLogRecord record) {
    streamMatcher.accept(record);
    return record;
  }


  @Override
  public void assertObservation() {
    close();
    if (!streamMatcher.isMatched()) {
      throw new AssertionError(streamMatcher.toString());
    }
  }


  @Override
  public String toString() {
    return "LogMatchingHandler{" + "minLevel=" + minLevel + ", matchers=" + streamMatcher + '}';
  }


}
