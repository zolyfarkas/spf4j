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

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collector;
import javax.annotation.Nullable;

/**
 *
 * @author Zoltan Farkas
 */
abstract class LogCollectorHandler<A, T> implements LogHandler, LogCollection<T> {

  private final A accObj;
  private BiConsumer<A, LogRecord> acc;
  private final Level fromLevel;
  private final Level toLevel;
  private final boolean passThrough;
  private final Function<A, T> finisher;

  LogCollectorHandler(final Level fromLevel, final Level toLevel,
          final boolean passThrough,
          final Collector<LogRecord, A, T> collector) {
    accObj = collector.supplier().get();
    acc = collector.accumulator();
    finisher = collector.finisher();
    this.fromLevel = fromLevel;
    this.toLevel = toLevel;
    this.passThrough = passThrough;
  }

  @Override
  public LogHandler.Handling handles(final Level level) {
    if (level.ordinal() >= fromLevel.ordinal() && level.ordinal() <= toLevel.ordinal()) {
      return passThrough ? LogHandler.Handling.HANDLE_PASS : LogHandler.Handling.HANDLE_CONSUME;
    } else {
      return LogHandler.Handling.NONE;
    }
  }

  @Override
  @Nullable
  public LogRecord handle(final LogRecord record) {
    synchronized (accObj) {
      acc.accept(accObj, record);
    }
    if (passThrough) {
      return record;
    } else {
      return null;
    }
  }

  @Override
  public T get() {
    synchronized (accObj) {
      close();
      return finisher.apply(accObj);
    }
  }

  @Override
  public String toString() {
    return "LogCollectorHandler{" + "accObj=" + accObj + ", fromLevel="
            + fromLevel + ", toLevel=" + toLevel + ", passThrough=" + passThrough + '}';
  }

}
