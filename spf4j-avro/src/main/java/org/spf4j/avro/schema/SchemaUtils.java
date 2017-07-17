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

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.apache.avro.JsonProperties;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.avro.compiler.specific.SpecificCompiler;

/**
 * @author zoly
 */
public final class SchemaUtils {

  public static final BiConsumer<Schema, Schema> SCHEMA_ESENTIALS =
          (a, b) -> {
            SchemaUtils.copyAliases(a, b);
            SchemaUtils.copyLogicalTypes(a, b);
          };


  public static final BiConsumer<Schema.Field, Schema.Field> FIELD_ESENTIALS =
          (a, b) -> {
            SchemaUtils.copyAliases(a, b);
          };


  public static final BiConsumer<Schema, Schema> SCHEMA_EVERYTHING =
          (a, b) -> {
            SchemaUtils.copyAliases(a, b);
            SchemaUtils.copyLogicalTypes(a, b);
            SchemaUtils.copyProperties(a, b);
          };

  public static final BiConsumer<Schema.Field, Schema.Field> FIELD_EVERYTHING =
          (a, b) -> {
            SchemaUtils.copyAliases(a, b);
            SchemaUtils.copyProperties(a, b);
          };



  private SchemaUtils() {
  }

  public static void copyAliases(final Schema from, final Schema to) {
    //CHECKSTYLE:OFF
    switch (from.getType()) { // only named types.
      case RECORD:
      case ENUM:
      case FIXED:
        Set<String> aliases = from.getAliases();
        for (String alias : aliases) {
          to.addAlias(alias);
        }
      default:
      //ignore unnamed one's
    }
    //CHECKSTYLE:OFF
  }

  public static void copyAliases(final Schema.Field from, final Schema.Field to) {
    Set<String> aliases = from.aliases();
    for (String alias : aliases) {
      to.addAlias(alias);
    }
  }

  public static void copyLogicalTypes(final Schema from, final Schema to) {
    LogicalType logicalType = from.getLogicalType();
    if (logicalType != null) {
      logicalType.addToSchema(to);
    }
  }

  public static void copyProperties(final JsonProperties from, final JsonProperties to) {
    Map<String, Object> objectProps = from.getObjectProps();
    for (Map.Entry<String, Object> entry : objectProps.entrySet()) {
      to.addProp(entry.getKey(), entry.getValue());
    }
  }

  public static boolean hasGeneratedJavaClass(final Schema schema) {
    Schema.Type type = schema.getType();
    switch (type) {
      case ENUM:
      case RECORD:
      case FIXED:
        return true;
      default:
        return false;
    }
  }

  public static String getJavaClassName(final Schema schema) {
    String namespace = schema.getNamespace();
    if (namespace == null || namespace.isEmpty()) {
      return SpecificCompiler.mangle(schema.getName());
    } else {
      return namespace + '.' + SpecificCompiler.mangle(schema.getName());
    }
  }

}
