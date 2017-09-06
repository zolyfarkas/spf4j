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
package org.spf4j.jmx.mappers;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sun.jmx.mbeanserver.MXBeanMapping;
import com.sun.jmx.mbeanserver.MXBeanMappingFactory;
import java.io.NotSerializableException;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.ListIterator;
import javax.annotation.Nullable;
import javax.management.openmbean.OpenDataException;
import org.apache.avro.specific.SpecificRecordBase;
import org.spf4j.base.Pair;
import org.spf4j.concurrent.UnboundedLoadingCache;
import org.spf4j.jmx.JMXBeanMapping;
import org.spf4j.jmx.JMXBeanMappingSupplier;

/**
 * OpenType conversion utility. right now can deal with JDK + avro stuff, will eventually become extensible with custom
 * type handling.
 *
 * @author zoly
 */
public final class DefaultMXBeanMappingFactoryOpenTypeMapper implements JMXBeanMappingSupplier {

  private final LoadingCache<Type, JMXBeanMapping> cache;

  private final LinkedList<Pair<Class<?>, JMXBeanMappingSupplier>> mappers;

  public DefaultMXBeanMappingFactoryOpenTypeMapper() {
    cache = new UnboundedLoadingCache(16, new CacheLoader<Type, JMXBeanMapping>() {
              @Override
              public JMXBeanMapping load(final Type key) throws NotSerializableException {
                JMXBeanMapping mxBeanMappingInternal = getMXBeanMappingInternal(key);
                return mxBeanMappingInternal == null ? JMXBeanMapping.NOMAPPING : mxBeanMappingInternal;
              }
            });
    mappers = new LinkedList<>();
    mappers.add(Pair.of(SpecificRecordBase.class, (clasz)
            -> new SpecificRecordOpenTypeMapping((Class) clasz, this)));
  }

  public synchronized <T> boolean register(final Class<T> type,
          final JMXBeanMappingSupplier mapperSuplier) {

    ListIterator<Pair<Class<?>, JMXBeanMappingSupplier>> listIterator = mappers.listIterator();
    while (listIterator.hasNext()) {
      Pair<Class<?>, JMXBeanMappingSupplier> next = listIterator.next();
      final Class<?> nType = next.getFirst();
      if (nType.isAssignableFrom(type)) {
        if (nType == type) {
          return false;
        }
        listIterator.previous();
        listIterator.add((Pair) Pair.of(type, mapperSuplier));
        return true;
      }
    }
    listIterator.add((Pair) Pair.of(type, mapperSuplier));
    return true;
  }

  /**
   * returns MXBeanMapping or null if type is not mappable to a OpenType.
   */
  @Nullable
  public JMXBeanMapping get(final Type type) {
    JMXBeanMapping mapping = cache.getUnchecked(type);
    return mapping == JMXBeanMapping.NOMAPPING ? null : mapping;
  }

  /**
   * returns MXBeanMapping or null if type is not mappable to a OpenType.
   */
  @Nullable
  private synchronized JMXBeanMapping getMXBeanMappingInternal(final Type type) throws NotSerializableException {
    try {
      for (Pair<Class<?>, JMXBeanMappingSupplier> reg : mappers) {
        if (type instanceof Class && reg.getFirst().isAssignableFrom((Class) type)) {
          return reg.getSecond().get(type);
        }
      }
      MXBeanMapping mapping = MXBeanMappingFactory.DEFAULT.mappingForType(type, new MXBeanMappingFactory() {
        @Override
        public MXBeanMapping mappingForType(final Type t, final MXBeanMappingFactory f) throws OpenDataException {
          for (Pair<Class<?>, JMXBeanMappingSupplier> reg : mappers) {
            if (t instanceof Class && reg.getFirst().isAssignableFrom((Class) t)) {
              try {
                return MXBeanMappings.convert(reg.getSecond().get(t));
              } catch (NotSerializableException ex) {
                OpenDataException tex = new OpenDataException(t + " is not serializable");
                tex.initCause(ex);
                throw tex;
              }
            }
          }
          return MXBeanMappingFactory.DEFAULT.mappingForType(t, f);
        }
      });
      return MXBeanMappings.convert(mapping);
    } catch (OpenDataException ex) {
      return null;
    }
  }

  @Override
  public String toString() {
    return "DefaultMXBeanMappingFactoryOpenTypeMapper{" + "cache=" + cache + ", mappers=" + mappers + '}';
  }



}
