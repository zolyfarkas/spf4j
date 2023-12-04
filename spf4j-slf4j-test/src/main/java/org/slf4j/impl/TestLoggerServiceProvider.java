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
import org.slf4j.IMarkerFactory;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;
import org.slf4j.event.Level;
import org.slf4j.event.SubstituteLoggingEvent;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.helpers.SubstituteLogger;
import org.slf4j.helpers.SubstituteLoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;
import org.spf4j.base.ErrLog;

/**
 * The SPI implementation for the SLF4j 2.0 API.
 */
@SuppressFBWarnings("MS_SHOULD_BE_FINAL")
public final class TestLoggerServiceProvider implements SLF4JServiceProvider {

    private static final String LOGGER_FACTORY_CLASS_STR = TestLoggers.class.getName();

    /**
     * The ILoggerFactory instance returned by the {@link #getLoggerFactory}
     * method should always be the same object
     */
    private volatile ILoggerFactory loggerFactory;
    private volatile SubstituteLoggerFactory substitute;

    public TestLoggerServiceProvider() {
        this.substitute =  new SubstituteLoggerFactory();
        this.loggerFactory = this.substitute;
    }

    @Override
    public void initialize() {
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
            for (SubstituteLogger logger : this.substitute.getLoggers()) {
                logger.setDelegate(newLoggers.getLogger(logger.getName()));
            }
            this.drainMessagesFromSubstitute(newLoggers);
        } finally {
            currentThread.setContextClassLoader(contextClassLoader);
            this.substitute = null;
        }
    }

    @SuppressFBWarnings("CE_CLASS_ENVY")
    private void drainMessagesFromSubstitute(final TestLoggers newLoggers) {
        LinkedBlockingQueue<SubstituteLoggingEvent> eventQueue = this.substitute.getEventQueue();
        // drain the collected log events.
        for (SubstituteLoggingEvent event : eventQueue) {
            Throwable t = event.getThrowable();
            Level level = event.getLevel();
            LoggingEventBuilder builder = newLoggers.getLogger(event.getLoggerName())
                    .atLevel(level).setMessage(event.getMessage());
            for (Marker marker : event.getMarkers()) {
                builder = builder.addMarker(marker);
            }
            if (t != null) {
                builder = builder.setCause(t);
            }
            for (Object object : event.getArguments()) {
                builder = builder.addArgument(object);
            }
            for (KeyValuePair kv : event.getKeyValuePairs()) {
                builder = builder.addKeyValue(kv.key, kv.value);
            }
            builder.log();
        }
        eventQueue.clear();
    }

    private void drainMessagesFromSubstitute() {
        LinkedBlockingQueue<SubstituteLoggingEvent> eventQueue = this.substitute.getEventQueue();
        // drain the collected log events.
        for (SubstituteLoggingEvent event : eventQueue) {
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

    @Override
    public IMarkerFactory getMarkerFactory() {
        return new BasicMarkerFactory();
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return new NOPMDCAdapter();
    }

    @Override
    public String getRequestedApiVersion() {
        return "2.0.9";
    }

}
