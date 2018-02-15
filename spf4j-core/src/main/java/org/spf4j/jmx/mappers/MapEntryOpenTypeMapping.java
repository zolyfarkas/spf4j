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

import org.spf4j.jmx.JMXBeanMapping;
import com.sun.jmx.mbeanserver.MXBeanMapping;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import org.spf4j.base.ComparablePair;
import org.spf4j.base.Pair;
import org.spf4j.base.SerializablePair;
import org.spf4j.jmx.JMXBeanMappingSupplier;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings({"SCII_SPOILED_CHILD_INTERFACE_IMPLEMENTOR", "CLI_CONSTANT_LIST_INDEX"})
public final class MapEntryOpenTypeMapping extends MXBeanMapping implements JMXBeanMapping {

  private static final String[] NAMES = {"key", "value"};

  private final JMXBeanMappingSupplier typeMapper;

  private final Class<?> rawType;

  private final Type[] actualTypeArguments;

  public MapEntryOpenTypeMapping(final ParameterizedType javaType,
          final JMXBeanMappingSupplier typeMapper) throws NotSerializableException {
    super(javaType, typeFromMapEntry(javaType, typeMapper));
    this.typeMapper = typeMapper;
    rawType = (Class) javaType.getRawType();
    actualTypeArguments = javaType.getActualTypeArguments();
  }

  @Override
  @SuppressFBWarnings("URV_INHERITED_METHOD_WITH_RELATED_TYPES")
  public Object fromOpenValue(final Object openValue) throws InvalidObjectException {
    if (!(openValue instanceof CompositeData)) {
      throw new InvalidObjectException("Not a CompositeData " + openValue);
    }
    CompositeData cd = (CompositeData) openValue;
    try {
      if (rawType == Pair.class) {
        return Pair.of(typeMapper.get(actualTypeArguments[0]).fromOpenValue(cd.get("key")),
                typeMapper.get(actualTypeArguments[1]).fromOpenValue(cd.get("value")));
      } else if (rawType == SerializablePair.class) {
        return SerializablePair.of((Serializable) typeMapper.get(actualTypeArguments[0]).fromOpenValue(cd.get("key")),
                (Serializable) typeMapper.get(actualTypeArguments[1]).fromOpenValue(cd.get("value")));
      } else if (rawType == ComparablePair.class) {
        return new ComparablePair(
                (Serializable & Comparable) typeMapper.get(actualTypeArguments[0]).fromOpenValue(cd.get("key")),
                (Serializable & Comparable) typeMapper.get(actualTypeArguments[1]).fromOpenValue(cd.get("value")));
      } else {
        return Pair.of(typeMapper.get(actualTypeArguments[0]).fromOpenValue(cd.get("key")),
                typeMapper.get(actualTypeArguments[1]).fromOpenValue(cd.get("value")));
      }
    } catch (NotSerializableException ex) {
      InvalidObjectException iex = new InvalidObjectException("Not serializable " + openValue);
      iex.initCause(ex);
      throw iex;
    }
  }

  @Override
  public Object toOpenValue(final Object javaValue) throws OpenDataException {
    Map.Entry entry = (Map.Entry) javaValue;
    try {
      return new CompositeDataSupport((CompositeType) getOpenType(), NAMES,
              new Object[]{typeMapper.get(actualTypeArguments[0]).toOpenValue(entry.getKey()),
                typeMapper.get(actualTypeArguments[1]).toOpenValue(entry.getValue())});
    } catch (NotSerializableException ex) {
      OpenDataException iex = new OpenDataException("Not serializable " + javaValue);
      iex.initCause(ex);
      throw iex;
    }
  }

  private static CompositeType typeFromMapEntry(final ParameterizedType type,
          final JMXBeanMappingSupplier typeMapper) throws NotSerializableException {
    Type[] typeArgs = type.getActualTypeArguments();
    String[] descriptions = new String[2];
    OpenType<?>[] types = new OpenType<?>[2];
    descriptions[0] = "key of " + type;
    Type typeArg0 = typeArgs[0];
    JMXBeanMapping km = typeMapper.get(typeArg0);
    if (km == null) {
      throw new IllegalArgumentException("Cannot map to open type " + typeArg0);
    }
    types[0] = km.getOpenType();
    descriptions[1] = "value of " + type;
    Type typeArg1 = typeArgs[1];
    JMXBeanMapping km2 = typeMapper.get(typeArg1);
    if (km2 == null) {
      throw new IllegalArgumentException("Cannot map to open type " + typeArg1);
    }
    types[1] = km2.getOpenType();
    try {
      return new CompositeType(type.getTypeName(), type.toString(),
              NAMES, descriptions, types);
    } catch (OpenDataException ex) {
     throw new IllegalArgumentException(ex);
    }
  }

  @Override
  public Class<?> getMappedType() {
    return getOpenClass();
  }

  @Override
  public String toString() {
    return "MapEntryOpenTypeMapping{" + "javaType=" + getJavaType()
            + ", openType=" + getOpenType() + '}';
  }

}
