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

import com.google.common.base.Throwables;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;
import org.spf4j.log.Level;
import org.spf4j.log.Slf4jLogRecordImpl;

/**
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
@ThreadSafe
public final class TestLogRecordImpl extends Slf4jLogRecordImpl implements TestLogRecord {

  public TestLogRecordImpl(final String loggerName, final Level level,
          final String format, final Object... arguments) {
    this(loggerName, level, null, format, arguments);
  }

  public TestLogRecordImpl(final String loggerName, final Level level,
          @Nullable final Marker marker, final String format, final Object... arguments) {
    super(loggerName, level, marker, format, arguments);
  }

  public TestLogRecordImpl(final String loggerName, final Level level,
          final List<Marker> markers, final List<KeyValuePair> kvs, final long currentTime,
          final String format, final Object... arguments) {
    super(false, loggerName, level, markers, kvs, currentTime, format, arguments);
  }

  @Nonnull
  @Override
  public List<Throwable> getExtraThrowableChain() {
    Throwable extraThrowable = getExtraThrowable();
    if (extraThrowable == null) {
      return Collections.EMPTY_LIST;
    }
    return Throwables.getCausalChain(extraThrowable);
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(128);
    LogPrinter.PRINTER.printTo(result, this, "");
    return result.toString();
  }

}
