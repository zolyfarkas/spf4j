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
package org.spf4j.log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Marker;
import org.spf4j.base.Arrays;
import org.spf4j.base.JsonWriteable;
import org.spf4j.base.Slf4jMessageFormatter;
import org.spf4j.base.Throwables;
import org.spf4j.io.AppendableWriter;
import org.spf4j.io.ObjectAppenderSupplier;

/**
 * @author Zoltan Farkas
 */

@SuppressFBWarnings("LO_SUSPECT_LOG_PARAMETER")
@ParametersAreNonnullByDefault
@ThreadSafe
public class Slf4jLogRecordImpl implements JsonWriteable, Slf4jLogRecord {

  private final String threadName;
  private final String loggerName;
  private final Level level;
  private final long timeStamp;
  private final Marker marker;
  private final String messageFormat;
  private final Object[] arguments;
  private volatile int startExtra;
  @Nullable
  private volatile String message;
  private volatile boolean isLogged;
  private Set<Object> attachments;


  public Slf4jLogRecordImpl(final String logger, final Level level,
          final String format, final Object... arguments) {
    this(logger, level, null, format, arguments);
  }

  public Slf4jLogRecordImpl(final String logger, final Level level,
          @Nullable final Marker marker, final String format, final Object... arguments) {
   this(false, logger, level, marker, System.currentTimeMillis(), format, arguments);
  }

  public Slf4jLogRecordImpl(final boolean  isLogged, final String logger, final Level level,
          @Nullable final Marker marker, final String format, final Object... arguments) {
   this(isLogged, logger, level, marker, System.currentTimeMillis(), format, arguments);
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public Slf4jLogRecordImpl(final boolean isLogged, final String logger, final Level level,
          @Nullable final Marker marker,  final long timestampMillis,
          final String format, final Object... arguments) {
    this.loggerName = logger;
    this.level = level;
    this.timeStamp = timestampMillis;
    this.marker = marker;
    this.messageFormat = format;
    this.arguments = arguments;
    this.threadName = Thread.currentThread().getName();
    this.startExtra = -1;
    this.message = null;
    this.isLogged = isLogged;
    this.attachments = Collections.EMPTY_SET;
  }

  @Override
  public final String getLoggerName() {
    return loggerName;
  }

  @Override
  public final Level getLevel() {
    return level;
  }

  @Override
  public final long getTimeStamp() {
    return timeStamp;
  }

  @Nullable
  @Override
  public final Marker getMarker() {
    return marker;
  }

  @Override
  public final String getMessageFormat() {
    return messageFormat;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP") // risk I take...
  @Nonnull
  @Override
  public final Object[] getArguments() {
    return arguments;
  }

  @Override
  public final int getNrMessageArguments() {
    int sx = this.startExtra;
    if (sx < 0) {
        sx = Slf4jMessageFormatter.getFormatParameterNumber(messageFormat);
      this.startExtra = sx;
    }
    return sx;
  }

  @Override
  public final String getThreadName() {
    return threadName;
  }

  @Nonnull
  @Override
  public final String getMessage() {
    materializeMessage();
    return message;
  }

  private void materializeMessage() {
    if (message == null) {
      synchronized (messageFormat) {
        if (message == null) {
          StringBuilder sb = new StringBuilder(messageFormat.length() + arguments.length * 8);
          try {
            this.startExtra = Slf4jMessageFormatter.format(0, sb, messageFormat,
                    ObjectAppenderSupplier.TO_STRINGER, arguments);
          } catch (IOException ex) {
            throw new UncheckedIOException(ex);
          }
          message = sb.toString();
        }
      }
    }
  }

  @Nonnull
  @Override
  public final Object[] getExtraArgumentsRaw() {
    int sx = getNrMessageArguments();
    if (sx < arguments.length) {
        return java.util.Arrays.copyOfRange(arguments, sx, arguments.length);
    } else {
      return Arrays.EMPTY_OBJ_ARRAY;
    }
  }

  @Nonnull
  @Override
  public final Object[] getExtraArguments() {
    int sx = getNrMessageArguments();
    if (sx < arguments.length) {
      int nrExtraThrowables = getNrExtraThrowables();
      if (nrExtraThrowables <= 0) {
        return java.util.Arrays.copyOfRange(arguments, sx, arguments.length);
      } else {
        Object[] result = new Object[arguments.length - sx - nrExtraThrowables];
        int i = 0;
        for (int j = sx; j < arguments.length; j++) {
          Object argument = arguments[j];
          if (!(argument instanceof Throwable)) {
            result[i++]  =  argument;
          }
        }
        return result;
      }
    } else {
      return Arrays.EMPTY_OBJ_ARRAY;
    }
  }

  private  int getNrExtraThrowables() {
    int sx = getNrMessageArguments();
    int count = 0;
    for (int i = sx; i < arguments.length; i++) {
      Object argument = arguments[i];
      if (argument instanceof Throwable) {
        count++;
      }
    }
    return count;
  }

  @Nullable
  @Override
  public final Throwable getExtraThrowable() {
    int sx = getNrMessageArguments();
    Throwable result = null;
    for (int i = sx; i < arguments.length; i++) {
      Object argument = arguments[i];
      if (argument instanceof Throwable) {
        if (result == null) {
          result = (Throwable) argument;
        } else {
          Throwables.suppressLimited(result, (Throwable) argument);
        }
      }
    }
    return result;
  }

  /**
   * can be sub-classed to change the string representation.
   * @return
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(64);
    writeTo(sb);
    return sb.toString();
  }

  /**
   * can be sub-classed to change the json representation.
   * @return
   */
  @Override
  public void writeJsonTo(final Appendable appendable) throws IOException {
    JsonGenerator gen = Lazy.JSON.createGenerator(new AppendableWriter(appendable));
    gen.setCodec(Lazy.MAPPER);
    gen.writeStartObject();
    gen.writeFieldName("ts");
    gen.writeString(Instant.ofEpochMilli(timeStamp).toString());
    gen.writeFieldName("logger");
    gen.writeString(loggerName);
    gen.writeFieldName("thread");
    gen.writeString(threadName);
    gen.writeFieldName("msg");
    gen.writeString(getMessage());
    Object[] extraArguments = getExtraArguments();
    if (extraArguments.length > 0) {
      gen.writeFieldName("xObj");
      gen.writeStartArray();
      for (Object  obj : extraArguments) {
        gen.writeObject(obj);
      }
      gen.writeEndArray();
    }
    Throwable t = getExtraThrowable();
    if (t != null) {
      gen.writeFieldName("throwable");
      gen.writeString(Throwables.toString(t));
    }
    gen.writeEndObject();
    gen.flush();
  }

  @Override
  public final boolean isLogged() {
    return isLogged;
  }

  @Override
  public final void setIsLogged() {
    isLogged =  true;
  }

  @Override
  public final synchronized void attach(final Object obj) {
    if (attachments.isEmpty()) {
      attachments = new HashSet<>(2);
    }
    attachments.add(obj);
  }

  @Override
  public final synchronized boolean hasAttachment(final Object obj) {
    return attachments.contains(obj);
  }

  @Override
  public final synchronized Set<Object> getAttachments() {
    return attachments.isEmpty() ? attachments : Collections.unmodifiableSet(attachments);
  }

  private static final class Lazy {

    private static final JsonFactory JSON = new JsonFactory();

    private static final ObjectMapper MAPPER = new ObjectMapper(JSON);
  }


}
