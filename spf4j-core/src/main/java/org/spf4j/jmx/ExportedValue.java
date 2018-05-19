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
import java.lang.reflect.Type;
import javax.annotation.Nullable;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenType;
import org.spf4j.base.Reflections;

public interface ExportedValue<T> {

  String getName();

  String getDescription();

  T get() throws OpenDataException;

  void set(T value) throws InvalidAttributeValueException, InvalidObjectException;

  boolean isWriteable();

  Type getValueType();

  @Nullable
  OpenType<?> getValueOpenType();

  default MBeanAttributeInfo toAttributeInfo() {
    final Type oClass = this.getValueType();
    Class<?> valClass = oClass instanceof Class ? Reflections.primitiveToWrapper((Class) oClass) : null;
    OpenType openType = this.getValueOpenType();
    String description = this.getDescription();
    if (description == null || description.isEmpty()) {
      description = this.getName();
    }
    if (openType != null) {
      try {
        return new OpenMBeanAttributeInfoSupport(this.getName(), description,
                openType, true, this.isWriteable(), valClass == Boolean.class);
      } catch (IllegalArgumentException ex) {
        throw new IllegalArgumentException("Cannot export " + this, ex);
      }
    } else {
      return new MBeanAttributeInfo(
              this.getName(),
              oClass.getTypeName(),
              this.getDescription(),
              true, // isReadable
              this.isWriteable(), // isWritable
              valClass == Boolean.class);
    }

  }

}
