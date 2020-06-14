/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Additionally licensed with:
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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;
import org.spf4j.base.Arrays;
import org.spf4j.base.Pair;
import org.spf4j.base.avro.Method;
import org.spf4j.text.MessageFormat;

/**
 * <p>
 * Bridge/route all JUL log records to the SLF4J API.</p>
 * <p>
 * Implementation based on jul-to-slf4j bridge but at least 20% faster. Speed improvements come from using spf4j
 * improved MessageFormatter + code cleanup. Also unlike jul-to-slf4j the sorce class and source method information is
 * not being dropped. </p>
 * <p>
 * Implementation is interchangeable with the jul-to-slf4j implementation </p>
 *
 */
public final class SLF4JBridgeHandler extends Handler {

  private static final String FQCN = java.util.logging.Logger.class.getName();
  private static final String UNKNOWN_LOGGER_NAME = "unknown.jul.logger";

  private static final int TRACE_LEVEL_THRESHOLD = Level.FINEST.intValue();
  private static final int DEBUG_LEVEL_THRESHOLD = Level.FINE.intValue();
  private static final int INFO_LEVEL_THRESHOLD = Level.INFO.intValue();
  private static final int WARN_LEVEL_THRESHOLD = Level.WARNING.intValue();

  private static final ThreadLocal<Reference<StringBuilder>> SB = new
          ThreadLocal<Reference<StringBuilder>>() {
    @Override
    protected Reference<StringBuilder> initialValue() {
      return new SoftReference<>(new StringBuilder(64));
    }

  };

  private static final MessageFormat INVALID_FORMAT = new MessageFormat("SPF4J Invalid Message Format");

  private static class Lazy {
    private static final LoadingCache<Locale, LoadingCache<String, MessageFormat>> LOCALIZED_FORMAT_CACHE
          = CacheBuilder.newBuilder()
                  .build(new CacheLoader<Locale, LoadingCache<String, MessageFormat>>() {
                    @Override
                    public LoadingCache<String, MessageFormat> load(final Locale locale) throws Exception {
                      return CacheBuilder.newBuilder()
                              .maximumSize(Integer.getInteger("spf4j.julBridge.MaxFormatCacheSize",
                                      1024)).build(new CacheLoader<String, MessageFormat>() {
                                @Override
                                public MessageFormat load(final String key) {
                                  try {
                                    return new MessageFormat(key, locale);
                                  } catch (IllegalArgumentException ex) {
                                    // certain loggers extend LogMessage with printf syntax...
                                    LoggerFactory.getLogger(Lazy.class).trace("Unable to forrmat {}", key, ex);
                                    return INVALID_FORMAT;
                                  }
                                }
                              });
                    }
                  }
                  );
  }

  private static final LoadingCache<String, MessageFormat> FORMAT_CACHE
          = CacheBuilder.newBuilder()
                  .maximumSize(Integer.getInteger("spf4j.julBridge.MaxFormatCacheSize",
                  1024)).build(new CacheLoader<String, MessageFormat>() {
            @Override
            public MessageFormat load(final String key) {
              try {
                return new MessageFormat(key);
              } catch (IllegalArgumentException ex) {
                // certain loggers extend LogMessage with printf syntax...
                LoggerFactory.getLogger(SLF4JBridgeHandler.class).trace("Unable to forrmat {}", key, ex);
                return INVALID_FORMAT;
              }
            }
          });

  private static final boolean ALWAYS_TRY_INFER = Boolean.getBoolean("spf4j.jul2slf4jBridge.alwaysTryInferSource");

  /**
   * Need this to avoid source class and method inference. (which is expensive)
   * @see https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8244321
   */
  @Nullable
  private static final Field NEED_INFER;

  static {
    NEED_INFER = AccessController.doPrivileged(new PrivilegedAction<Field>() {
      @Override
      public Field run() {
        try {
          Field declaredField = LogRecord.class.getDeclaredField("needToInferCaller");
          declaredField.setAccessible(true);
          return declaredField;
        } catch (NoSuchFieldException | SecurityException ex) {
          LoggerFactory.getLogger(SLF4JBridgeHandler.class)
                  .debug("jul to slf4j bridge will not differentiate between computed caller info and provided", ex);
          return null;
        }
      }
    });
  }

  /**
   * Adds a SLF4JBridgeHandler instance to jul's root logger.
   *
   * This handler will redirect j.u.l. logging to SLF4J. However, only logs enabled in j.u.l. will be redirected. For
   * example, if a log statement invoking a j.u.l. logger is disabled, then the corresponding non-event will
   * <em>not</em>
   * reach SLF4JBridgeHandler and cannot be redirected.
   */
  public static void install() {
    getRootLogger().addHandler(new SLF4JBridgeHandler());
  }

  private static java.util.logging.Logger getRootLogger() {
    return LogManager.getLogManager().getLogger("");
  }

  /**
   * Removes previously installed SLF4JBridgeHandler instances. See also {@link #install()}.
   *
   * @throws SecurityException A <code>SecurityException</code> is thrown, if a security manager exists and if the
   * caller does not have LoggingPermission("control").
   */
  public static void uninstall() {
    java.util.logging.Logger rootLogger = getRootLogger();
    Handler[] handlers = rootLogger.getHandlers();
    for (int i = 0; i < handlers.length; i++) {
      if (handlers[i] instanceof SLF4JBridgeHandler) {
        rootLogger.removeHandler(handlers[i]);
      }
    }
  }

  /**
   * Returns true if SLF4JBridgeHandler has been previously installed, returns false otherwise.
   *
   * @return true if SLF4JBridgeHandler is already installed, false other wise
   * @throws SecurityException
   */
  public static boolean isInstalled() {
    java.util.logging.Logger rootLogger = getRootLogger();
    Handler[] handlers = rootLogger.getHandlers();
    for (int i = 0; i < handlers.length; i++) {
      if (handlers[i] instanceof SLF4JBridgeHandler) {
        return true;
      }
    }
    return false;
  }

  /**
   * Invoking this method removes/unregisters/detaches all handlers currently attached to the root logger
   */
  public static void removeHandlersForRootLogger() {
    java.util.logging.Logger rootLogger = getRootLogger();
    Handler[] handlers = rootLogger.getHandlers();
    for (int i = 0; i < handlers.length; i++) {
      rootLogger.removeHandler(handlers[i]);
    }
  }

  @Override
  public void close() {
    // NOOP
  }

  @Override
  public void flush() {
    // NOOP
  }

  /**
   * Return the Logger instance that will be used for logging.
   */
  private static Logger getSLF4JLogger(final LogRecord record) {
    String name = record.getLoggerName();
    if (name == null) {
      name = UNKNOWN_LOGGER_NAME;
    }
    return LoggerFactory.getLogger(name);
  }

  private static void callLocationAwareLogger(final LocationAwareLogger lal, final LogRecord record) {
    int julLevelValue = record.getLevel().intValue();
    int slf4jLevel;
    if (julLevelValue <= TRACE_LEVEL_THRESHOLD) {
      slf4jLevel = LocationAwareLogger.TRACE_INT;
    } else if (julLevelValue <= DEBUG_LEVEL_THRESHOLD) {
      slf4jLevel = LocationAwareLogger.DEBUG_INT;
    } else if (julLevelValue <= INFO_LEVEL_THRESHOLD) {
      slf4jLevel = LocationAwareLogger.INFO_INT;
    } else if (julLevelValue <= WARN_LEVEL_THRESHOLD) {
      slf4jLevel = LocationAwareLogger.WARN_INT;
    } else {
      slf4jLevel = LocationAwareLogger.ERROR_INT;
    }
    Pair<String, Object[]> messageArgs = getMessageI18N(record);
    Method m = getSourceMethodInfo(record);
    if (m != null) {
      lal.log(null, m.toString(), slf4jLevel, messageArgs.getFirst(), messageArgs.getSecond(), record.getThrown());
    } else {
      lal.log(null, FQCN, slf4jLevel, messageArgs.getFirst(), messageArgs.getSecond(), record.getThrown());
    }
  }

  @SuppressFBWarnings("UCC_UNRELATED_COLLECTION_CONTENTS") // nature of log args
  private static void callPlainSLF4JLogger(final Logger slf4jLogger, final LogRecord record) {
    Pair<String, Object[]> message = getMessageI18N(record);
    Object[] args = message.getSecond();
    if (args == null || args.length == 0) {
      logEfficient(message.getFirst(), record, slf4jLogger);
    } else {
      int julLevelValue = record.getLevel().intValue();
      Throwable thrown = record.getThrown();
      Method m = getSourceMethodInfo(record);
      Object[] pargs;
      if (m == null) {
        if (thrown == null) {
          pargs = args;
        } else {
          pargs = java.util.Arrays.copyOf(args, args.length + 1);
          pargs[args.length] = thrown;
        }
      } else {
        if (thrown == null) {
          pargs = java.util.Arrays.copyOf(args, args.length + 1);
          pargs[args.length] = m;
        } else {
          pargs = java.util.Arrays.copyOf(args, args.length + 2);
          pargs[args.length] = m;
          pargs[args.length + 1] = thrown;
        }
      }
      if (julLevelValue <= TRACE_LEVEL_THRESHOLD) {
        slf4jLogger.trace(message.getFirst(), pargs);
      } else if (julLevelValue <= DEBUG_LEVEL_THRESHOLD) {
        slf4jLogger.debug(message.getFirst(), pargs);
      } else if (julLevelValue <= INFO_LEVEL_THRESHOLD) {
        slf4jLogger.info(message.getFirst(), pargs);
      } else if (julLevelValue <= WARN_LEVEL_THRESHOLD) {
        slf4jLogger.warn(message.getFirst(), pargs);
      } else {
        slf4jLogger.error(message.getFirst(), pargs);
      }

    }
  }

  public static void logEfficient(final String i18nMessage,
          final LogRecord record, final Logger slf4jLogger) {
    int julLevelValue = record.getLevel().intValue();
    Throwable thrown = record.getThrown();
    Method m = getSourceMethodInfo(record);
    if (thrown != null) {
      if (julLevelValue <= TRACE_LEVEL_THRESHOLD) {
        if (m != null) {
          slf4jLogger.trace(i18nMessage, m, thrown);
        } else {
          slf4jLogger.trace(i18nMessage, thrown);
        }
      } else if (julLevelValue <= DEBUG_LEVEL_THRESHOLD) {
        if (m != null) {
          slf4jLogger.debug(i18nMessage, m, thrown);
        } else {
          slf4jLogger.debug(i18nMessage, thrown);
        }
      } else if (julLevelValue <= INFO_LEVEL_THRESHOLD) {
        if (m != null) {
          slf4jLogger.info(i18nMessage, m, thrown);
        } else {
          slf4jLogger.info(i18nMessage, thrown);
        }
      } else if (julLevelValue <= WARN_LEVEL_THRESHOLD) {
        if (m != null) {
          slf4jLogger.warn(i18nMessage, m, thrown);
        } else {
          slf4jLogger.warn(i18nMessage, thrown);
        }
      } else {
        if (m != null) {
          slf4jLogger.error(i18nMessage, m, thrown);
        } else {
          slf4jLogger.error(i18nMessage, thrown);
        }
      }
    } else {
      if (julLevelValue <= TRACE_LEVEL_THRESHOLD) {
        if (m != null) {
          slf4jLogger.trace(i18nMessage, m);
        } else {
          slf4jLogger.trace(i18nMessage);
        }
      } else if (julLevelValue <= DEBUG_LEVEL_THRESHOLD) {
        if (m != null) {
          slf4jLogger.debug(i18nMessage, m);
        } else {
          slf4jLogger.debug(i18nMessage);
        }
      } else if (julLevelValue <= INFO_LEVEL_THRESHOLD) {
        if (m != null) {
          slf4jLogger.info(i18nMessage, m);
        } else {
          slf4jLogger.info(i18nMessage);
        }
      } else if (julLevelValue <= WARN_LEVEL_THRESHOLD) {
        if (m != null) {
          slf4jLogger.warn(i18nMessage, m);
        } else {
          slf4jLogger.warn(i18nMessage);
        }
      } else {
        if (m != null) {
          slf4jLogger.error(i18nMessage, m);
        } else {
          slf4jLogger.error(i18nMessage);
        }
      }
    }
  }

  @Nullable
  public static Method getSourceMethodInfo(final LogRecord record) {
    Method m;
    try {
      if (ALWAYS_TRY_INFER || (NEED_INFER != null && !NEED_INFER.getBoolean(record))) {
        m = new Method(record.getSourceClassName(), record.getSourceMethodName());
      } else {
        m = null;
      }
    } catch (IllegalArgumentException | IllegalAccessException ex) {
      throw new RuntimeException(ex);
    }
    return m;
  }

  /**
   * Get the record's message, possibly via a resource bundle.
   *
   * @param record
   * @return
   */
  @Nonnull
  private static Pair<String, Object[]> getMessageI18N(final LogRecord record) {
    String message = record.getMessage();
    if (message == null) {
      return Pair.of("", record.getParameters());
    }

    ResourceBundle bundle = record.getResourceBundle();
    if (bundle != null) {
      try {
        message = bundle.getString(message);
      } catch (MissingResourceException e) {
      }
    }
    Object[] params = record.getParameters();
    if (params != null && params.length > 0) {
      StringBuilder msg = SB.get().get();
      if (msg == null) {
        msg = new StringBuilder(64);
        SB.set(new SoftReference<>(msg));
      } else  {
        msg.setLength(0);
      }
      try {
        boolean[] used;
        try {
          MessageFormat msgFormat;
          if (bundle == null) {
            msgFormat = FORMAT_CACHE.getUnchecked(message);
          } else {
            msgFormat = Lazy.LOCALIZED_FORMAT_CACHE.getUnchecked(bundle.getLocale()).getUnchecked(message);
          }
          if (msgFormat != INVALID_FORMAT) {
            used =  msgFormat.format(params, msg);
          } else {
            // Some libraries like jboss log manager embeded do unkosher stuff,
            // like printf style formatting see ExtLogRecord...
            return Pair.of(record.getMessage(), record.getParameters());
          }
        } catch (IOException ex) {
          throw new UncheckedIOException(ex);
        }
        int nrLeft = 0;
        for (boolean u : used) {
          if (!u) {
            nrLeft++;
          }
        }
        if (nrLeft == 0) {
          return Pair.of(msg.toString(), Arrays.EMPTY_OBJ_ARRAY);
        } else {
          Object[] left = new Object[nrLeft];
          for (int i = 0, j = 0; i < used.length; i++) {
            if (!used[i]) {
              left[j++] = params[i];
            }
          }
          return Pair.of(msg.toString(), left);
        }
      } catch (IllegalArgumentException e) {
        LoggerFactory.getLogger(SLF4JBridgeHandler.class).warn("Unable to format {} with {}", message, params, e);
         return Pair.of(message, Arrays.EMPTY_OBJ_ARRAY);
      }
    } else {
      return Pair.of(message, Arrays.EMPTY_OBJ_ARRAY);
    }
  }

  @Override
  public void publish(final LogRecord record) {
    try {
      Logger slf4jLogger = getSLF4JLogger(record);
      if (slf4jLogger instanceof LocationAwareLogger) {
        callLocationAwareLogger((LocationAwareLogger) slf4jLogger, record);
      } else {
        callPlainSLF4JLogger(slf4jLogger, record);
      }
    } catch (RuntimeException ex) {
      LoggerFactory.getLogger(SLF4JBridgeHandler.class).warn("Unable to publish {}", record, ex);
    }
  }

}
