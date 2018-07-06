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
package org.spf4j.concurrent;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.base.Callables;
import org.spf4j.base.UncheckedExecutionException;

/**
 *
 * custom build high performance implementation for a unbounded guava cache: UnboundedLoadingCache is implemented with
 * JDK concurrent map UnboundedLoadingCache2 is using the JDK 1.8 computing map functionality, but benchmarks show worse
 * performance.
 *
 * Benchmark Mode Cnt Score Error Units CacheBenchmark.guavaCache thrpt 15 29011674.275 ± 710672.413 ops/s
 * CacheBenchmark.spf4j2Cache thrpt 15 30567248.015 ± 807965.535 ops/s CacheBenchmark.spf4jCache thrpt 15 37961593.882 ±
 * 1136244.254 ops/s CacheBenchmark.spf4jRacyCache thrpt 15 37553655.751 ± 855349.501 ops/s
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class UnboundedLoadingCache<K, V> implements LoadingCache<K, V> {

  private final ConcurrentMap<K, Callable<? extends V>> map;

  private final CacheLoader<K, V> loader;

  public UnboundedLoadingCache(final int initialSize, final CacheLoader<K, V> loader) {
    this(initialSize, 8, loader);
  }

  public UnboundedLoadingCache(final int initialSize, final int concurrency, final CacheLoader<K, V> loader) {
    this.map = new ConcurrentHashMap<>(
            initialSize, 0.75f, concurrency);
    this.loader = loader;
  }

  /**
   * Will use a ConcurrentSkipListMap to store the underlying data.
   * @param comparator
   * @param loader
   */
  public UnboundedLoadingCache(final Comparator<? super K> comparator, final CacheLoader<K, V> loader) {
    this.map = new ConcurrentSkipListMap<>(comparator);
    this.loader = loader;
  }


  @Override
  public V get(final K key) throws ExecutionException {
    Callable<? extends V> existingValHolder = map.get(key);
    if (existingValHolder == null) {
      Callable<? extends V> newHolder = Callables.memorized(new Callable<V>() {
        @Override
        public V call() throws Exception {
          return loader.load(key);
        }
      });
      existingValHolder = map.putIfAbsent(key, newHolder);
      if (existingValHolder == null) {
        existingValHolder = newHolder;
      }
    }
    try {
      return existingValHolder.call();
    } catch (Exception ex) {
      throw new ExecutionException(ex);
    }

  }

  @Override
  public V getUnchecked(final K key) {
    try {
      return get(key);
    } catch (ExecutionException ex) {
      throw new UncheckedExecutionException(ex);
    }
  }

  @Override
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
  public ImmutableMap<K, V> getAll(final Iterable<? extends K> keys) throws ExecutionException {
    ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
    for (K key : keys) {
      builder.put(key, get(key));
    }
    return builder.build();
  }

  @Override
  public V apply(final K key) {
    return getUnchecked(key);
  }

  @Override
  public void refresh(final K key) {
    getUnchecked(key);
  }

  @Override
  public ConcurrentMap<K, V> asMap() {
    return new MapView();
  }

  @Override
  @Nullable
  public V getIfPresent(final Object key) {
    Callable<? extends V> existingValHolder = map.get(key);
    if (existingValHolder != null) {
      try {
        return existingValHolder.call();
      } catch (Exception ex) {
        throw new UncheckedExecutionException(ex);
      }
    } else {
      return null;
    }
  }

  @Override
  public V get(final K key, final Callable<? extends V> valueLoader) throws ExecutionException {
    Callable<? extends V> existingValHolder = map.get(key);
    if (existingValHolder == null) {
      Callable<? extends V> newHolder = Callables.memorized(valueLoader);
      existingValHolder = map.putIfAbsent(key, newHolder);
      if (existingValHolder == null) {
        existingValHolder = newHolder;
      }
    }
    try {
      return existingValHolder.call();
    } catch (Exception ex) {
      throw new ExecutionException(ex);
    }
  }

  @Override
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
  public ImmutableMap<K, V> getAllPresent(final Iterable<?> keys) {
    ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
    for (K key : (Iterable<K>) keys) {
      V val = getIfPresent(key);
      if (val != null) {
        builder.put(key, val);
      }
    }
    return builder.build();
  }

  @Override
  public void put(final K key, final V value) {
    map.put(key, Callables.constant(value));
  }

  @Override
  public void putAll(final Map<? extends K, ? extends V> m) {
    for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void invalidate(final Object key) {
    map.remove(key);
  }

  @Override
  public void invalidateAll(final Iterable<?> keys) {
    for (K key : (Iterable<K>) keys) {
      invalidate(key);
    }
  }

  @Override
  public void invalidateAll() {
    map.clear();
  }

  @Override
  public long size() {
    return map.size();
  }

  @Override
  public CacheStats stats() {
    throw new UnsupportedOperationException("Not supported");
  }

  @Override
  @SuppressFBWarnings("NM_CONFUSING")
  public void cleanUp() {
    map.clear();
  }

  public Set<K> getKeysLoaded() {
    return map.keySet();
  }

  @Override
  public String toString() {
    return "UnboundedLoadingCache{" + "map=" + map + ", loader=" + loader + '}';
  }

  private class MapView implements ConcurrentMap<K, V> {

    @Override
    public V putIfAbsent(final K key, final V value) {
      Callable<? extends V> result = map.putIfAbsent(key, Callables.constant(value));
      try {
        return result == null ? null : result.call();
      } catch (Exception ex) {
        throw new UncheckedExecutionException(ex);
      }
    }

    @Override
    public boolean remove(final Object key, final Object value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean replace(final K key, final V oldValue, final V newValue) {
      throw new UnsupportedOperationException();
    }

    @Override
    public V replace(final K key, final V value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
      return map.size();
    }

    @Override
    public boolean isEmpty() {
      return map.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
      return map.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public V get(final Object key) {
      Callable<? extends V> result = map.get(key);
      try {
        return result == null ? null : result.call();
      } catch (Exception ex) {
        throw new UncheckedExecutionException(ex);
      }
    }

    @Override
    public V put(final K key, final V value) {
      Callable<? extends V> result = map.put(key, Callables.constant(value));
      try {
        return result == null ? null : result.call();
      } catch (Exception ex) {
        throw new UncheckedExecutionException(ex);
      }
    }

    @Override
    public V remove(final Object key) {
      Callable<? extends V> result = map.remove(key);
      try {
        return result == null ? null : result.call();
      } catch (Exception ex) {
        throw new UncheckedExecutionException(ex);
      }
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
      for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
        map.put(entry.getKey(), entry::getValue);
      }
    }

    @Override
    public void clear() {
      map.clear();
    }

    @Override
    public Set<K> keySet() {
      return map.keySet();
    }

    @Override
    public Collection<V> values() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
      throw new UnsupportedOperationException();
    }
  }

}
