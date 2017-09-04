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
package org.spf4j.jmx;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.sun.jmx.mbeanserver.MXBeanMapping;
import com.sun.jmx.mbeanserver.MXBeanMappingFactory;
import java.io.InvalidObjectException;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificRecordBase;
import org.spf4j.base.Pair;
import org.spf4j.concurrent.UnboundedLoadingCache;

/**
 * OpenType conversion utility. right now can deal with JDK + avro stuff, will eventually become extensible with custom
 * type handling.
 *
 * @author zoly
 */
// FB does not like guava Convertere equals/hashcode.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings("HE_INHERITS_EQUALS_USE_HASHCODE")
public final class OpenTypeConverter {

  private static final MXBeanMapping NULL_MAPPING = new MXBeanMapping(Void.class, SimpleType.VOID) {
    @Override
    public Object fromOpenValue(final Object openValue) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object toOpenValue(final Object javaValue) {
      throw new UnsupportedOperationException();
    }
  };

  private static final LoadingCache<Class<?>, MXBeanMapping> CACHE
          = new UnboundedLoadingCache(16, new CacheLoader<Class<?>, MXBeanMapping>() {
            @Override
            public MXBeanMapping load(final Class<?> key) {
              MXBeanMapping mxBeanMappingInternal = getMXBeanMappingInternal(key);
              return mxBeanMappingInternal == null ? NULL_MAPPING : mxBeanMappingInternal;
            }
          });

  private static final LinkedList<Pair<Class<?>, Function<Class<?>, MXBeanMapping>>> JMX_MAPPERS
          = new LinkedList<>();

  static {
    JMX_MAPPERS.add(Pair.of(SpecificRecordBase.class, (clasz)
            -> {
      try {
        return new SpecificRecordOpenTypeMapping(clasz,
                typeFromSpecificRecord((SpecificRecordBase) clasz.newInstance()));
      } catch (InstantiationException | IllegalAccessException ex) {
        throw new UncheckedExecutionException(ex);
      }
    }));
  }

  private OpenTypeConverter() {
  }

  public <T> boolean register(final Class<T> type, final Function<Class<?>, MXBeanMapping> mapperSuplier) {

    ListIterator<Pair<Class<?>, Function<Class<?>, MXBeanMapping>>> listIterator = JMX_MAPPERS.listIterator();
    while (listIterator.hasNext()) {
      Pair<Class<?>, Function<Class<?>, MXBeanMapping>> next = listIterator.next();
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
  public static MXBeanMapping getMXBeanMapping(final Class<?> type) {
    MXBeanMapping mapping = CACHE.getUnchecked(type);
    return mapping == NULL_MAPPING ? null : mapping;
  }

  /**
   * returns MXBeanMapping or null if type is not mappable to a OpenType.
   */
  @Nullable
  private static MXBeanMapping getMXBeanMappingInternal(final Class<?> type) {
    try {
      for (Pair<Class<?>, Function<Class<?>, MXBeanMapping>> reg : JMX_MAPPERS) {
        if (reg.getFirst().isAssignableFrom(type)) {
          return reg.getSecond().apply(type);
        }
      }
      return MXBeanMappingFactory.DEFAULT.mappingForType(type, new MXBeanMappingFactory() {
        @Override
        public MXBeanMapping mappingForType(final Type t, final MXBeanMappingFactory f) throws OpenDataException {
          for (Pair<Class<?>, Function<Class<?>, MXBeanMapping>> reg : JMX_MAPPERS) {
            if (t instanceof Class && reg.getFirst().isAssignableFrom((Class) t)) {
              return reg.getSecond().apply((Class) t);
            }
          }
          return MXBeanMappingFactory.DEFAULT.mappingForType(t, f);
        }
      });
    } catch (OpenDataException ex) {
      return null;
    }
  }

  @Nullable
  public static OpenType<?> getOpenType(@Nonnull final Class<?> type) {
    MXBeanMapping mxBeanMapping = getMXBeanMapping(type);
    return mxBeanMapping == null ? null : mxBeanMapping.getOpenType();
  }

  private static CompositeData fromSpecificRecord(final SpecificRecordBase r) throws OpenDataException {
    CompositeType ctype = typeFromSpecificRecord(r);
    Schema schema = r.getSchema();
    List<Schema.Field> fields = schema.getFields();
    int size = fields.size();
    String[] names = new String[size];
    Object[] values = new Object[size];
    for (Schema.Field field : fields) {
      int pos = field.pos();
      names[pos] = field.name();
      Object val = r.get(pos);
      MXBeanMapping mapping = OpenTypeConverter.getMXBeanMapping(SpecificData.get().getClass(field.schema()));
      values[pos] = mapping.toOpenValue(val);
    }
    return new CompositeDataSupport(ctype, names, values);
  }

  private static CompositeType typeFromSpecificRecord(final SpecificRecordBase r) {
    Schema schema = r.getSchema();
    List<Schema.Field> fields = schema.getFields();
    int size = fields.size();
    String[] names = new String[size];
    String[] descriptions = new String[size];
    OpenType<?>[] types = new OpenType<?>[size];
    for (Schema.Field field : fields) {
      int pos = field.pos();
      names[pos] = field.name();
      descriptions[pos] = field.doc();
      types[pos] = OpenTypeConverter.getOpenType(SpecificData.get().getClass(field.schema()));
    }
    try {
      return new CompositeType(schema.getFullName(), schema.getDoc(),
              names, descriptions, types);
    } catch (OpenDataException ex) {
      throw new UncheckedExecutionException(ex);
    }
  }

  private static final class SpecificRecordOpenTypeMapping extends MXBeanMapping {

    SpecificRecordOpenTypeMapping(final Type javaType, final OpenType<?> openType) {
      super(javaType, openType);
    }

    @Override
    public Object fromOpenValue(final Object openValue) throws InvalidObjectException {
      if (!(openValue instanceof CompositeData)) {
        throw new InvalidObjectException("Not a CompositeData " + openValue);
      }
      CompositeData cd = (CompositeData) openValue;
      CompositeType compositeType = cd.getCompositeType();
      String typeName = compositeType.getTypeName();
      SpecificRecordBase rec;
      try {
        rec = (SpecificRecordBase) Class.forName(typeName).newInstance();
      } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
        InvalidObjectException oex = new InvalidObjectException("Invalid object " + openValue);
        oex.addSuppressed(ex);
        throw oex;
      }
      for (Schema.Field field : rec.getSchema().getFields()) {
        rec.put(field.pos(),
                OpenTypeConverter.getMXBeanMapping(SpecificData.get().getClass(field.schema()))
                        .fromOpenValue(cd.get(field.name())));
      }
      return rec;
    }

    @Override
    public Object toOpenValue(final Object javaValue) throws OpenDataException {
      SpecificRecordBase sr = (SpecificRecordBase) javaValue;
      return fromSpecificRecord((SpecificRecordBase) sr);
    }

  }

}
