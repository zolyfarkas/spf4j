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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Marker;
import org.spf4j.base.CoreTextMediaType;
import org.spf4j.base.EscapeJsonStringAppendableWrapper;
import org.spf4j.base.Slf4jMessageFormatter;
import org.spf4j.base.Throwables;
import org.spf4j.base.avro.AThrowables;
import org.spf4j.base.avro.LogRecord;
import org.spf4j.io.ByteArrayBuilder;
import org.spf4j.io.ConfigurableAppenderSupplier;
import org.spf4j.io.ObjectAppender;
import org.spf4j.recyclable.impl.ArraySuppliers;
import org.spf4j.recyclable.impl.ThreadLocalRecyclingSupplier;

/**
 * A log printer. The format is not configurable, and this is intentional.
 * create One instance of this printer and re-use it.
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
@ThreadSafe
public final class LogPrinter {

  private static final ConcurrentMap<Charset, ThreadLocalRecyclingSupplier<Buffer>>
          BUFFERS = new ConcurrentHashMap<>();

  private final ThreadLocalRecyclingSupplier<Buffer> tlBuffer;

  private final ConfigurableAppenderSupplier toStringer;

  private final DateTimeFormatter fmt;

  interface BufferedAppendable {

    Appendable getAppendable();

    Appendable getJsonStringEscapingAppendable();

    int getCurrentPos();

    void resetPos(int pos);

    static BufferedAppendable from(final StringBuilder sb) {

      return new BufferedAppendable() {

        private Appendable escaper = null;

        @Override
        public Appendable getAppendable() {
          return sb;
        }

        @Override
        public Appendable getJsonStringEscapingAppendable() {
          if (escaper == null) {
            escaper = new EscapeJsonStringAppendableWrapper(sb);
          }
          return escaper;
        }

        @Override
        public int getCurrentPos() {
          return sb.length();
        }

        @Override
        public void resetPos(final int pos) {
          sb.setLength(pos);
        }
      };
    }

  }

  private static final class Buffer implements BufferedAppendable {

    private static final int MAX_BUFFER_SIZE = Integer.getInteger("spf4j.logPrinter", 1024 * 32);

    private final ByteArrayBuilder bab;

    private final Writer writer;

    private final EscapeJsonStringAppendableWrapper writerEscaper;

    Buffer(final Charset charset) {
      bab = new ByteArrayBuilder(512, ArraySuppliers.Bytes.JAVA_NEW);
      writer = new BufferedWriter(new OutputStreamWriter(bab, charset));
      writerEscaper = new EscapeJsonStringAppendableWrapper(writer);
    }

    private void clear() {
      try {
        writer.flush();
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
      bab.reset();
    }

    public Appendable getAppendable() {
      return writer;
    }

    public Appendable getJsonStringEscapingAppendable() {
      return writerEscaper;
    }

    private void flush() {
      try {
        writer.flush();
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }

    private byte[] getBytes() {
      return bab.getBuffer();
    }

    private int size() {
      return bab.size();
    }

    @Override
    public int getCurrentPos() {
      flush();
      return bab.size();
    }

    @Override
    @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED") //on purpose.
    public void resetPos(final int pos) {
      flush();
      bab.resetCountTo(pos);
    }

  }

  public ConfigurableAppenderSupplier getAppenderSupplier() {
    return toStringer;
  }

  public LogPrinter() {
    this(DateTimeFormatter.ISO_INSTANT, Charset.defaultCharset());
  }

  public LogPrinter(final Charset charset) {
    this(DateTimeFormatter.ISO_INSTANT, charset);
  }

  public LogPrinter(final DateTimeFormatter fmt, final Charset charset) {
    this.fmt = fmt;
    this.toStringer = new ConfigurableAppenderSupplier();
    tlBuffer =  BUFFERS.computeIfAbsent(charset,
            (cs) -> new ThreadLocalRecyclingSupplier<Buffer>(() -> new Buffer(cs)));
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
    Buffer buff = tlBuffer.get();
    boolean recycle = true;
    try {
      buff.clear();
      print(record, buff, "");
      buff.flush();
      int len = buff.size();
      os.write(buff.getBytes(), 0, len);
      if (len > Buffer.MAX_BUFFER_SIZE) {
        recycle = false;
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    } finally {
      if (recycle) {
        tlBuffer.recycle(buff);
      }
    }
  }

  public byte[] printToBytes(final Slf4jLogRecord record) {
    Buffer buff = tlBuffer.get();
    boolean recycle = true;
    try {
      buff.clear();
      print(record, buff, "");
      buff.flush();
      int size = buff.size();
      if (size > Buffer.MAX_BUFFER_SIZE) {
        recycle = false;
      }
      return Arrays.copyOf(buff.getBytes(), size);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    } finally {
      if (recycle) {
        tlBuffer.recycle(buff);
      }
    }
  }

  public void print(final LogRecord record, final OutputStream os) throws IOException {
    printTo(os, record, "");
    os.flush();
  }

  public void printTo(final StringBuilder sb, final Slf4jLogRecord record, final String annotate) {
    try {
      print(record, BufferedAppendable.from(sb), annotate);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public void printTo(final OutputStream stream, final LogRecord record, final String annotate) {
    Buffer buff = tlBuffer.get();
    buff.clear();
    try {
      print(record, buff, annotate);
      buff.flush();
      stream.write(buff.getBytes(), 0, buff.size());
      stream.flush();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public void printTo(final PrintStream stream, final Slf4jLogRecord record, final String annotate) {
    Buffer buff = tlBuffer.get();
    buff.clear();
    try {
      print(record, buff, annotate);
      buff.flush();
      stream.write(buff.getBytes(), 0, buff.size());
      stream.flush();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  static void printMarker(final Marker marker, final Appendable wr,
          final Appendable wrapper)
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


   private void print(final Slf4jLogRecord record, final BufferedAppendable app, final String annotate)
          throws IOException {
    Appendable wr = app.getAppendable();
    Appendable wrapper = app.getJsonStringEscapingAppendable();
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
            toStringer, arguments);
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
          printJsonObject(arg, app);
        }
      }
      if (!first) {
        wr.append(']');
      }
    }
    if (t != null) {
      wr.append('\n');
      Throwables.writeTo(t, wr, Throwables.PackageDetail.SHORT);
    } else {
      wr.append('\n');
    }
  }

   private void print(final LogRecord record, final BufferedAppendable ba, final String annotate)
          throws IOException {
    Appendable wr = ba.getAppendable();
    Appendable wrapper = ba.getJsonStringEscapingAppendable();
    wr.append(annotate);
    wr.append('"');
    wrapper.append(record.getOrigin());
    wr.append("\" ");
    fmt.formatTo(record.getTs(), wr);
    wr.append(' ');
    String level = record.getLevel().toString();
    wr.append(level);
    wr.append(' ');
    wr.append(record.getLogger());
    wr.append(" \"");
    wrapper.append(record.getThr());
    wrapper.append(':');
    wrapper.append(record.getTrId());
    wr.append("\" \"");
    wrapper.append(record.getMsg());
    wr.append("\" ");
    Map<String, Object> attrs = record.getAttrs();
    List<Object> xtra = record.getXtra();
    if (attrs.size() + xtra.size() > 0) {
      boolean first = true;
      wr.append('[');
      for (Map.Entry<String, Object> entry : attrs.entrySet()) {
        if (first) {
          first = false;
        } else {
          wr.append(',');
        }
        printJsonObject(entry, ba);
      }
      for (Object obj : xtra) {
        if (first) {
          first = false;
        } else {
          wr.append(',');
        }
        printJsonObject(obj, ba);
      }
      wr.append(']');
    }
    org.spf4j.base.avro.Throwable t = record.getThrowable();
    if (t != null) {
      wr.append('\n');
      AThrowables.writeTo(t, wr, Throwables.PackageDetail.SHORT, true, "");
    } else {
      wr.append('\n');
    }
  }

  /**
   * Function that will write the Object as a json representation.
   * If json appender not available a json string value will be written.
   * @param obj
   * @param wr
   * @param wrapper
   * @throws IOException
   */
  private void printJsonObject(@Nullable final Object obj,
          final BufferedAppendable app) throws IOException {
    if (obj == null) {
      app.getAppendable().append("null");
    } else {
      ObjectAppender ostrApp = toStringer.get(CoreTextMediaType.APPLICATION_JSON, obj.getClass());
      if (ostrApp != null) {
        int currentPos = app.getCurrentPos();
        try {
          ostrApp.append(obj, app.getAppendable(), toStringer);
          return;
        } catch (IOException | RuntimeException e) {
          app.resetPos(currentPos);
        }
      }
      Appendable wr = app.getAppendable();
      Appendable wrapper = app.getJsonStringEscapingAppendable();
      ostrApp = toStringer.get(CoreTextMediaType.TEXT_PLAIN, obj.getClass());
      wr.append('"');
      int currentPos = app.getCurrentPos();
      try {
        ostrApp.append(obj, wrapper, toStringer);
      }  catch (IOException | RuntimeException e) {
        app.resetPos(currentPos);
        exHandle(obj, wrapper, e);
      }
      wr.append('"');
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
    return "LogPrinter{" + "toStringer=" + toStringer + ", fmt=" + fmt + '}';
  }

}
