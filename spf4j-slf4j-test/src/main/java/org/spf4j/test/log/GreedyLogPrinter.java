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

/**
 * A Greedy log printer.
 * will print all logs above a log level, BUT will mark printed logs as PRINTED or NO_NOT_PRINT otherwise,
 * as such downstream printers will not print them.
 *
 * @author Zoltan Farkas
 */
public final class GreedyLogPrinter implements LogHandler {

  private final LogPrinter handler;

  public GreedyLogPrinter(final LogPrinter handler) {
    this.handler = handler;
  }

  @Override
  public Handling handles(final Level level) {
      return Handling.HANDLE_PASS;
  }

  @Override
  @SuppressFBWarnings("CFS_CONFUSING_FUNCTION_SEMANTICS")
  public LogRecord handle(final LogRecord record) {
    if (handler.handles(record.getLevel()) != Handling.NONE) {
      return handler.handle(record);
    }
    record.attach(LogPrinter.DO_NOT_PRINT);
    return record;
  }

  @Override
  public String toString() {
    return "GreedyLogPrinter{" + "handler=" + handler + '}';
  }

}
