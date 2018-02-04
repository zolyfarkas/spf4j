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

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.EnumMap;
import java.util.Map;
import org.spf4j.concurrent.UnboundedLoadingCache;

/**
 *
 * @author Zoltan Farkas
 */
public final class CachedLogConfig implements LogConfig {

  private static final LogConsumer NULL_CONSUMER = (LogRecord record) -> { };

  private final Map<Level, LoadingCache<String, LogConsumer>> cache;

  private final LogConfig wrapped;

  @SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
  public CachedLogConfig(final LogConfig wrapped) {
    this.wrapped = wrapped;
    cache = new EnumMap<>(Level.class);
    for (Level level : Level.values()) {
      cache.put(level, new UnboundedLoadingCache<>(16,
              new CacheLoader<String, LogConsumer>() {
        @Override
        public LogConsumer load(final String cat) {
          LogConsumer logHandlers = wrapped.getLogConsumer(cat, level);
          if (logHandlers == null) {
            return NULL_CONSUMER;
          } else {
            return logHandlers;
          }
        }
      }));
    }
  }

  @Override
  public LogConsumer getLogConsumer(final String category, final Level level) {
    LogConsumer cons = cache.get(level).getUnchecked(category);
    return (cons == NULL_CONSUMER) ? null : cons;
  }

  @Override
  public String toString() {
    return "CachedLogConfig{" + "cache=" + cache + '}';
  }

  @Override
  public CachedLogConfig add(final String category, final LogHandler handler) {
    return new CachedLogConfig(wrapped.add(category, handler));
  }

  @Override
  public CachedLogConfig remove(final String category, final LogHandler handler) {
    return new CachedLogConfig(wrapped.remove(category, handler));
  }

}
