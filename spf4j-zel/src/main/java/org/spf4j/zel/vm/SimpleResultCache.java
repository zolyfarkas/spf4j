/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
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
 */
package org.spf4j.zel.vm;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.spf4j.concurrent.UnboundedLoadingCache;

/**
 * simple implementation for resultcache.
 *
 * @author zoly
 */
@ThreadSafe
public final class SimpleResultCache implements ResultCache {

    private final LoadingCache<Program, ConcurrentMap<List<Object>, Object>> permanentCache;

    private final LoadingCache<Program, Cache<List<Object>, Object>> transientCache;

    public SimpleResultCache() {
        this(100000);
    }

    public SimpleResultCache(final int maxSize) {
        permanentCache = new UnboundedLoadingCache<Program, ConcurrentMap<List<Object>, Object>>(16,
                new CacheLoader<Program, ConcurrentMap<List<Object>, Object>>() {
                    @Override
                    public ConcurrentMap<List<Object>, Object> load(final Program key) throws Exception {
                        return new ConcurrentHashMap<List<Object>, Object>();
                    }
                });
        transientCache = new UnboundedLoadingCache<Program, Cache<List<Object>, Object>>(16,
                new CacheLoader<Program, Cache<List<Object>, Object>>() {
            @Override
            public Cache<List<Object>, Object> load(final Program key) throws Exception {
                return CacheBuilder.newBuilder().maximumSize(maxSize).build();
            }
        });

    }

    @Override
    public void putPermanentResult(final Program program,
            @Nonnull final List<Object> params, final Object result) {
        final ConcurrentMap<List<Object>, Object> resultsMap = permanentCache.getUnchecked(program);
        if (result == null) {
            resultsMap.put(params, ResultCache.NULL);
        } else {
            resultsMap.put(params, result);
        }
    }

    @Override
    public void putTransientResult(final Program program,
            @Nonnull final  List<Object> params, final Object result) {
        final Cache<List<Object>, Object> resultsMap = transientCache.getUnchecked(program);
        if (result == null) {
            resultsMap.put(params, ResultCache.NULL);
        } else {
            resultsMap.put(params, result);
        }
    }

    @Override
    public Object getResult(final Program program,
            @Nonnull final List<Object> params, final Callable<Object> compute)
            throws ZExecutionException {
        Object result = permanentCache.getUnchecked(program).get(params);
        if (result == null) {
            try {
                result = transientCache.getUnchecked(program).get(params, new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        Object result = compute.call();
                        if (result == null) {
                            return ResultCache.NULL;
                        } else {
                            return result;
                        }
                    }
                });
            } catch (ExecutionException ex) {
                throw new ZExecutionException(ex);
            }
        }
        if (result == ResultCache.NULL) {
            result = null;
        }
        return result;
    }

    @Override
    public String toString() {
        return "SimpleResultCache{" + "permanentCache=" + permanentCache + ", transientCache=" + transientCache + '}';
    }

}
