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
package org.spf4j.avro.schema;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.BiConsumer;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import static org.apache.avro.Schema.Type.RECORD;
import org.apache.avro.ImmutableSchema;

/**
 * this visitor will create a clone of the original Schema with docs and other nonesential fields stripped
 * by default. what attributes are copied is customizable.
 * @author zoly
 */
public final class ImmutableCloningVisitor implements SchemaVisitor<ImmutableSchema> {

  private final IdentityHashMap<Schema, ImmutableSchema> replace = new IdentityHashMap<>();

  private final Schema root;

  private final BiConsumer<Field, Field> copyField;
  private final BiConsumer<Schema, Schema> copySchema;
  private final boolean copyDocs;

  public ImmutableCloningVisitor(final Schema root, final boolean serializationSignificatOnly) {
    this(serializationSignificatOnly ? SchemaUtils.FIELD_ESENTIALS : SchemaUtils.FIELD_EVERYTHING,
            serializationSignificatOnly ? SchemaUtils.SCHEMA_ESENTIALS : SchemaUtils.SCHEMA_EVERYTHING,
            !serializationSignificatOnly, root);
  }


  public ImmutableCloningVisitor(final BiConsumer<Field, Field> copyField,
          final BiConsumer<Schema, Schema> copySchema,
          final boolean copyDocs, final Schema root) {
    this.copyField = copyField;
    this.copySchema = copySchema;
    this.copyDocs = copyDocs;
    this.root = root;
  }

  @Override
  public SchemaVisitorAction visitTerminal(final Schema terminal) {
    Schema.Type type = terminal.getType();
    Schema newSchema;
    switch (type) {
      case RECORD: // recursion.
      case ARRAY:
      case MAP:
      case UNION:
        if (!replace.containsKey(terminal)) {
          throw new IllegalStateException("Schema " + terminal + " must be already processed");
        }
        return SchemaVisitorAction.CONTINUE;
      case BOOLEAN:
      case BYTES:
      case DOUBLE:
      case FLOAT:
      case INT:
      case LONG:
      case NULL:
      case STRING:
        newSchema = Schema.create(type);
      break;
      case ENUM:
        newSchema = Schema.createEnum(terminal.getName(), copyDocs ? terminal.getDoc() : null,
                terminal.getNamespace(), terminal.getEnumSymbols());
        break;
      case FIXED:
        newSchema = Schema.createFixed(terminal.getName(), copyDocs ? terminal.getDoc() : null,
                terminal.getNamespace(), terminal.getFixedSize());
        break;
      default:
        throw new IllegalStateException("Unsupported schema " + terminal);
    }
    copySchema.accept(terminal, newSchema);
    replace.put(terminal, ImmutableSchema.create(newSchema));
    return SchemaVisitorAction.CONTINUE;
  }

  @Override
  public SchemaVisitorAction visitNonTerminal(final Schema nt) {
    Schema.Type type = nt.getType();
    if  (type == RECORD) {
        Schema newSchema = Schema.createRecord(nt.getName(), copyDocs ? nt.getDoc() : null,
                nt.getNamespace(), nt.isError());
        copySchema.accept(nt, newSchema);
        replace.put(nt, ImmutableSchema.create(newSchema));
    }
    return SchemaVisitorAction.CONTINUE;
  }

  @Override
  public SchemaVisitorAction afterVisitNonTerminal(final Schema nt) {
     Schema.Type type = nt.getType();
     Schema newSchema;
     switch (type) {
       case RECORD:
         newSchema = replace.get(nt);
         List<Schema.Field> fields = nt.getFields();
         List<Schema.Field> newFields = new ArrayList<>(fields.size());
         for (Schema.Field field : fields) {
          Schema.Field newField = new Schema.Field(field.name(), replace.get(field.schema()),
                  copyDocs ? field.doc() : null, field.defaultVal(), field.order());
          copyField.accept(field, newField);
          newFields.add(newField);
         }
         newSchema.setFields(newFields);
         return SchemaVisitorAction.CONTINUE;
       case UNION:
          List<Schema> types = nt.getTypes();
          List<Schema> newTypes = new ArrayList<>(types.size());
          for (Schema sch : types) {
            newTypes.add(replace.get(sch));
          }
          newSchema = Schema.createUnion(newTypes);

          break;
       case ARRAY:
         newSchema = Schema.createArray(replace.get(nt.getElementType()));
         break;
       case MAP:
         newSchema = Schema.createMap(replace.get(nt.getValueType()));
         break;
       default:
         throw new IllegalStateException("Illegal type " + type + ", schema " + nt);
     }
     copySchema.accept(nt, newSchema);
     replace.put(nt, ImmutableSchema.create(newSchema));
     return SchemaVisitorAction.CONTINUE;
  }

  @Override
  public ImmutableSchema get() {
    ImmutableSchema result = replace.get(root);
    if (result == null) {
      throw new IllegalStateException("Replacement map does not contain " + root + ", map " + replace);
    }
    return result;
  }

  @Override
  public String toString() {
    return "UnmodifiableCloningVisitor{" + "replace=" + replace + ", root=" + root + '}';
  }

}
