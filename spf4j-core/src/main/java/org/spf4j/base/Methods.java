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
package org.spf4j.base;

import com.google.common.escape.Escaper;
import com.google.common.html.HtmlEscapers;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gnu.trove.map.hash.THashMap;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import javax.annotation.concurrent.Immutable;
import org.spf4j.base.avro.Converters;
import org.spf4j.base.avro.Method;

/**
 * @author zoly
 */
@Immutable
// using racy single check idiom makes findbugs think the Method obejct is mutable...
@SuppressFBWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
public final class Methods  {

  public static final Method ROOT = Converters.ROOT;

  private static final Map<String, Map<String, Method>> INSTANCE_REPO = new THashMap<>(1024);


  private Methods() { }

  public static void writeHtml(final Method m, final Writer w) throws IOException {
    Escaper htmlEscaper = HtmlEscapers.htmlEscaper();
    w.append(htmlEscaper.escape(m.getName())).append(htmlEscaper.escape("@")).
            append(htmlEscaper.escape(m.getDeclaringClass()));
  }

  public static Method getMethod(final StackTraceElement elem) {
    return getMethod(elem.getClassName(), elem.getMethodName());
  }

  /*
     * this function is to allow reuse of Method instances.
     * not thread safe, use with care, see description for suppressed findbugs bug for more detail.
   */
  @SuppressFBWarnings("PMB_POSSIBLE_MEMORY")
  public static synchronized Method getMethod(final String className, final String methodName) {
    Map<String, Method> mtom = INSTANCE_REPO.get(className);
    Method result;
    if (mtom == null) {
      mtom = new THashMap<>(5);
      result = new Method(className, methodName);
      mtom.put(methodName, result);
      INSTANCE_REPO.put(className, mtom);
    } else {
      result = mtom.get(methodName);
      if (result == null) {
        result = new Method(className, methodName);
        mtom.put(methodName, result);
      }
    }
    return result;
  }

  public static Method annotate(final Method m, String annotationPrefix) {
    return new Method(annotationPrefix + ':' + m.getDeclaringClass(), m.getName());
  }

  public static void writeTo(final Method m, final Appendable w) throws IOException {
    w.append(m.getName()).append('@').append(m.getDeclaringClass());
  }

  public static void writeTo(final Method m, final StringBuilder w) {
    w.append(m.getName()).append('@').append(m.getDeclaringClass());
  }

  public static CharSequence toCharSequence(final Method m) {
    StringBuilder sb = new StringBuilder(32);
    writeTo(m, sb);
    return sb;
  }

  public static String toString(final Method m) {
    return toCharSequence(m).toString();
  }

  public static Method from(final CharSequence cs) {
    return from(cs, 0, cs.length());
  }

  public static Method from(final CharSequence cs, final int start, final int end) {
    int idx = CharSequences.indexOf(cs, start, end, '@');
    if (idx < 0) {
      throw new IllegalArgumentException("Invalid method representation: " + cs);
    }
    return getMethod(cs.subSequence(idx + 1, end).toString(), cs.subSequence(start, idx).toString());
  }

}
