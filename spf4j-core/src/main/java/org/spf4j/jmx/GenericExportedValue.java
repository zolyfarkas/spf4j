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
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import org.spf4j.base.UncheckedExecutionException;

/**
 *
 * @author zoly
 */
public final class GenericExportedValue<T> implements ExportedValue {

  private final String name;
  private final String description;
  private final Supplier<T> getter;
  private final Consumer<T> setter;
  private final Class<T> clasz;
  private final JMXBeanMapping converter;
  private final OpenType<?> openType;

  public GenericExportedValue(
          @Nonnull final String name, @Nonnull final String description,
          @Nonnull final Supplier<T> getter, @Nullable final Consumer<T> setter,
          final Class<T> clasz) throws NotSerializableException {
    this.description = description;
    this.name = name;
    this.getter = getter;
    this.setter = setter;
    this.clasz = clasz;
    this.converter = GlobalMXBeanMapperSupplier.getOpenTypeMapping(clasz);
    this.openType = this.converter == null ? null : this.converter.getOpenType();
  }

  @SuppressWarnings("unchecked")
  public GenericExportedValue(
          @Nonnull final String name, @Nonnull final String description,
          @Nonnull final Supplier<CompositeData> getter,
          @Nullable final Consumer<CompositeData> setter,
          final CompositeType openType) {
    this.description = description;
    this.name = name;
    this.getter = (Supplier) getter;
    this.setter = (Consumer) setter;
    this.clasz = (Class) CompositeData.class;
    this.converter = null;
    this.openType = openType;
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
    Object val = getter.get();
    if (converter == null) {
      return val;
    } else {
      return converter.toOpenValue(val);
    }
  }

  @Override
  public void set(final Object value) throws InvalidObjectException {
    if (converter == null) {
      setter.accept((T) value);
    } else {
      setter.accept((T) converter.fromOpenValue(value));
    }
  }

  @Override
  public boolean isWriteable() {
    return setter != null;
  }

  @Override
  public Class getValueType() {
    return clasz;
  }

  @Override
  public String toString() {
    try {
      return "GenericExportedValue{" + "val=" + get() + "valClass=" + getValueType()
              + "valopenType=" + getValueOpenType() + ", description="
              + getDescription() + ", name=" + name + ", converter=" + converter + '}';
    } catch (OpenDataException ex) {
      throw new UncheckedExecutionException(ex);
    }
  }



  @Override
  public OpenType getValueOpenType() {
    return openType;
  }

}
