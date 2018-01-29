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
import java.util.ArrayDeque;
import java.util.function.Consumer;

/**
 *
 * @author Zoltan Farkas
 */
public abstract class LogCollector implements  LogHandler, LogCollectionHandler {

  private final Level minLevelToCollect;

  private final int maxLogsToCollect;

  private final ArrayDeque<LogRecord> records;

  private final boolean collectPrinted;

  public LogCollector(final Level minLevelToCollect,  final int maxLogsToCollect, final boolean collectPrinted) {
    this.minLevelToCollect = minLevelToCollect;
    this.maxLogsToCollect = maxLogsToCollect;
    records = new ArrayDeque<>();
    this.collectPrinted = collectPrinted;
  }

  @Override
  public final Handling handles(final Level level) {
    return level.ordinal() >= minLevelToCollect.ordinal() ? Handling.HANDLE_PASS : Handling.NONE;
  }

  @Override
  @SuppressFBWarnings("CFS_CONFUSING_FUNCTION_SEMANTICS")
  public final  LogRecord handle(final LogRecord record) {
    if (collectPrinted || !record.hasAttachment(LogPrinter.PRINTED)) {
      synchronized (records) {
        if (records.size() >= maxLogsToCollect) {
          records.removeFirst();
        }
        records.addLast(record);
      }
      record.attach("COLLECTED");
    }
    return record;
  }

  public final Level getMinLevelToCollect() {
    return minLevelToCollect;
  }

  public final int getMaxLogsToCollect() {
    return maxLogsToCollect;
  }

  @Override
  public final int forEach(final Consumer<LogRecord> consumer) {
    synchronized (records) {
      records.stream().forEach(consumer);
      return records.size();
    }
  }

  @Override
  public final String toString() {
    return "DebugLogCollector{" + "minLevelToCollect=" + minLevelToCollect + ", maxLogsToCollect="
            + maxLogsToCollect + '}';
  }

}
