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
package org.spf4j.text;

import java.text.ChoiceFormat;
import java.text.DateFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.spf4j.base.Arrays;
import org.spf4j.base.Slf4jMessageFormatter;

/**
 * @author Zoltan Farkas
 */
final class Slf4jFormatImpl implements Slf4jFormat {

  private final String format;

  private final ParamConverter[] converters;

  public interface ParamConverter {

    Object convert(BitSet used, Object... args);

  }

  static final class FormatInfoParameterConverter implements ParamConverter {

    private final FormatInfo finfo;

    private final Locale locale;

    FormatInfoParameterConverter(final FormatInfo finfo, final Locale locale) {
      this.finfo = finfo;
      this.locale = locale;
    }

    @Override
    public Object convert(final BitSet used, final Object... args) {
      int argumentNumber = finfo.getArgumentNumber();
      used.set(argumentNumber);
      Object obj = args[argumentNumber];
      Object arg = null;
      Format fmt = finfo.getFormat();
      if (obj == null) {
        arg = "null";
      } else if (fmt != null) {
        Format subFormatter = fmt;
        if (subFormatter instanceof ChoiceFormat) {
          String strArg = subFormatter.format(obj);
          if (strArg.indexOf('{') >= 0) {
            MessageFormat mformat = new MessageFormat(strArg, locale);
            Slf4jFormat sfmt = mformat.subformatSlf4j();
            String slf4jFmt = sfmt.getFormat();
            Object[] converted = sfmt.convert(used, args);
            arg = Slf4jFormat.lazyString(() -> Slf4jMessageFormatter.toString(slf4jFmt, converted));
          } else {
            arg = Slf4jFormat.lazyString(() -> subFormatter.format(obj));
          }
        } else {
          arg = Slf4jFormat.lazyString(() -> subFormatter.format(obj));
        }
      } else if (obj instanceof Number) {
        // format number if can
        Format subFormatter = NumberFormat.getInstance(locale);
        arg = Slf4jFormat.lazyString(() -> subFormatter.format(obj));
      } else if (obj instanceof Date) {
        // format a Date if can
        Format subFormatter = DateFormat.getDateTimeInstance(
                DateFormat.SHORT, DateFormat.SHORT, locale);
        arg = Slf4jFormat.lazyString(() -> subFormatter.format(obj));
      } else {
        arg = obj;
      }
      return arg;
    }

  }

  Slf4jFormatImpl(final String format, final ParamConverter[] converters) {
    this.format = format;
    this.converters = converters;
  }

  @Override
  public String getFormat() {
    return format;
  }

  @Override
  public Object[] convert(final Object... parameters) {
    if (parameters.length == 0) {
      return Arrays.EMPTY_OBJ_ARRAY;
    }
    BitSet used = new BitSet(parameters.length);
    return convert(used, parameters);
  }

  @Override
  public Object[] convert(final BitSet used, final Object... parameters) {
    int l = parameters.length;
    List<Object> result = new ArrayList<>(l + 1);
    for (ParamConverter converter : converters) {
      result.add(converter.convert(used, parameters));
    }
    int at = 0;
    int notUsedIdx;
    while ((notUsedIdx = used.nextClearBit(at)) < l) {
      result.add(parameters[notUsedIdx]);
      at = notUsedIdx + 1;
    }
    return result.toArray(new Object[result.size()]);
  }

  @Override
  public String toString() {
    return "Slf4jFormatImpl{" + "format=" + format + ", converters=" + java.util.Arrays.toString(converters) + '}';
  }

}
