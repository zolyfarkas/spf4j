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
package org.spf4j.reflect;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gnu.trove.set.hash.THashSet;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.spf4j.base.UncheckedExecutionException;
import org.spf4j.concurrent.UnboundedLoadingCache;

/**
 * @author Zoltan Farkas
 */
@ThreadSafe
public final class CachingTypeMapSupplierWrapper<H, E extends Exception> implements ByTypeSupplier<H, E> {

  private final LoadingCache<Type, Set<H>> cache;

  @GuardedBy("syncObj")
  private final TypeMap<ByTypeSupplier<H, E>> wrapped;

  private final Object syncObj;

  private final Class<E> exClass;

  public CachingTypeMapSupplierWrapper(final CacheBuilder<Type, Set<H>> cacheBuilder, final TypeMap wrapped,
          final Class<E> exClass) {
    this.syncObj = new Object();
    this.wrapped = wrapped;
    this.exClass = exClass;
    cache = cacheBuilder.build(new TypeMapedObjLoader());
  }

  public CachingTypeMapSupplierWrapper(final TypeMap wrapped, final Class<E> exClass) {
    this.syncObj = new Object();
    this.wrapped = wrapped;
    this.exClass = exClass;
    cache = new UnboundedLoadingCache<>(16, new TypeMapedObjLoader());
  }


  public boolean putIfNotPresent(final Type type, final ByTypeSupplier<H, E> appender) {
    synchronized (syncObj) {
      return wrapped.putIfNotPresent(type, appender);
    }
  }

  public void safePut(final Type type, final ByTypeSupplier<H, E> object) {
    if (!putIfNotPresent(type, object)) {
      throw new IllegalArgumentException("Cannot put " + type + ", " + object + " exiting mapping present");
    }
  }

  public boolean remove(final Type type) {
    boolean remove;
    synchronized (syncObj) {
      remove = wrapped.remove(type);
    }
    if (remove) {
      cache.invalidateAll(); // a bit lazy
      return true;
    } else {
      return false;
    }
  }

  public void clearCache() {
    cache.invalidateAll();
  }

  @Override
  @SuppressFBWarnings({ "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE", "SPP_USE_ISEMPTY", "LEST_LOST_EXCEPTION_STACK_TRACE" })
  @Nullable
  public H get(final Type type) throws E {
    Set<H> get;
    try {
      get = cache.get(type);
    } catch (ExecutionException ex) {
      Throwable cause = ex.getCause();
      if (cause.getClass() == exClass) {
        throw (E) cause;
      } else {
        throw new UncheckedExecutionException(ex);
      }
    }
    int size = get.size();
    if (size == 1) {
      return get.iterator().next();
    } else if (size == 0) {
      return null;
    } else {
      throw new IllegalArgumentException("Ambiguous handlers " + get + " for " + type + " in  " + this);
    }
  }

  private final class TypeMapedObjLoader extends CacheLoader<Type, Set<H>> {

    @Override
    public Set<H> load(final Type key) throws Exception {
      synchronized (wrapped) {
        Set<ByTypeSupplier<H, E>> all = wrapped.getAll(key);
        Set<H> result = new THashSet<>(all.size());
        for (ByTypeSupplier<H, E> s : all) {
          result.add(s.get(key));
        }
        return result;
      }
    }
  }

  @Override
  public String toString() {
    String wts;
    synchronized (wrapped) {
      wts = wrapped.toString();
    }
    return "CachingTypeMapWrapper{" + "cache=" + cache + ", wrapped=" + wts + '}';
  }


}
