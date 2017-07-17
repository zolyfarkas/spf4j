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

import org.spf4j.io.Csv;
import com.google.common.base.Objects;
import java.beans.ConstructorProperties;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 *
 * @author zoly
 */
public class Pair<A, B> implements Map.Entry<A, B>, Writeable {

  //CHECKSTYLE:OFF
  @Nullable
  protected final A first;

  @Nullable
  protected final B second;
  //CHECKSTYLE:ON

  @ConstructorProperties({"first", "second"})
  public Pair(@Nullable final A first, @Nullable final B second) {
    this.first = first;
    this.second = second;
  }

  public static <A, B> Pair<A, B> of(@Nullable final A first, @Nullable final B second) {
    return new Pair<>(first, second);
  }

  /**
   * Creates a pair from a str1,str2 pair.
   *
   * @param stringPair - pair in the format (a,b) csv pair.
   * @return null if this is not a pair.
   */
  @Nullable
  public static Pair<String, String> from(@Nonnull final String stringPair) {
    if (stringPair.isEmpty()) {
      return null;
    }
    int commaIdx = stringPair.indexOf(',');
    if (commaIdx < 0) {
      return null;
    }
    StringReader sr = new StringReader(stringPair);
    StringBuilder first = new StringBuilder();
    StringBuilder second = new StringBuilder();
    int comma;
    try {
      comma = Csv.readCsvElement(sr, first);
      if (comma != ',') {
        return null;
      }
      int last = Csv.readCsvElement(sr, second);
      if (last >= 0) {
        return null;
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return Pair.of(first.toString(), second.toString());
  }

  public final A getFirst() {
    return first;
  }

  public final B getSecond() {
    return second;
  }

  @Override
  public final boolean equals(final Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Pair<A, B> other = (Pair<A, B>) obj;
    if (this.first != other.first && (this.first == null || !this.first.equals(other.first))) {
      return false;
    }
    return (!(this.second != other.second && (this.second == null || !this.second.equals(other.second))));
  }

  @Override
  public final int hashCode() {
    return Objects.hashCode(first, second);
  }

  @Override
  public final String toString() {
    StringBuilder result = new StringBuilder(32);
    writeTo(result);
    return result.toString();
  }

  @Override
  public final void writeTo(final Appendable appendable) throws IOException {
    Csv.writeCsvElement(first == null ? "" : first.toString(), appendable);
    appendable.append(',');
    Csv.writeCsvElement(second == null ? "" : second.toString(), appendable);
  }

  public final List<Object> toList() {
    return java.util.Arrays.asList(first, second);
  }

  public static <K, V extends Object> Map<K, V> asMap(final Pair<K, ? extends V>... pairs) {
    Map<K, V> result = new LinkedHashMap<>(pairs.length);
    for (Pair<K, ? extends V> pair : pairs) {
      result.put(pair.getFirst(), pair.getSecond());
    }
    return result;
  }

  @Override
  public final A getKey() {
    return first;
  }

  @Override
  public final B getValue() {
    return second;
  }

  @Override
  public final B setValue(final B value) {
    throw new UnsupportedOperationException("Object not mutable");
  }

}
