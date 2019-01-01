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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.spf4j.log.Level;

/**
 *
 * @author Zoltan Farkas
 */
@ThreadSafe
final class LogCollectorHandler<A, T> implements LogHandler, LogCollection<T> {

  private final Level fromLevel;
  private final Level toLevel;
  private final boolean passThrough;
  private final Object sync = new Object();
  @GuardedBy("sync")
  private A accObj;
  private final BiConsumer<A, TestLogRecord> acc;
  private final Function<A, T> finisher;
  private final Consumer<LogCollectorHandler<A, T>> onClose;
  private boolean isClosed;
  private final Collector<TestLogRecord, A, T> collector;

  LogCollectorHandler(final Level fromLevel, final Level toLevel,
          final boolean passThrough,
          final Collector<TestLogRecord, A, T> collector, final Consumer<LogCollectorHandler<A, T>> onClose) {
    this.collector = collector;
    this.fromLevel = fromLevel;
    this.toLevel = toLevel;
    this.passThrough = passThrough;
    this.accObj = collector.supplier().get();
    this.acc = collector.accumulator();
    this.finisher = collector.finisher();
    this.onClose = onClose;
    this.isClosed = false;
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
  public TestLogRecord handle(final TestLogRecord record) {
    synchronized (sync) {
      if (!isClosed) {
        acc.accept(accObj, record);
      }
    }
    if (passThrough) {
      return record;
    } else {
      return null;
    }
  }

  @Override
  public T get() {
    synchronized (sync) {
      T result = finisher.apply(accObj);
      // Create a copy... relies on combiner using left arg to mutate, if it mutates...
      accObj = collector.combiner().apply(collector.supplier().get(), accObj);
      return result;
    }
  }


  @Override
  public void close() {
    synchronized (sync) {
      if (!isClosed) {
        try {
          onClose.accept(this);
        } finally {
          isClosed = true;
        }
      }
    }
  }

  @Override
  public String toString() {
    return "LogCollectorHandler{" + "accObj=" + accObj + ", fromLevel="
            + fromLevel + ", toLevel=" + toLevel + ", passThrough=" + passThrough + '}';
  }

}
