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
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.base.UncheckedExecutionException;

/**
 * @author zoly
 * @deprecated use UnboundedLoadingCache instead.
 */
@Deprecated
@ParametersAreNonnullByDefault
public final class UnboundedRacyLoadingCache<K, V> implements LoadingCache<K, V> {

    private final ConcurrentMap<K, V> map;

    private final CacheLoader<K, V> loader;

    public UnboundedRacyLoadingCache(final int initialSize, final CacheLoader<K, V> loader) {
        this(initialSize, 8, loader);
    }

    public UnboundedRacyLoadingCache(final int initialSize, final int concurrency, final CacheLoader<K, V> loader) {
        this.map = new ConcurrentHashMap<>(
                initialSize, 0.75f, concurrency);
        this.loader = loader;
    }

    @Override
    public V get(final K key) throws ExecutionException {
        V val = map.get(key);
        if (val == null) {
            try {
                val = loader.load(key);
            } catch (Exception ex) {
               throw new ExecutionException(ex);
            }
            map.put(key, val);
        }
        return val;
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
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
    public void refresh(final K key) {
        getUnchecked(key);
    }

    @Override
    public ConcurrentMap<K, V> asMap() {
        throw new UnsupportedOperationException();
    }

    @Override
    public V getIfPresent(final Object key) {
        return map.get(key);
    }

    @Override
    public V get(final K key, final Callable<? extends V> valueLoader) throws ExecutionException {
        V val = map.get(key);
        if (val == null) {
            try {
                val = valueLoader.call();
            } catch (Exception ex) {
                throw new ExecutionException(ex);
            }
            map.put(key, val);
        }
        return val;
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
        map.put(key, value);
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
    public void cleanUp() {
        map.clear();
    }

    @Override
    public String toString() {
        return "UnboundedRacyLoadingCache{" + "map=" + map + ", loader=" + loader + '}';
    }

}
