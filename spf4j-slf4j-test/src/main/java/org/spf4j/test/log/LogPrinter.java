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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.log.Level;

/**
 * A log handler that will print all logs that are not marked as printed above a log level.
 * It passes through all logs to downstream handlers.
 * Marks Log messages a PRINTED.
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
@SuppressFBWarnings("FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY") // this is with LogRecord...
public final class LogPrinter implements LogHandler {

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

  public static final org.spf4j.log.LogPrinter PRINTER = new org.spf4j.log.LogPrinter(FMT, Charset.defaultCharset());

  private final Level minLogged;


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
  public TestLogRecord handle(final TestLogRecord record) {
    if (record.hasAttachment(Attachments.PRINTED) || record.hasAttachment(Attachments.DO_NOT_PRINT)) {
      return record;
    }
    try {
      PRINTER.print(record, System.out, System.err).flush();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    record.attach(Attachments.PRINTED);
    return record;
  }



  @Override
  public String toString() {
    return "LogPrinter{" + "minLogged=" + minLogged + '}';
  }

}
