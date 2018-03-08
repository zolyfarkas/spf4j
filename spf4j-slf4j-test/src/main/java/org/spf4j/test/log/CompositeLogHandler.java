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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
final class CompositeLogHandler implements LogConsumer {

  private static final Interner<CompositeLogHandler> INTERNER =
          Interners.newBuilder().concurrencyLevel(8).weak().build();

  private int hash = 0;

  @VisibleForTesting
  final List<LogHandler> logHandlers;

  private CompositeLogHandler(final List<LogHandler> logHandlers) {
    this.logHandlers = logHandlers;
  }

  @Nullable
  static LogConsumer from(final List<LogHandler> logHandlers) {
    if (logHandlers.isEmpty()) {
      return null;
    } else {
      return INTERNER.intern(new CompositeLogHandler(logHandlers));
    }
  }

  @Override
  public void accept(final LogRecord record) {
    LogRecord logRecord = record;
    for (LogHandler handler : logHandlers) {
      logRecord = handler.handle(logRecord);
      if (logRecord == null) {
        break;
      }
    }
  }

  @Override
  public int hashCode() {
    if (hash == 0) {
      hash = Objects.hashCode(this.logHandlers);
      return hash;
    } else {
      return hash;
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final CompositeLogHandler other = (CompositeLogHandler) obj;
    return Objects.equals(this.logHandlers, other.logHandlers);
  }

  @Override
  public String toString() {
    return "CompositeLogHandler{" + "logHandlers=" + logHandlers + '}';
  }

}
