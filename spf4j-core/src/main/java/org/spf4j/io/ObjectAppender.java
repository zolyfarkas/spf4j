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
package org.spf4j.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ConcurrentModificationException;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.base.CoreTextMediaType;
import org.spf4j.base.EscapeJsonStringAppendableWrapper;

/**
 * @author zoly
 * @param <T> - type of object to append.
 */
@ParametersAreNonnullByDefault
@FunctionalInterface
public interface ObjectAppender<T> extends BiConsumer<T, Appendable> {

  /**
   * the MimeType of the format used to write the Object.
   * @return
   */
  default CoreTextMediaType getAppendedType() {
    return CoreTextMediaType.TEXT_PLAIN;
  }

  /**
   * Write an Object to a char stream.
   * @param object
   * @param appendTo
   * @throws IOException
   */
  void append(T object, Appendable appendTo) throws IOException;


  static void appendNullable(@Nullable final Object o, final Appendable appendTo,
          final ObjectAppenderSupplier appenderSupplier)
          throws IOException {
    if (o == null) {
      appendTo.append("null");
    } else {
      appenderSupplier.get(o.getClass()).append(o, appendTo, appenderSupplier);
    }
  }

  static void appendNullableJson(@Nullable final Object o, final Appendable appendTo,
          final ObjectAppenderSupplier appenderSupplier)
          throws IOException {
    if (o == null) {
      appendTo.append("null");
    } else {
      ObjectAppender app = appenderSupplier.get(CoreTextMediaType.APPLICATION_JSON, o.getClass());
      if (app != null) {
         app.append(o, appendTo, appenderSupplier);
      } else {
        app = appenderSupplier.get(CoreTextMediaType.TEXT_PLAIN, o.getClass());
        EscapeJsonStringAppendableWrapper sEsc = new EscapeJsonStringAppendableWrapper(appendTo);
        appendTo.append('"');
        app.append(o, sEsc, appenderSupplier);
        appendTo.append('"');
      }
    }
  }


  /**
   * Method to overwrite for implementing apenders for container objects.
   */
  default void append(T object, Appendable appendTo, ObjectAppenderSupplier appenderSupplier) throws IOException {
    append(object, appendTo);
  }

  default void accept(final T object, final Appendable appendTo) {
    try {
      append(object, appendTo);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /**
   * A simple Object appender that invokes the toString method of the object and writes the object out.
   */
  ObjectAppender<Object> TOSTRING_APPENDER = new ObjectAppender<Object>() {
    @Override
    public void append(final Object object, final Appendable appendTo) throws IOException {

      String toString = null;
      int i = 10;
      do {
        try {
          toString = object.toString();
        } catch (ConcurrentModificationException ex) {
          i--;
        }
      } while (toString == null && i > 0);
      if (i != 10) {
        appendTo.append("ConcurrentlyModifiedDuringToString:");
      }
      if (toString == null) {
        appendTo.append(object.getClass().getName()).append('@')
                .append(Integer.toHexString(System.identityHashCode(object)));
      } else {
        appendTo.append(toString);
      }
    }

  };

}
