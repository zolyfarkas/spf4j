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
import java.lang.reflect.Type;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.spf4j.concurrent.UnboundedLoadingCache;

/**
 * @author Zoltan Farkas
 */
@ThreadSafe
public final class CachingTypeMapWrapper<H> implements TypeMap<H> {

  private final LoadingCache<Type, Set<H>> cache;

  @GuardedBy("syncObj")
  private final TypeMap<H> wrapped;

  private final Object syncObj;

  public CachingTypeMapWrapper(final CacheBuilder<Type, Set<H>> cacheBuilder, final TypeMap wrapped) {
    this.syncObj = new Object();
    this.wrapped = wrapped;
    cache = cacheBuilder.build(new TypeMapedObjLoader());
  }

  public CachingTypeMapWrapper(final TypeMap wrapped) {
    this.syncObj = new Object();
    this.wrapped = wrapped;
    cache = new UnboundedLoadingCache<>(16, new TypeMapedObjLoader());
  }


  @Override
  public Set<H> getAll(final Type t) {
    return cache.getUnchecked(t);
  }

  @Override
  public boolean putIfNotPresent(final Type type, final H appender) {
    synchronized (syncObj) {
      return wrapped.putIfNotPresent(type, appender);
    }
  }

  @Override
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

  @Override
  public H getExact(final Type t) {
    synchronized (wrapped) {
      return wrapped.getExact(t);
    }
  }

  public void replace(final Type t, final Function<H, H> f) {
    synchronized (wrapped) {
      H exact = wrapped.getExact(t);
      if (exact != null && !wrapped.remove(t)) {
        throw new IllegalStateException("Illegal Stat, type = " + t + " wrapped =  " + wrapped);
      }
      wrapped.safePut(t, f.apply(exact));
     }
    cache.invalidateAll(); // a bit lazy :-)
  }

  public void clearCache() {
    cache.invalidateAll();
  }

  private final class TypeMapedObjLoader extends CacheLoader<Type, Set<H>> {

    @Override
    public Set<H> load(final Type key) throws Exception {
      synchronized (wrapped) {
        return wrapped.getAll(key);
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
