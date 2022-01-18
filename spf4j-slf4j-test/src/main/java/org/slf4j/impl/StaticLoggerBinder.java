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
package org.slf4j.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.LinkedBlockingQueue;
import org.spf4j.test.log.TestLoggers;
import org.slf4j.ILoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.event.SubstituteLoggingEvent;
import org.slf4j.helpers.SubstituteLogger;
import org.slf4j.helpers.SubstituteLoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;
import org.spf4j.base.ErrLog;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("MS_SHOULD_BE_FINAL")
public final class StaticLoggerBinder implements LoggerFactoryBinder {

    private static final SubstituteLoggerFactory SUBSTITUTE = new SubstituteLoggerFactory();
    /**
     * The unique instance of this class.
     *
     */
    private static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();


    private static final String LOGGER_FACTORY_CLASS_STR = TestLoggers.class.getName();

    static {
      SINGLETON.init();
    }
    /**
     * The ILoggerFactory instance returned by the {@link #getLoggerFactory}
     * method should always be the same object
     */
    private volatile ILoggerFactory loggerFactory;

    private StaticLoggerBinder() {
      this.loggerFactory = SUBSTITUTE;
    }

    private void init() {
      Thread currentThread = Thread.currentThread();
      ClassLoader contextClassLoader = currentThread.getContextClassLoader();
      currentThread.setContextClassLoader(ClassLoader.getSystemClassLoader());
      try {
        TestLoggers newLoggers;
        try {
          newLoggers = TestLoggers.sys();
        } catch (RuntimeException | Error e) {
          drainMessagesFromSubstitute();
          ErrLog.error("Failed to initialize test logger", e);
          throw e;
        }
        this.loggerFactory = newLoggers;
        // init complete, now delegate to test loggers.
        for (SubstituteLogger logger : SUBSTITUTE.getLoggers()) {
          logger.setDelegate(newLoggers.getLogger(logger.getName()));
        }
        drainMessagesFromSubstitute(newLoggers);
      } finally {
         currentThread.setContextClassLoader(contextClassLoader);
      }
    }

  @SuppressFBWarnings("CE_CLASS_ENVY")
  private static void drainMessagesFromSubstitute(final TestLoggers newLoggers)  {
    LinkedBlockingQueue<SubstituteLoggingEvent> eventQueue = SUBSTITUTE.getEventQueue();
    // drain the collected log events.
    for (SubstituteLoggingEvent event: eventQueue) {
      Throwable t = event.getThrowable();
      Level level = event.getLevel();
      switch (level) {
        case TRACE:
          if (t == null) {
            newLoggers.getLogger(event.getLoggerName()).trace(event.getMarker(), event.getMessage(),
                    event.getArgumentArray());
          } else {
            newLoggers.getLogger(event.getLoggerName()).trace(event.getMarker(), event.getMessage(),
                    org.spf4j.base.Arrays.append(event.getArgumentArray(), t));
          }
          break;
        case DEBUG:
          if (t == null) {
            newLoggers.getLogger(event.getLoggerName()).debug(event.getMarker(), event.getMessage(),
                    event.getArgumentArray());
          } else {
            newLoggers.getLogger(event.getLoggerName()).debug(event.getMarker(), event.getMessage(),
                    org.spf4j.base.Arrays.append(event.getArgumentArray(), t));
          }
          break;
        case INFO:
          if (t == null) {
            newLoggers.getLogger(event.getLoggerName()).info(event.getMarker(), event.getMessage(),
                    event.getArgumentArray());
          } else {
            newLoggers.getLogger(event.getLoggerName()).info(event.getMarker(), event.getMessage(),
                    org.spf4j.base.Arrays.append(event.getArgumentArray(), t));
          }
          break;
        case WARN:
          if (t == null) {
            newLoggers.getLogger(event.getLoggerName()).warn(event.getMarker(), event.getMessage(),
                    event.getArgumentArray());
          } else {
            newLoggers.getLogger(event.getLoggerName()).warn(event.getMarker(), event.getMessage(),
                    org.spf4j.base.Arrays.append(event.getArgumentArray(), t));
          }
          break;
        case ERROR:
          if (t == null) {
            newLoggers.getLogger(event.getLoggerName()).error(event.getMarker(), event.getMessage(),
                    event.getArgumentArray());
          } else {
            newLoggers.getLogger(event.getLoggerName()).error(event.getMarker(), event.getMessage(),
                    org.spf4j.base.Arrays.append(event.getArgumentArray(), t));
          }
          break;
        default:
          throw new UnsupportedOperationException("Unsupported log level " + level);
      }
    }
    eventQueue.clear();
  }

  private static void drainMessagesFromSubstitute()  {
      LinkedBlockingQueue<SubstituteLoggingEvent> eventQueue = SUBSTITUTE.getEventQueue();
      // drain the collected log events.
      for (SubstituteLoggingEvent event: eventQueue) {
        Throwable t = event.getThrowable();
        Level level = event.getLevel();
        if (t == null) {
          ErrLog.error(event.getMessage(), event.getArgumentArray(), event.getLoggerName(), level);
        } else {
          ErrLog.error(event.getMessage(), event.getArgumentArray(), event.getLoggerName(), level, t);
        }
        break;
      }
      eventQueue.clear();
    }

    /**
     * Return the singleton of this class.
     *
     * @return the StaticLoggerBinder singleton
     */
    public static StaticLoggerBinder getSingleton() {
        return SINGLETON;
    }

    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    public String getLoggerFactoryClassStr() {
        return LOGGER_FACTORY_CLASS_STR;
    }

  @Override
  public String toString() {
    return "StaticLoggerBinder{" + "loggerFactory=" + loggerFactory + '}';
  }

}

