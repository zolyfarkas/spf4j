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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import org.spf4j.concurrent.UnboundedLoadingCache;

/**
 *
 * @author Zoltan Farkas
 */
public final class CachedLogConfig implements LogConfig {

  private final LoadingCache<Level, LoadingCache<String, List<LogHandler>>> cache;

  private final ConcurrentMap<List<LogHandler>, List<LogHandler>> interner;

  private final LogConfig wrapped;

  @SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
  public CachedLogConfig(final LogConfig wrapped) {
    this.wrapped = wrapped;
    interner = new ConcurrentHashMap<>();
    cache = new UnboundedLoadingCache<>(6, new CacheLoader<Level, LoadingCache<String, List<LogHandler>>>() {
    @Override
    public LoadingCache<String, List<LogHandler>> load(final Level level) {
      return new UnboundedLoadingCache<>(32,
              new CacheLoader<String, List<LogHandler>>() {
        @Override
        public List<LogHandler> load(final String cat) {
          List<LogHandler> logHandlers = wrapped.getLogHandlers(cat, level);
          return interner.computeIfAbsent(logHandlers, Function.identity());
        }
      });
    }
  });
  }

  @Override
  public List<LogHandler> getLogHandlers(final String category, final Level level) {
    return cache.getUnchecked(level).getUnchecked(category);
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
