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
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import java.util.Iterator;
import javax.activation.MimeType;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.slf4j.Marker;
import org.spf4j.base.EscapeJsonStringAppendableWrapper;
import org.spf4j.base.Slf4jMessageFormatter;
import org.spf4j.base.Throwables;
import org.spf4j.io.ByteArrayBuilder;
import org.spf4j.io.ConfigurableAppenderSupplier;
import org.spf4j.io.ObjectAppender;
import org.spf4j.recyclable.impl.ArraySuppliers;
import org.spf4j.recyclable.impl.ThreadLocalRecyclingSupplier;

/**
 * A log handler that will print all logs that are not marked as printed above a log level.
 * It passes through all logs to downstream handlers.
 * Marks Log messages a PRINTED.
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
public final class LogPrinter implements LogHandler {

  public static final String PRINTED = "PRINTED";

  public static final String DO_NOT_PRINT = "DO_NOT_PRINT";

  private static final DateTimeFormatter FMT =
          TestUtils.isExecutedFromIDE() ? new DateTimeFormatterBuilder()
                .appendValue(HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2)
                .optionalStart()
                .appendLiteral(':')
                .appendValue(SECOND_OF_MINUTE, 2)
                .optionalStart()
                .appendFraction(ChronoField.MILLI_OF_SECOND, 3, 3, true)
                .toFormatter().withZone(ZoneId.systemDefault())
          : DateTimeFormatter.ISO_INSTANT;

  private static final ThreadLocalRecyclingSupplier<Buffer> TL_BUFFER =
          new ThreadLocalRecyclingSupplier<Buffer>(() -> new Buffer());

  private static final ConfigurableAppenderSupplier TO_STRINGER = new ConfigurableAppenderSupplier();

  private final Level minLogged;

  private static final class Buffer {

    private final ByteArrayBuilder bab;

    private final Writer writer;

    private final EscapeJsonStringAppendableWrapper writerEscaper;

    Buffer() {
      bab = new ByteArrayBuilder(512, ArraySuppliers.Bytes.JAVA_NEW);
      writer = new BufferedWriter(new OutputStreamWriter(bab, Charset.defaultCharset()));
      writerEscaper = new EscapeJsonStringAppendableWrapper(writer);
    }

    private void clear() {
      bab.reset();
    }

    private Writer getWriter() {
      return writer;
    }

    private EscapeJsonStringAppendableWrapper getWriterEscaper() {
      return writerEscaper;
    }

    private byte[] getBytes() throws IOException {
      writer.flush();
      return bab.getBuffer();
    }

    private int size() {
      return bab.size();
    }

  }

  public static ConfigurableAppenderSupplier getAppenderSupplier() {
    return TO_STRINGER;
  }

  LogPrinter(final Level minLogged) {
    this.minLogged = minLogged;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Handling handles(final Level level) {
    return level.ordinal() >= minLogged.ordinal() ? Handling.HANDLE_PASS : Handling.NONE;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressFBWarnings({ "CFS_CONFUSING_FUNCTION_SEMANTICS", "EXS_EXCEPTION_SOFTENING_NO_CHECKED" })
  @Override
  public LogRecord handle(final LogRecord record) {
    if (record.hasAttachment(PRINTED) || record.hasAttachment(DO_NOT_PRINT)) {
      return record;
    }
    Buffer buff = TL_BUFFER.get();
    try {
      buff.clear();
      print(record, buff.getWriter(), buff.getWriterEscaper(), "");
      if (record.getLevel() == Level.ERROR) {
        System.err.write(buff.getBytes(), 0, buff.size());
        System.err.flush();
      } else {
        System.out.write(buff.getBytes(), 0, buff.size());
        System.out.flush();
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    } finally {
      TL_BUFFER.recycle(buff);
    }
    record.attach(PRINTED);
    return record;
  }

  public static void printTo(final PrintStream stream, final LogRecord record, final String annotate) {
    Buffer buff = TL_BUFFER.get();
    buff.clear();
    try {
      print(record, buff.getWriter(), buff.getWriterEscaper(), annotate);
      stream.write(buff.getBytes(), 0, buff.size());
      stream.flush();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

   static void printMarker(final Marker marker, final Appendable wr,
          final EscapeJsonStringAppendableWrapper wrapper)
          throws IOException {
      if (marker.hasReferences()) {
        wr.append('{');
        wr.append('"');
        wrapper.append(marker.getName());
        wr.append("\":[");
        Iterator<Marker> it = marker.iterator();
        if (it.hasNext()) {
          printMarker(it.next(), wr, wrapper);
          while (it.hasNext()) {
            wr.append(',');
            printMarker(it.next(), wr, wrapper);
          }
        }
        wr.append("]}");
      } else {
        wr.append('"');
        wrapper.append(marker.getName());
        wr.append('"');
      }
   }


  static void print(final LogRecord record, final Appendable wr,
          final EscapeJsonStringAppendableWrapper wrapper, final String annotate)
          throws IOException {
    wr.append(annotate);
    FMT.formatTo(Instant.ofEpochMilli(record.getTimeStamp()), wr);
    wr.append(' ');
    String level = record.getLevel().toString();
    wr.append(level);
    wr.append(' ');
    Marker marker = record.getMarker();
    if (marker != null) {
      printMarker(marker, wr, wrapper);
      wr.append(' ');
    }
    Throwables.writeAbreviatedClassName(record.getLogger().getName(), wr);
    wr.append(" \"");
    wrapper.append(record.getThread().getName());
    wr.append("\" \"");
    Object[] arguments = record.getArguments();
    int i = Slf4jMessageFormatter.format(LogPrinter::exHandle, 0, wrapper, record.getFormat(),
            TO_STRINGER, arguments);
    wr.append("\" ");
    Throwable t = null;
    if (i < arguments.length) {
      boolean first = true;
      for (; i < arguments.length; i++) {
        Object arg = arguments[i];
        if (arg instanceof Throwable) {
          if (t == null) {
            t = (Throwable) arg;
          } else {
            t.addSuppressed(t); // not ideal
          }
        } else {
          if (!first) {
            wr.append(", ");
          } else {
            wr.append('[');
            first = false;
          }
          printObject(arg, wr, wrapper);
        }
      }
      if (!first) {
        wr.append(']');
      }
    }
    if (t != null) {
      wr.append('\n');
      Throwables.writeTo(t, wr, Throwables.PackageDetail.SHORT);
    }
    wr.append('\n');
  }

  private static void printObject(@Nullable final Object obj,
          final Appendable wr, final EscapeJsonStringAppendableWrapper wrapper) throws IOException {
    if (obj == null) {
      wr.append("null");
    } else {
      ObjectAppender ostrApp = TO_STRINGER.get(obj.getClass());
      MimeType type = ostrApp.getAppendedType();
      if ("json".equalsIgnoreCase(type.getSubType())) {
        ostrApp.append(obj, wr);
      } else {
        wr.append('"');
        ostrApp.append(obj, wrapper);
        wr.append('"');
      }
    }
  }

  static void exHandle(final Object obj, final Appendable sbuf, final Throwable t) throws IOException {
    String className = obj.getClass().getName();
    sbuf.append("[FAILED toString() for ");
    sbuf.append(className);
    sbuf.append("]{");
    Throwables.writeTo(t, sbuf, Throwables.PackageDetail.SHORT);
    sbuf.append('}');
  }

  @Override
  public String toString() {
    return "LogPrinter{" + "minLogged=" + minLogged + '}';
  }

}
