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
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * simple implementation for resultcache.
 *
 * @author zoly
 */
@ThreadSafe
public final class SimpleResultCache implements ResultCache {

    private final ConcurrentMap<Object, Object> permanentCache;

    private final Cache<Object, Object> transientCache;

    public SimpleResultCache() {
        this(100000);
    }

    public SimpleResultCache(final int maxSize) {
        permanentCache = new ConcurrentHashMap<Object, Object>();
        transientCache = CacheBuilder.newBuilder().maximumSize(maxSize).build();
    }

    @Override
    public void putPermanentResult(final Object key, final Object result) {
        if (result == null) {
            permanentCache.put(key, ResultCache.NULL);
        } else {
            permanentCache.put(key, result);
        }
    }

    @Override
    public void putTransientResult(final Object key, final Object result) {
        if (result == null) {
            transientCache.put(key, ResultCache.NULL);
        } else {
            transientCache.put(key, result);
        }
    }

    @Override
    public Object getResult(final Object key, final Callable<Object> compute)
            throws ZExecutionException {
        Object result = permanentCache.get(key);
        if (result == null) {
            try {
                result = transientCache.get(key, new Callable<Object>() {
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
