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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;

/**
 * @author zoly
 */
//CHECKSTYLE IGNORE EqualsHashCode FOR NEXT 1000 LINES
@Immutable
public final class ImmutableSchema extends Schema {

  private final Schema wrapped;

  private ImmutableSchema(final Schema schema) {
    super(schema.getType());
    this.wrapped = schema;
  }

  public static ImmutableSchema create(final Schema schema) {
    if (schema instanceof ImmutableSchema) {
      return (ImmutableSchema) schema;
    }
    return new ImmutableSchema(schema);
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
    final ImmutableSchema other = (ImmutableSchema) obj;
    return Objects.equals(this.wrapped, other.wrapped);
  }

  @Override
  int computeHash() {
    return wrapped.hashCode();
  }

  @Override
  public String toString() {
    return wrapped.toString();
  }

  @Override
  public int getFixedSize() {
    return wrapped.getFixedSize();
  }

  @Override
  public Integer getIndexNamed(final String name) {
    return wrapped.getIndexNamed(name);
  }

  @Override
  public List<Schema> getTypes() {
    return wrapped.getTypes();
  }

  @Override
  public Schema getValueType() {
    return wrapped.getValueType();
  }

  @Override
  public Schema getElementType() {
    return wrapped.getElementType();
  }

  @Override
  public boolean isError() {
    return wrapped.isError();
  }

  @Override
  public Set<String> getAliases() {
    return wrapped.getAliases();
  }

  @Override
  public void addAlias(final String alias, final String space) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addAlias(final String alias) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getFullName() {
    return wrapped.getFullName();
  }

  @Override
  public String getNamespace() {
    return wrapped.getNamespace();
  }

  @Override
  @Nullable
  public String getDoc() {
    return wrapped.getDoc();
  }

  @Override
  public String getName() {
    return wrapped.getName();
  }

  @Override
  public boolean hasEnumSymbol(final String symbol) {
    return wrapped.hasEnumSymbol(symbol);
  }

  @Override
  public int getEnumOrdinal(final String symbol) {
    return wrapped.getEnumOrdinal(symbol);
  }

  @Override
  public List<String> getEnumSymbols() {
    return super.getEnumSymbols();
  }

  @Override
  public void setFields(final List<Field> fields) {
    try {
      wrapped.setFields(fields);
    } catch (AvroRuntimeException ex) {
      throw new UnsupportedOperationException(ex);
    }
  }

  @Override
  public List<Field> getFields() {
    return wrapped.getFields();
  }

  @Override
  public Field getField(final String fieldname) {
    return wrapped.getField(fieldname);
  }

  @Override
  public LogicalType getLogicalType() {
    return wrapped.getLogicalType();
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
  public Map<String, Object> getObjectProps() {
    return wrapped.getObjectProps();
  }

  @Override
  public Map<String, JsonNode> getJsonProps() {
    return wrapped.getJsonProps();
  }

  @Override
  public Map<String, String> getProps() {
    return wrapped.getProps();
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
  public void fieldsToJson(final Names names, final JsonGenerator gen) throws IOException {
    wrapped.fieldsToJson(names, gen);
  }

  @Override
  void toJson(final Names names, final JsonGenerator gen) throws IOException {
    wrapped.toJson(names, gen);
  }

  @Override
  public String toString(final boolean pretty) {
    return wrapped.toString(pretty);
  }

  @Override
  void setLogicalType(final LogicalType logicalType) {
    throw new UnsupportedOperationException();
  }

  @Override
  void writeProps(final JsonGenerator gen) throws IOException {
    wrapped.writeProps(gen);
  }

  @Override
  Map<String, JsonNode> jsonProps(final Map<String, String> stringProps) {
    throw new UnsupportedOperationException();
  }




}
