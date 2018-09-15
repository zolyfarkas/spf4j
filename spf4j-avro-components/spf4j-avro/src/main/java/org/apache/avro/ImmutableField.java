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
package org.apache.avro;

import com.google.common.annotations.Beta;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.apache.avro.Schema.Field;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;

/**
 * @author zoly
 */
@Beta
@Immutable
public final class ImmutableField extends Field {

  private final Field wrapped;

  private ImmutableField(final Field field) {
    super(field.name(), field.schema(), field.doc(), field.defaultVal(), field.order());
    this.wrapped = field;
  }

  public static ImmutableField create(final Field field) {
      return new ImmutableField(field);
  }

  @Override
  public String toString() {
    return wrapped.toString();
  }

  @Override
  public Set<String> aliases() {
    return wrapped.aliases();
  }

  @Override
  public void addAlias(final String alias) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int pos() {
    return wrapped.pos();
  }


  @Override
  public void writeProps(final JsonGenerator gen) throws IOException {
    wrapped.writeProps(gen);
  }

  @Override
  public Map<String, Object> getObjectProps() {
    return wrapped.getObjectProps();
  }

  @Override
  public Map<String, JsonNode> getJsonProps() {
    return wrapped.getJsonProps();
  }

  @Override
  Map<String, JsonNode> jsonProps(final Map<String, String> stringProps) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, String> getProps() {
    return wrapped.getProps();
  }

  @Override
  public void addProp(final String name, final Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addProp(final String name, final JsonNode value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addProp(final String name, final String value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object getObjectProp(final String name) {
    return wrapped.getObjectProp(name);
  }

  @Override
  public JsonNode getJsonProp(final String name) {
    return wrapped.getJsonProp(name);
  }

  @Override
  @Nullable
  public String getProp(final String name) {
    return wrapped.getProp(name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.wrapped);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final ImmutableField other = (ImmutableField) obj;
    return Objects.equals(this.wrapped, other.wrapped);
  }

}
