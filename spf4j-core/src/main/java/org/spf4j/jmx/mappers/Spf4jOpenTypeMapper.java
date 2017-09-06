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
import java.io.NotSerializableException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.management.ObjectName;
import javax.management.openmbean.OpenDataException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Pair;
import org.spf4j.jmx.JMXBeanMapping;
import org.spf4j.jmx.JMXBeanMappingSupplier;


/**
 * @author Zoltan Farkas
 */
public final class Spf4jOpenTypeMapper implements JMXBeanMappingSupplier {

  private static final Logger LOG = LoggerFactory.getLogger(Spf4jOpenTypeMapper.class);

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


  private final Map<Type, JMXBeanMapping> convertedTypes
          = new ConcurrentHashMap<>();

  public Spf4jOpenTypeMapper() {
    try {
      for (final Pair<OpenType<?>, Class<?>[]> t : SIMPLE_TYPES) {
        for (Class<?> c : t.getSecond()) {
          convertedTypes.put(c, Spf4jJMXBeanMapping.newBasicType(c, t.getFirst()));
        }
      }
    } catch (OpenDataException e) {
      throw new AssertionError(e);
    }
  }


  @Override
  public JMXBeanMapping get(final Type t) throws NotSerializableException {
    JMXBeanMapping mt = convertedTypes.get(t);
    if (mt == null) {
      try {
        mt =  Spf4jJMXBeanMapping.newMappedType(t, this);
        if (mt == null) {
          mt = JMXBeanMapping.NOMAPPING;
        }
        convertedTypes.put(t, mt);
      } catch (OpenDataException ex) {
        mt = JMXBeanMapping.NOMAPPING;
      }
    }
    if (mt != JMXBeanMapping.NOMAPPING && mt.getOpenType() == Spf4jJMXBeanMapping.IN_PROGRESS) {
      LOG.debug("No openType for {}, recursive data structure", t);
      return null;
    }
    if (mt == JMXBeanMapping.NOMAPPING) {
      TypeToken<?> tt = TypeToken.of(t);
      if (tt.isSubtypeOf(Serializable.class)) {
        LOG.debug("No openType mapping for {}", t);
        return null;
      } else {
        throw new NotSerializableException(t + " must be serializable to be exported via JMX");
      }
     }
    return mt;
  }

  @Override
  public String toString() {
    return "Spf4jOpenTypeMapper{" + "convertedTypes=" + convertedTypes + '}';
  }

}
