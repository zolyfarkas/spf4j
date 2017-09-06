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
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

/**
 *
 * @author Zoltan Farkas
 */
public interface JMXBeanMapping {

  /**
   * convert from open value.
   * @param openValue
   * @return
   */
  Object fromOpenValue(Object openValue) throws InvalidObjectException;

  /**
   * convert to open value.
   * @param javaValue
   * @return
   */
  Object toOpenValue(Object javaValue) throws OpenDataException;

  /**
   * <p>
   * The Java type the open type mapping is mapped to</p>
   *
   * @return the Java type that the open type mapping is mapped to.
   */
  Type getJavaType();

  /**
   * Jet the mapped java type.
   * @return
   */
  Class<?> getMappedType();


  /**
   * <p>
   * The Open Type.</p>
   *
   * @return the Open Type.
   */
  OpenType<?> getOpenType();


  /**
   * A type where the open type class and the java class are the same.
   * @return
   */
  default boolean isSimpleType() {
    return getOpenType() instanceof SimpleType;
  }


  JMXBeanMapping NOMAPPING = new JMXBeanMapping() {
    @Override
    public Object fromOpenValue(final Object openValue) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object toOpenValue(final Object javaValue) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Type getJavaType() {
      throw new UnsupportedOperationException();
    }

    @Override
    public OpenType<?> getOpenType() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Class<?> getMappedType() {
      throw new UnsupportedOperationException();
    }

  };

}
