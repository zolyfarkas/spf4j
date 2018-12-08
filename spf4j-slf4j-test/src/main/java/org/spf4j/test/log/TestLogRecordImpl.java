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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Marker;
import org.spf4j.log.Level;
import org.spf4j.log.Slf4jLogRecordImpl;

/**
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
@ThreadSafe
public final class TestLogRecordImpl extends Slf4jLogRecordImpl implements TestLogRecord {

  private Set<Object> attachments;

  public TestLogRecordImpl(final String loggerName, final Level level,
          final String format, final Object... arguments) {
    this(loggerName, level, null, format, arguments);
  }

  public TestLogRecordImpl(final String loggerName, final Level level,
          @Nullable final Marker marker, final String format, final Object... arguments) {
    super(loggerName, level, marker, format, arguments);
    this.attachments = Collections.EMPTY_SET;
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
  public synchronized void attach(final Object obj) {
    if (attachments.isEmpty()) {
      attachments = new HashSet<>(2);
    }
    attachments.add(obj);
  }

  @Override
  public synchronized boolean hasAttachment(final Object obj) {
    return attachments.contains(obj);
  }

  @Override
  public synchronized Set<Object> getAttachments() {
    return attachments.isEmpty() ? attachments : Collections.unmodifiableSet(attachments);
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(128);
    LogPrinter.printTo(result, this, "");
    return result.toString();
  }

}
