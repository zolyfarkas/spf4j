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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.spf4j.base.Arrays;
import org.spf4j.base.Slf4jMessageFormatter;
import org.spf4j.io.ObjectAppenderSupplier;

/**
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
@ThreadSafe
public final class LogRecord {


  private final Thread thread;
  private final Logger logger;
  private final Level level;
  private final long timeStamp;
  private final Marker marker;
  private final String format;
  private final Object[] arguments;
  private Set<Object> attachments;
  private int startExtra;
  @Nullable
  private String message;

  @SuppressFBWarnings("LO_SUSPECT_LOG_PARAMETER")
  public LogRecord(final Logger logger, final Level level,
          final String format, final Object... arguments) {
    this(logger, level, null, format, arguments);
  }

  @SuppressFBWarnings("LO_SUSPECT_LOG_PARAMETER")
  public LogRecord(final Logger logger, final Level level,
          @Nullable final Marker marker, final String format, final Object... arguments) {
    this.logger = logger;
    this.level = level;
    this.timeStamp = System.currentTimeMillis();
    this.marker = marker;
    this.format = format;
    this.arguments = arguments;
    this.thread = Thread.currentThread();
    this.attachments = Collections.EMPTY_SET;
    this.startExtra = arguments.length;
    this.message = null;
  }

  public Logger getLogger() {
    return logger;
  }

  public Level getLevel() {
    return level;
  }

  public long getTimeStamp() {
    return timeStamp;
  }

  @Nullable
  public Marker getMarker() {
    return marker;
  }

  public String getFormat() {
    return format;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP") // risk I take...
  public Object[] getArguments() {
    return arguments;
  }

  public Thread getThread() {
    return thread;
  }

  @Nonnull
  public synchronized String getMessage() {
    materializeMessage();
    return message;
  }

  public synchronized void materializeMessage() {
    if (message == null) {
      StringBuilder sb = new StringBuilder(format.length() + arguments.length * 8);
      try {
        this.startExtra = Slf4jMessageFormatter.format(LogPrinter::exHandle, 0, sb, format,
                ObjectAppenderSupplier.TO_STRINGER, arguments);
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
      message = sb.toString();
    }
  }

  @Nonnull
  public synchronized Object[] getExtraArguments() {
    materializeMessage();
    if (startExtra < arguments.length) {
      return java.util.Arrays.copyOfRange(arguments, startExtra, arguments.length);
    } else {
      return Arrays.EMPTY_OBJ_ARRAY;
    }
  }

  @Nullable
  public synchronized Throwable getExtraThrowable() {
    materializeMessage();
    Throwable result = null;
    for (int i = startExtra; i < arguments.length; i++) {
      Object argument = arguments[i];
      if (argument instanceof Throwable) {
        if (result == null) {
          result = (Throwable) argument;
        } else {
          result.addSuppressed((Throwable) argument);
        }
      }
    }
    return result;
  }

  @Nonnull
  public List<Throwable> getExtraThrowableChain() {
    Throwable extraThrowable = getExtraThrowable();
    if (extraThrowable == null) {
      return Collections.EMPTY_LIST;
    }
    return Throwables.getCausalChain(extraThrowable);
  }

  public synchronized void attach(final Object obj) {
    if (attachments.isEmpty()) {
      attachments = new HashSet<>(2);
    }
    attachments.add(obj);
  }

  public synchronized boolean hasAttachment(final Object obj) {
    return attachments.contains(obj);
  }

  public synchronized Set<Object> getAttachments() {
    return attachments.isEmpty() ? attachments : Collections.unmodifiableSet(attachments);
  }

  @Override
  public String toString() {
    return "LogRecord{ thread=" + thread + ", logger=" + logger + ", level="
            + level + ", timeStamp=" + timeStamp + ", marker=" + marker + ", format="
            + format + ", arguments=" + java.util.Arrays.toString(arguments) + '}';
  }



}
