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

import java.util.SortedSet;
import org.spf4j.log.Level;

/**
 * @author Zoltan Farkas
 */
final class DefaultAsserter implements LogHandler {

  private final SortedSet<String> excludeCategories;

  DefaultAsserter(final SortedSet<String> excludeCategories) {
    this.excludeCategories = excludeCategories;
  }

  public static boolean isInCategories(final SortedSet<String> categories, final String loggername) {
    if (categories.isEmpty()) {
      return false;
    }
    for (String cat : categories.tailSet(loggername)) {
      if (loggername.startsWith(cat)) {
        return true;
      } else if (!loggername.isEmpty() && cat.charAt(0) != loggername.charAt(0)) {
        break;
      }
    }
    return false;
  }


  @Override
  public Handling handles(final Level level) {
    return level == Level.ERROR ? Handling.HANDLE_CONSUME : Handling.NONE;
  }

  @Override
  public TestLogRecord handle(final TestLogRecord record) {
    if (!record.hasAttachment(Attachments.ASSERTED) && !isInCategories(excludeCategories, record.getLoggerName())) {
      throw new AssertionError("Most test should not log errors, if a error scenario is validated,"
              + " please assert this behavior using TestLoggers.expect, received:\n" + record,
              record.getExtraThrowable());
    }
    return record;
  }

  @Override
  public String toString() {
    return "DefaultAsserter{" + "excludeCategories=" + excludeCategories + '}';
  }

}
