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

import com.google.common.util.concurrent.UncheckedExecutionException;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.util.Map;
import javax.annotation.Nullable;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;

/**
 *
 * @author zoly
 */
final class MapExportedValue implements ExportedValue {

  private final Map<String, Object> map;
  private final Map<String, String> descriptions;
  private final String name;
  private final JMXBeanMapping converter;

  MapExportedValue(final Map<String, Object> map, final Map<String, String> descriptions,
          final String name, @Nullable final Object value) throws NotSerializableException {
    this.map = map;
    this.descriptions = descriptions;
    this.name = name;
    if (value == null) {
      this.converter = null;
    } else {
      this.converter = GlobalMXBeanMapperSupplier.getOpenTypeMapping(value.getClass());
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    if (descriptions != null) {
      return descriptions.get(name);
    } else {
      return "";
    }
  }

  @Override
  public Object get() throws OpenDataException {
    Object val = map.get(name);
    if (converter == null) {
      return val;
    } else {
      return converter.toOpenValue(val);
    }
  }

  @Override
  public void set(final Object value) throws InvalidObjectException {
    if (converter == null) {
      map.put(name, value);
    } else {
      map.put(name, converter.fromOpenValue(value));
    }
  }

  @Override
  public boolean isWriteable() {
    return true;
  }

  @Override
  public Class getValueType() {
    Object obj = map.get(name);
    if (obj == null) {
      return String.class;
    } else {
      return obj.getClass();
    }
  }

  @Override
  public String toString() {
    try {
      return "MapExportedValue{" + "val=" + get() + "valClass=" + getValueType()
              + "valopenType=" + getValueOpenType() + ", description="
              + getDescription() + ", name=" + name + ", converter=" + converter + '}';
    } catch (OpenDataException ex) {
      throw new UncheckedExecutionException(ex);
    }
  }

  @Override
  @Nullable
  public OpenType getValueOpenType() {
    return (converter != null) ? converter.getOpenType() : null;
  }

}
