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

import com.google.common.reflect.TypeToken;
import java.io.File;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.management.ObjectName;
import javax.management.openmbean.OpenType;
import static javax.management.openmbean.SimpleType.BIGDECIMAL;
import static javax.management.openmbean.SimpleType.BIGINTEGER;
import static javax.management.openmbean.SimpleType.BOOLEAN;
import static javax.management.openmbean.SimpleType.BYTE;
import static javax.management.openmbean.SimpleType.CHARACTER;
import static javax.management.openmbean.SimpleType.DATE;
import static javax.management.openmbean.SimpleType.FLOAT;
import static javax.management.openmbean.SimpleType.INTEGER;
import static javax.management.openmbean.SimpleType.OBJECTNAME;
import static javax.management.openmbean.SimpleType.SHORT;
import static javax.management.openmbean.SimpleType.STRING;
import static javax.management.openmbean.SimpleType.LONG;
import static javax.management.openmbean.SimpleType.DOUBLE;
import static javax.management.openmbean.SimpleType.VOID;
import org.apache.avro.specific.SpecificRecordBase;
import org.spf4j.base.Pair;
import org.spf4j.jmx.JMXBeanMapping;
import org.spf4j.jmx.JMXBeanMappingSupplier;
import org.spf4j.jmx.mappers.Spf4jJMXBeanMapping.ArrayMXBeanType;
import org.spf4j.reflect.CachingTypeMapSupplierWrapper;
import org.spf4j.reflect.GraphTypeMap;


/**
 * @author Zoltan Farkas
 */
public final class Spf4jOpenTypeMapper implements JMXBeanMappingSupplier {

  private static final Logger LOG = Logger.getLogger(Spf4jOpenTypeMapper.class.getName());

  private static final ThreadLocal<Set<Type>> IN_PROGRESS = new ThreadLocal<Set<Type>>() {
    @Override
    protected Set<Type> initialValue() {
      return new HashSet<>(2);
    }

  };

  private static final Pair<OpenType<?>, Class<?>[]>[] SIMPLE_TYPES = new Pair[]{
    Pair.of(BIGDECIMAL, new Class[]{BigDecimal.class}),
    Pair.of(BIGINTEGER, new Class[]{BigInteger.class}),
    Pair.of(BOOLEAN, new Class[]{Boolean.class, boolean.class}),
    Pair.of(BYTE, new Class[]{Byte.class, byte.class}),
    Pair.of(CHARACTER, new Class[]{Character.class, char.class}),
    Pair.of(DATE, new Class[]{Date.class}),
    Pair.of(DOUBLE, new Class[]{Double.class, double.class}),
    Pair.of(FLOAT, new Class[]{Float.class, float.class}),
    Pair.of(INTEGER, new Class[]{Integer.class, int.class}),
    Pair.of(LONG, new Class[]{Long.class, long.class}),
    Pair.of(SHORT, new Class[]{Short.class, short.class}),
    Pair.of(OBJECTNAME, new Class[]{ObjectName.class}),
    Pair.of(STRING, new Class[]{String.class}),
    Pair.of(VOID, new Class[]{Void.class})
  };

  private final CachingTypeMapSupplierWrapper<JMXBeanMapping, NotSerializableException> cache;

  public Spf4jOpenTypeMapper() {
    cache = new CachingTypeMapSupplierWrapper<>(new GraphTypeMap(), NotSerializableException.class);
      for (final Pair<OpenType<?>, Class<?>[]> t : SIMPLE_TYPES) {
        for (Class<?> c : t.getSecond()) {
          cache.safePut(c, (type) -> new Spf4jJMXBeanMapping.BasicMXBeanType(c, t.getFirst()));
        }
      }
      cache.safePut(Enum.class, (type) -> new Spf4jJMXBeanMapping.EnumMXBeanType((Class) type));
      cache.safePut(SpecificRecordBase.class, (type) -> new SpecificRecordOpenTypeMapping((Class) type, this));
      cache.safePut(Properties.class, (type) -> new Spf4jJMXBeanMapping.MapMXBeanType((Class) type, this));
      cache.safePut(Iterable.class, (type) -> {
        if (type instanceof ParameterizedType) {
          return new Spf4jJMXBeanMapping.ListMXBeanType((ParameterizedType) type, this);
        } else {
          return Spf4jJMXBeanMapping.defaultHandler(type, this);
        }
      });
      cache.safePut(Map.class, (type) -> {
        if (type instanceof ParameterizedType) {
          return new Spf4jJMXBeanMapping.MapMXBeanType((ParameterizedType) type, this);
        } else {
          throw new IllegalArgumentException("Only parameterized maps can be converted to opentype, not " + type);
        }
      });
      cache.safePut(Map.Entry.class, (type) -> {
        if (type instanceof ParameterizedType) {
          return new MapEntryOpenTypeMapping((ParameterizedType) type, this);
        } else {
          return Spf4jJMXBeanMapping.defaultHandler(type, this);
        }
      });
      cache.safePut(Object.class, (type) -> {
        if (type instanceof GenericArrayType) {
          return new Spf4jJMXBeanMapping.GenericArrayMXBeanType((GenericArrayType) type, this);
        } else if (type instanceof Class && ((Class) type).isArray()) {
          return new ArrayMXBeanType(((Class) type), this);
        } else {
          return Spf4jJMXBeanMapping.defaultHandler(type, this);
        }
      });
      cache.safePut(File.class, (type) -> JMXBeanMapping.NOMAPPING);
  }


  @Override
  @Nullable
  public JMXBeanMapping get(final Type t) throws NotSerializableException {

        Set<Type> ip = IN_PROGRESS.get();
        if (ip.contains(t)) {
           LOG.log(Level.FINE, "No openType mapping for {0} recursive data structure", t);
           return null;
        }
        try {
          ip.add(t);
          JMXBeanMapping val = cache.get(t);
          if (val == JMXBeanMapping.NOMAPPING) {
            return null;
          } else {
            return val;
          }
        } catch (NotSerializableException ex) {
          TypeToken<?> tt = TypeToken.of(t);
          if (tt.isSubtypeOf(Serializable.class) || tt.getRawType().isInterface()) {
            LOG.log(Level.FINE, "No mapping for type {0}", t);
            LOG.log(Level.FINEST, "Exception Detail", ex);
            return null;
          } else {
            throw ex;
          }
        } catch (RuntimeException ex) {
          TypeToken<?> tt = TypeToken.of(t);
          if (tt.isSubtypeOf(Serializable.class) || tt.getRawType().isInterface()) {
            LOG.log(Level.FINE, "No mapping for type {0}", t);
            LOG.log(Level.FINEST, "Exception Detail", ex);
            return null;
          } else {
            NotSerializableException nsex = new NotSerializableException("Type " + t + "  must be serializable");
            nsex.addSuppressed(ex);
            throw nsex;
          }
        } finally {
          ip.remove(t);
        }
  }

  public CachingTypeMapSupplierWrapper<JMXBeanMapping, NotSerializableException> getCache() {
    return cache;
  }

  @Override
  public String toString() {
    return "Spf4jOpenTypeMapper{" + "convertedTypes=" + cache + '}';
  }

}
