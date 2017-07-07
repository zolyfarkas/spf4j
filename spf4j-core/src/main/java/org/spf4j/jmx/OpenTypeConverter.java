/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
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
 */
package org.spf4j.jmx;

import com.google.common.base.Converter;
import com.sun.jmx.mbeanserver.MXBeanMapping;
import com.sun.jmx.mbeanserver.MXBeanMappingFactory;
import java.io.InvalidObjectException;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;

/**
 *
 * @author zoly
 */
// FB does not like guava Convertere equals/hashcode.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings("HE_INHERITS_EQUALS_USE_HASHCODE")
public final class OpenTypeConverter {

  private OpenTypeConverter() {
  }


  /**
   * returns MXBeanMapping
   */
  private static MXBeanMapping getMXBeanMapping(final Class<?> type) {
    try {
      return MXBeanMappingFactory.DEFAULT.mappingForType(type, MXBeanMappingFactory.DEFAULT);
    } catch (OpenDataException ex) {
      throw new UnsupportedOperationException("Unsupported MX bean mapping for " + type, ex);
    }
  }

  public static OpenType<?> getOpenType(final Class<?> type) {
    return getMXBeanMapping(type).getOpenType();
  }

  /**
   *
   * @param type the java type to from to convert to.
   * @return Converter - the guava converter for the type.
   */
  public static Converter<Object, Object> getConverter(final Class<?> type) {

    final MXBeanMapping mapping = getMXBeanMapping(type);
    return new Converter<Object, Object>() {

      @Override
      protected Object doForward(final Object a) {
        try {
          return mapping.fromOpenValue(a);
        } catch (InvalidObjectException ex) {
          throw new IllegalArgumentException("Open value canot be converted: " + a, ex);
        }
      }

      @Override
      protected Object doBackward(final Object b) {
        try {
          return mapping.toOpenValue(b);
        } catch (OpenDataException ex) {
          throw new IllegalArgumentException("Value cannot be converted to open value: " + b, ex);
        }
      }
    };

  }

}
