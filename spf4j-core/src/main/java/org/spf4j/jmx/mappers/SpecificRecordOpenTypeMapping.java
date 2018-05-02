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

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import org.spf4j.jmx.JMXBeanMapping;
import com.sun.jmx.mbeanserver.MXBeanMapping;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificRecordBase;
import org.spf4j.base.Reflections;
import org.spf4j.base.UncheckedExecutionException;
import org.spf4j.jmx.JMXBeanMappingSupplier;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("SCII_SPOILED_CHILD_INTERFACE_IMPLEMENTOR")
public final class SpecificRecordOpenTypeMapping extends MXBeanMapping implements JMXBeanMapping {

  private static final Schema NULL_SCHEMA = Schema.create(Schema.Type.NULL);

  private final JMXBeanMappingSupplier typeMapper;

  public SpecificRecordOpenTypeMapping(final Class<? extends SpecificRecordBase> javaType,
          final JMXBeanMappingSupplier typeMapper) throws NotSerializableException {
    super(javaType, typeFromSpecificRecord(javaType, typeMapper));
    this.typeMapper = typeMapper;
  }

  @Override
  @Nonnull
  @SuppressFBWarnings("AI_ANNOTATION_ISSUES_NEEDS_NULLABLE") // false positive
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
      Object obj = cd.get(field.name());
      try {
        rec.put(field.pos(),
                typeMapper.get(getGenericType(field.schema()))
                        .fromOpenValue(obj));
      } catch (NotSerializableException ex) {
        InvalidObjectException tex = new InvalidObjectException("Ivalid Object " + obj + " id " + cd);
        tex.addSuppressed(ex);
        throw tex;
      }
    }
    return rec;
  }

  private static <K, V> TypeToken<Map<K, V>> mapToken(final TypeToken<K> keyToken,
          final TypeToken<V> valueToken) {
    return new TypeToken<Map<K, V>>() { }
      .where(new TypeParameter<K>() { }, keyToken)
      .where(new TypeParameter<V>() { }, valueToken);
  }

  private static <T> TypeToken<List<T>> listToken(final TypeToken<T> valToken) {
    return new TypeToken<List<T>>() { }
      .where(new TypeParameter<T>() { }, valToken);
  }

  @SuppressFBWarnings("AI_ANNOTATION_ISSUES_NEEDS_NULLABLE") // false positive
  @Nonnull
  public static Type getGenericType(final Schema schema) {
    Schema.Type type = schema.getType();
    switch (type) {
      case ARRAY:
        Type elementType = getGenericType(schema.getElementType());
        return listToken(TypeToken.of(elementType)).getType();
      case MAP:
        Type valueType = getGenericType(schema.getValueType());
        return mapToken(TypeToken.of(String.class), TypeToken.of(valueType)).getType();
      case UNION:
        List<Schema> types = schema.getTypes();     // elide unions with null
        if ((types.size() == 2) && types.contains(NULL_SCHEMA)) {
          return Reflections.primitiveToWrapper(
                  getGenericType(types.get(types.get(0).equals(NULL_SCHEMA) ? 1 : 0)));
        } else {
          throw new IllegalArgumentException("Schema " + schema
                  + "Cannot be mapped to an OpenType duw to multibranch union");
        }
      default:
        return SpecificData.get().getClass(schema);
    }
  }


  @Override
  public Object toOpenValue(final Object javaValue) throws OpenDataException {
    SpecificRecordBase sr = (SpecificRecordBase) javaValue;
    return fromSpecificRecord((SpecificRecordBase) sr);
  }


  private CompositeData fromSpecificRecord(final SpecificRecordBase r)
          throws OpenDataException {
    CompositeType ctype;
    try {
      ctype = typeFromSpecificRecord(r, typeMapper);
    } catch (NotSerializableException ex) {
        throw new UncheckedIOException(ex); //this should never happen
    }
    Schema schema = r.getSchema();
    List<Schema.Field> fields = schema.getFields();
    int size = fields.size();
    String[] names = new String[size];
    Object[] values = new Object[size];
    for (Schema.Field field : fields) {
      int pos = field.pos();
      names[pos] = field.name();
      Object val = r.get(pos);
      JMXBeanMapping mapping;
      try {
        mapping = typeMapper.get(getGenericType(field.schema()));
      } catch (NotSerializableException ex) {
        throw new UncheckedIOException(ex); //this should never happen
      }
      values[pos] = mapping.toOpenValue(val);
    }
    return new CompositeDataSupport(ctype, names, values);
  }

  private static  CompositeType typeFromSpecificRecord(final Class<? extends SpecificRecordBase> rc,
          final JMXBeanMappingSupplier typeMapper) throws NotSerializableException {
    try {
      return typeFromSpecificRecord(rc.newInstance(), typeMapper);
    } catch (InstantiationException | IllegalAccessException ex) {
      throw new UncheckedExecutionException(ex);
    }
  }


  private static  CompositeType typeFromSpecificRecord(final SpecificRecordBase r,
          final JMXBeanMappingSupplier typeMapper) throws NotSerializableException {
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
      types[pos] = typeMapper.get(getGenericType(field.schema())).getOpenType();
    }
    try {
      return new CompositeType(schema.getFullName(), schema.getDoc(),
              names, descriptions, types);
    } catch (OpenDataException ex) {
      throw new UncheckedExecutionException(ex);
    }
  }

  @Override
  public Class<?> getMappedType() {
    return getOpenClass();
  }

  @Override
  public String toString() {
    return "SpecificRecordOpenTypeMapping{" + "javaType=" + getJavaType()
            + ", openType=" + getOpenType() + '}';
  }


}
