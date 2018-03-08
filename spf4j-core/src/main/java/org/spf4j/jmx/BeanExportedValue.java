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

import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.management.InvalidAttributeValueException;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;

/**
 *
 * @author zoly
 */
class BeanExportedValue implements ExportedValue<Object> {

  private final String name;
  private final String description;
  private final Method getMethod;
  private final Method setMethod;
  private final Object object;
  private final Type valueClass;
  private final JMXBeanMapping converter;

  BeanExportedValue(@Nonnull final String name, @Nullable final String description,
          @Nullable final Method getMethod, @Nullable final Method setMethod,
          @Nullable final Object object, @Nonnull final Type valueClass, final boolean mapOpenType) {
    this.name = name;
    this.description = description;
    this.getMethod = getMethod;
    this.setMethod = setMethod;
    this.object = object;
    this.valueClass = valueClass;
    if (mapOpenType) {
      try {
        this.converter = GlobalMXBeanMapperSupplier.getOpenTypeMapping(valueClass);
      } catch (NotSerializableException ex) {
        throw new UnsupportedOperationException("Unable to export " + getMethod + ", " + setMethod, ex);
      }
    } else {
      this.converter = null;
    }
  }

  public BeanExportedValue withSetter(@Nonnull final Method psetMethod) {
    if (setMethod != null) {
      throw new IllegalArgumentException("duplicate value registration attemted " + setMethod
              + ", " + psetMethod);
    }
    return new BeanExportedValue(name, description, getMethod, psetMethod, object, valueClass, converter != null);
  }

  public BeanExportedValue withGetter(@Nonnull final Method pgetMethod, @Nonnull final String pdescription) {
    if (getMethod != null) {
      throw new IllegalArgumentException("duplicate value registration attemted " + getMethod
              + ", " + pgetMethod);
    }
    return new BeanExportedValue(name, pdescription, pgetMethod, setMethod, object, valueClass, converter != null);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public Object get() throws OpenDataException {

    try {
      if (converter != null) {
        return converter.toOpenValue(getMethod.invoke(object));
      } else {
        return getMethod.invoke(object);
      }
    } catch (IllegalAccessException | InvocationTargetException ex) {
      OpenDataException thr = new OpenDataException("Cannot get " + getMethod);
      thr.addSuppressed(ex);
      throw thr;
    }
  }

  @Override
  public void set(final Object value) throws InvalidAttributeValueException, InvalidObjectException {
    if (setMethod == null) {
      throw new InvalidAttributeValueException(name + " is a read only attribute ");
    }
    try {
      if (converter != null) {
        setMethod.invoke(object, converter.fromOpenValue(value));
      } else {
        setMethod.invoke(object, value);
      }
    } catch (IllegalAccessException | InvocationTargetException ex) {
      InvalidObjectException iox = new InvalidObjectException("Cannot set " + value);
      iox.addSuppressed(ex);
      throw iox;
    }
  }

  @Override
  public boolean isWriteable() {
    return setMethod != null;
  }

  @Override
  public Type getValueType() {
    return valueClass;
  }

  public boolean isValid() {
    return getMethod != null;
  }

  @Override
  public String toString() {
    return "ExportedValueImpl{" + "name=" + name + ", description="
            + description + ", getMethod=" + getMethod + ", setMethod="
            + setMethod + ", object=" + object + ", valueClass=" + valueClass
            + ", converter=" + converter + '}';
  }



  @Override
  @Nullable
  public OpenType<?> getValueOpenType() {
    return (converter != null) ? converter.getOpenType() : null;
  }

}
