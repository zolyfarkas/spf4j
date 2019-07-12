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
package org.spf4j.io.appenders;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.spf4j.base.CoreTextMediaType;
import org.spf4j.io.ObjectAppender;
import org.spf4j.io.ObjectAppenderSupplier;

/**
 *
 * @author zoly
 */
public final class MapAppender implements ObjectAppender<Map<Object, Object>> {

  @Override
  public CoreTextMediaType getAppendedType() {
    return CoreTextMediaType.TEXT_PLAIN;
  }

  @Override
  public void append(final Map<Object, Object> map,
          final Appendable appendTo, final ObjectAppenderSupplier appenderSupplier)
       throws IOException {
    appendTo.append('[');
    Set<Map.Entry<Object, Object>> entrySet = map.entrySet();
    Iterator<Map.Entry<Object, Object>> it = entrySet.iterator();
    if (it.hasNext()) {
      Map.Entry<Object, Object> o = it.next();
      appendEntry(o, appendTo, appenderSupplier);
      while (it.hasNext()) {
        o = it.next();
        appendTo.append(',');
        appendEntry(o, appendTo, appenderSupplier);
      }
    }
    appendTo.append(']');
  }

  private static void appendEntry(final Map.Entry<Object, Object> o, final Appendable appendTo,
          final ObjectAppenderSupplier appenderSupplier) throws IOException {
    Object key = o.getKey();
    Object value = o.getValue();
    appendTo.append('(');
    ObjectAppender.appendNullable(key, appendTo, appenderSupplier);
    appendTo.append(',');
    ObjectAppender.appendNullable(value, appendTo, appenderSupplier);
    appendTo.append(')');
  }

  @Override
  public void append(final Map<Object, Object> object, final Appendable appendTo) throws IOException {
    appendTo.append(object.toString());
  }

}
