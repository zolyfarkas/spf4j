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
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.Marker;
//CHECKSTYLE:OFF
import sun.misc.Contended;
//CHECKSTYLE:ON

/**
 * @author Zoltan Farkas
 */
@ThreadSafe
public final class TestLogger implements Logger {

  private final String name;

  private final Supplier<LogConfig> cfgSource;

  private final Map<Level, Supplier<LogConsumer>> consumers;


  private static final class LogConfigConsumer {

    private static final LogConfigConsumer NULL = new LogConfigConsumer(null, null);

    private final LogConfig cfg;

    private final LogConsumer consumer;

    LogConfigConsumer(final LogConfig cfg, final LogConsumer consumer) {
      this.cfg = cfg;
      this.consumer = consumer;
    }

  }

  @ThreadSafe
  private final class ConsumerSupplier implements Supplier<LogConsumer> {

    private final Level level;

    @Contended
    private volatile LogConfigConsumer cfgConsumer;

    ConsumerSupplier(final Level level) {
      this.level = level;
      this.cfgConsumer = LogConfigConsumer.NULL;
    }

    @Override
    @SuppressFBWarnings("NOS_NON_OWNED_SYNCHRONIZATION") //its a private class
    public LogConsumer get() {
      LogConfig nCfg = cfgSource.get();
      LogConfigConsumer curCfgConsumer = cfgConsumer;
      if (nCfg == curCfgConsumer.cfg) {
        return curCfgConsumer.consumer;
      } else {
        synchronized (this) {
          nCfg = cfgSource.get();
          curCfgConsumer = cfgConsumer;
          if (nCfg == curCfgConsumer.cfg) {
            return curCfgConsumer.consumer;
          } else {
            LogConsumer nCons = nCfg.getLogConsumer(name, level);
            cfgConsumer = new LogConfigConsumer(nCfg, nCons);
            return nCons;
          }
        }
      }
    }

  }

  public TestLogger(final String name, final Supplier<LogConfig> configSource) {
    this.name = name;
    this.cfgSource = configSource;
    this.consumers = new EnumMap<>(Level.class);
    for (Level level : Level.values()) {
      consumers.put(level, new ConsumerSupplier(level));
    }
  }

  @Override
  public String getName() {
    return name;
  }

  public void log(final Level level, final Marker marker, final String msg, final Object... args) {
    LogConsumer consumer = consumers.get(level).get();
    if (consumer != null) {
      consumer.accept(new LogRecord(this, level, marker, msg, args));
    }
  }

  @Override
  public boolean isTraceEnabled() {
    return consumers.get(Level.TRACE).get() != null;
  }

  @Override
  public void trace(final String msg) {
    log(Level.TRACE, null, msg);
  }


  @Override
  public void trace(final String format, final Object arg) {
    log(Level.TRACE, null, format, arg);
  }

  @Override
  public void trace(final String format, final Object arg1, final Object arg2) {
    log(Level.TRACE, null, format, arg1, arg2);
  }

  @Override
  public void trace(final String format, final Object... arguments) {
    log(Level.TRACE, null, format, arguments);
  }

  @Override
  public void trace(final String msg, final Throwable t) {
    log(Level.TRACE, null, msg, t);
  }

  @Override
  public boolean isTraceEnabled(final Marker marker) {
    return isTraceEnabled();
  }

  @Override
  public void trace(final Marker marker, final String msg) {
     log(Level.TRACE, marker, msg);
  }

  @Override
  public void trace(final Marker marker, final String format, final Object arg) {
     log(Level.TRACE, marker, format, arg);
  }

  @Override
  public void trace(final Marker marker, final String format, final Object arg1, final Object arg2) {
    log(Level.TRACE, marker, format, arg1, arg2);
  }

  @Override
  public void trace(final Marker marker, final String format, final Object... argArray) {
    log(Level.TRACE, marker, format, argArray);
  }

  @Override
  public void trace(final Marker marker, final String msg, final Throwable t) {
    log(Level.TRACE, marker, msg, t);
  }

  @Override
  public boolean isDebugEnabled() {
    return  consumers.get(Level.DEBUG).get() != null;
  }

  @Override
  public void debug(final String msg) {
    log(Level.DEBUG, null, msg);
  }

  @Override
  public void debug(final String format, final Object arg) {
    log(Level.DEBUG, null, format, arg);
  }

  @Override
  public void debug(final String format, final Object arg1, final Object arg2) {
    log(Level.DEBUG, null, format, arg1, arg2);
  }

  @Override
  public void debug(final String format, final Object... arguments) {
    log(Level.DEBUG, null, format, arguments);
  }

  @Override
  public void debug(final String msg, final Throwable t) {
    log(Level.DEBUG, null, msg, t);
  }

  @Override
  public boolean isDebugEnabled(final Marker marker) {
    return isDebugEnabled();
  }

  @Override
  public void debug(final Marker marker, final String msg) {
    log(Level.DEBUG, marker, msg);
  }

  @Override
  public void debug(final Marker marker, final String format, final Object arg) {
    log(Level.DEBUG, marker, format, arg);
  }

  @Override
  public void debug(final Marker marker, final String format, final Object arg1, final Object arg2) {
    log(Level.DEBUG, marker, format, arg1, arg2);
  }

  @Override
  public void debug(final Marker marker, final String format, final Object... arguments) {
    log(Level.DEBUG, marker, format, arguments);
  }

  @Override
  public void debug(final Marker marker, final String msg, final Throwable t) {
    log(Level.DEBUG, marker, msg, t);
  }

  @Override
  public boolean isInfoEnabled() {
    return consumers.get(Level.INFO).get() != null;
  }

  @Override
  public void info(final String msg) {
    log(Level.INFO, null, msg);
  }

  @Override
  public void info(final String format, final Object arg) {
    log(Level.INFO, null, format, arg);
  }

  @Override
  public void info(final String format, final Object arg1, final Object arg2) {
    log(Level.INFO, null, format, arg1, arg2);
  }

  @Override
  public void info(final String format, final Object... arguments) {
    log(Level.INFO, null, format, arguments);
  }

  @Override
  public void info(final String msg, final Throwable t) {
    log(Level.INFO, null, msg, t);
  }

  @Override
  public boolean isInfoEnabled(final Marker marker) {
    return isInfoEnabled();
  }

  @Override
  public void info(final Marker marker, final String msg) {
    log(Level.INFO, marker, msg);
  }

  @Override
  public void info(final Marker marker, final String format, final Object arg) {
    log(Level.INFO, marker, format, arg);
  }

  @Override
  public void info(final Marker marker, final String format, final Object arg1, final Object arg2) {
    log(Level.INFO, marker, format, arg1, arg2);
  }

  @Override
  public void info(final Marker marker, final String format, final Object... arguments) {
    log(Level.INFO, marker, format, arguments);
  }

  @Override
  public void info(final Marker marker, final String msg, final Throwable t) {
    log(Level.INFO, marker, msg, t);
  }

  @Override
  public boolean isWarnEnabled() {
    return consumers.get(Level.WARN).get() != null;
  }

  @Override
  public void warn(final String msg) {
    log(Level.WARN, null, msg);
  }

  @Override
  public void warn(final String format, final Object arg) {
    log(Level.WARN, null, format, arg);
  }

  @Override
  public void warn(final String format, final Object... arguments) {
    log(Level.WARN, null, format, arguments);
  }

  @Override
  public void warn(final String format, final Object arg1, final Object arg2) {
    log(Level.WARN, null, format, arg1, arg2);
  }

  @Override
  public void warn(final String msg, final Throwable t) {
    log(Level.WARN, null, msg, t);
  }

  @Override
  public boolean isWarnEnabled(final Marker marker) {
    return isWarnEnabled();
  }

  @Override
  public void warn(final Marker marker, final String msg) {
    log(Level.WARN, marker, msg);
  }

  @Override
  public void warn(final Marker marker, final String format, final Object arg) {
    log(Level.WARN, marker, format, arg);
  }

  @Override
  public void warn(final Marker marker, final String format, final Object arg1, final Object arg2) {
    log(Level.WARN, marker, format, arg1, arg2);
  }

  @Override
  public void warn(final Marker marker, final String format, final Object... arguments) {
    log(Level.WARN, marker, format, arguments);
  }

  @Override
  public void warn(final Marker marker, final String msg, final Throwable t) {
    log(Level.WARN, marker, msg, t);
  }

  @Override
  public boolean isErrorEnabled() {
    return consumers.get(Level.ERROR).get() != null;
  }

  @Override
  public void error(final String msg) {
    log(Level.ERROR, null, msg);
  }

  @Override
  public void error(final String format, final Object arg) {
    log(Level.ERROR, null, format, arg);
  }

  @Override
  public void error(final String format, final Object arg1, final Object arg2) {
    log(Level.ERROR, null, format, arg1, arg2);
  }

  @Override
  public void error(final String format, final Object... arguments) {
    log(Level.ERROR, null, format, arguments);
  }

  @Override
  public void error(final String msg, final Throwable t) {
    log(Level.ERROR, null, msg, t);
  }

  @Override
  public boolean isErrorEnabled(final Marker marker) {
    return isErrorEnabled();
  }

  @Override
  public void error(final Marker marker, final String msg) {
    log(Level.ERROR, marker, msg);
  }

  @Override
  public void error(final Marker marker, final String format, final Object arg) {
    log(Level.ERROR, marker, format, arg);
  }

  @Override
  public void error(final Marker marker, final String format, final Object arg1, final Object arg2) {
    log(Level.ERROR, marker, format, arg1, arg2);
  }

  @Override
  public void error(final Marker marker, final String format, final Object... arguments) {
    log(Level.ERROR, marker, format, arguments);
  }

  @Override
  public void error(final Marker marker, final String msg, final Throwable t) {
    log(Level.ERROR, marker, msg, t);
  }

  @Override
  public String toString() {
    return "TestLogger{" + "name=" + name + '}';
  }

}
