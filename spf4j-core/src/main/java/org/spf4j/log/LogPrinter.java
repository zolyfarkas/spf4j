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
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
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
@SuppressFBWarnings("FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY") // this is with LogRecord...
public final class LogPrinter {

  private static final ThreadLocalRecyclingSupplier<Buffer> TL_BUFFER =
          new ThreadLocalRecyclingSupplier<Buffer>(() -> new Buffer());

  private static final ConfigurableAppenderSupplier TO_STRINGER = new ConfigurableAppenderSupplier();

  private final DateTimeFormatter fmt;

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

  public LogPrinter() {
    this(DateTimeFormatter.ISO_INSTANT);
  }

  public LogPrinter(final DateTimeFormatter fmt) {
    this.fmt = fmt;
  }


  public OutputStream print(final Slf4jLogRecord record, final OutputStream os, final OutputStream errStream) {
    if (record.getLevel() == Level.ERROR) {
      print(record, errStream);
      return errStream;
    } else {
      print(record, os);
      return os;
    }
  }


  public void print(final Slf4jLogRecord record, final OutputStream os) {
    Buffer buff = TL_BUFFER.get();
    try {
      buff.clear();
      print(record, buff.getWriter(), buff.getWriterEscaper(), "");
      os.write(buff.getBytes(), 0, buff.size());
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    } finally {
      TL_BUFFER.recycle(buff);
    }
  }

  public void printTo(final Appendable stream, final Slf4jLogRecord record, final String annotate) {
    try {
      print(record, stream, new EscapeJsonStringAppendableWrapper(stream), annotate);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public void printTo(final PrintStream stream, final Slf4jLogRecord record, final String annotate) {
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


   private void print(final Slf4jLogRecord record, final Appendable wr,
          final EscapeJsonStringAppendableWrapper wrapper, final String annotate)
          throws IOException {
    wr.append(annotate);
    fmt.formatTo(Instant.ofEpochMilli(record.getTimeStamp()), wr);
    wr.append(' ');
    String level = record.getLevel().toString();
    wr.append(level);
    wr.append(' ');
    Marker marker = record.getMarker();
    if (marker != null) {
      printMarker(marker, wr, wrapper);
      wr.append(' ');
    }
    Throwables.writeAbreviatedClassName(record.getLoggerName(), wr);
    wr.append(" \"");
    wrapper.append(record.getThreadName());
    wr.append("\" \"");
    Object[] arguments = record.getArguments();
    int i = Slf4jMessageFormatter.format(LogPrinter::exHandle, 0, wrapper, record.getMessageFormat(),
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
            t.addSuppressed((Throwable) arg); // not ideal
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
      Throwables.writeTo(t, wr, Throwables.PackageDetail.SHORT, "");
    } else {
      wr.append('\n');
    }
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
    return "LogPrinter{" + "fmt=" + fmt + '}';
  }

}
