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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.ThreadSafe;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
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
@ParametersAreNonnullByDefault
@ThreadSafe
public final class LogRecord implements JsonWriteable {


  private final String threadName;
  private final String loggerName;
  private final Level level;
  private final long timeStamp;
  private final Marker marker;
  private final String format;
  private final Object[] arguments;
  private int startExtra;
  @Nullable
  private String message;

  @SuppressFBWarnings("LO_SUSPECT_LOG_PARAMETER")
  public LogRecord(final String logger, final Level level,
          final String format, final Object... arguments) {
    this(logger, level, null, format, arguments);
  }

  @SuppressFBWarnings("LO_SUSPECT_LOG_PARAMETER")
  public LogRecord(final String logger, final Level level,
          @Nullable final Marker marker, final String format, final Object... arguments) {
    this.loggerName = logger;
    this.level = level;
    this.timeStamp = System.currentTimeMillis();
    this.marker = marker;
    this.format = format;
    this.arguments = arguments;
    this.threadName = Thread.currentThread().getName();
    this.startExtra = -1;
    this.message = null;
  }

  public String getLoggerName() {
    return loggerName;
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
  @Nonnull
  public Object[] getArguments() {
    return arguments;
  }

  public String getThreadName() {
    return threadName;
  }

  @Nonnull
  public synchronized String getMessage() {
    materializeMessage();
    return message;
  }

  private synchronized void materializeMessage() {
    if (message == null) {
      StringBuilder sb = new StringBuilder(format.length() + arguments.length * 8);
      try {
        this.startExtra = Slf4jMessageFormatter.format(0, sb, format,
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

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(64);
    writeTo(sb);
    return sb.toString();
  }

  @Override
  public void writeTo(final Appendable appendable) throws IOException {
    JsonGenerator gen = Lazy.JSON.createJsonGenerator(new AppendableWriter(appendable));
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

  private static final class Lazy {

    private static final JsonFactory JSON = new JsonFactory();

    private static final ObjectMapper MAPPER = new ObjectMapper(JSON);
  }


}
