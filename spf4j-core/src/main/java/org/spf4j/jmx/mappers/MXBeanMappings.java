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

import com.sun.jmx.mbeanserver.MXBeanMapping;
import java.io.InvalidObjectException;
import java.lang.reflect.Type;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import org.spf4j.jmx.JMXBeanMapping;

/**
 * @author Zoltan Farkas
 */
public final class MXBeanMappings {

  private MXBeanMappings() { }

  public static JMXBeanMapping convert(final MXBeanMapping mapping) {
    return new JMXBeanMapping() {
      @Override
      public Object fromOpenValue(final Object openValue) throws InvalidObjectException {
        return mapping.fromOpenValue(openValue);
      }

      @Override
      public Object toOpenValue(final Object javaValue) throws OpenDataException {
        return mapping.toOpenValue(javaValue);
      }

      @Override
      public Type getJavaType() {
        return mapping.getJavaType();
      }

      @Override
      public OpenType<?> getOpenType() {
        return mapping.getOpenType();
      }

      @Override
      public Class<?> getMappedType() {
        return mapping.getOpenClass();
      }
    };
  }

  public static MXBeanMapping convert(final JMXBeanMapping mapping) {
    return new MXBeanMapping(mapping.getJavaType(), mapping.getOpenType()) {
      @Override
      public Object fromOpenValue(final Object openValue) throws InvalidObjectException {
        return mapping.fromOpenValue(openValue);
      }

      @Override
      public Object toOpenValue(final Object javaValue) throws OpenDataException {
        return mapping.toOpenValue(javaValue);
      }
    };
  }

  public static Class<?> makeOpenClass(final Type javaType, final OpenType<?> openType) {
    if (javaType instanceof Class<?> && ((Class<?>) javaType).isPrimitive()) {
      return (Class<?>) javaType;
    }
    try {
      String className = openType.getClassName();
      return Class.forName(className, false, MXBeanMapping.class.getClassLoader());
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Cannot get OpenType " + openType, e);
    }
  }

}
