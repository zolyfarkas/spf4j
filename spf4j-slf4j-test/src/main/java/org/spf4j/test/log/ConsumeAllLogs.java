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

import javax.annotation.Nullable;

/**
 * @author Zoltan Farkas
 */
final class ConsumeAllLogs implements LogHandler {

  private final Level from;

  private final Level to;

  ConsumeAllLogs(final Level from, final Level to) {
    this.from = from;
    this.to = to;
  }



  @Override
  public Handling handles(final Level level) {
    int ordinal = level.ordinal();
    return  (from.ordinal() <= ordinal && to.ordinal() >= ordinal)
            ?  Handling.HANDLE_CONSUME : Handling.NONE;
  }

  @Override
  @Nullable
  public LogRecord handle(final LogRecord record) {
    return null;
  }

  @Override
  public String toString() {
    return "ConsumeAllLogs{" + "from=" + from + ", to=" + to + '}';
  }

}
