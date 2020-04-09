/*
 * Copyright 2020 SPF4J.
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

import com.fasterxml.jackson.core.JsonParser;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.annotation.Nonnull;
import static org.apache.avro.Schema.MAPPER;

/**
 * @author Zoltan Farkas
 */
public final class SchemaAdapter {

  private static final Method PARSE_METHOD;

  static {
    Method m;
    try {
      m = Schema.class.getMethod("parse", JsonParser.class, Schema.Names.class,
              boolean.class, boolean.class, boolean.class);
    } catch (NoSuchMethodException ex) {
      m = null;
    } catch (SecurityException ex) {
      throw new RuntimeException(ex);
    }
    PARSE_METHOD = m;
  }

  private SchemaAdapter() { }

  @Nonnull
  public static Schema parse(final JsonParser parser,
          final ExtendedNames names, final boolean allowUndefinedLogicalTypes,
          final boolean validateNames, final boolean validateDefaults)
          throws IOException {
    Schema schema;
    if (PARSE_METHOD == null) {
      schema = Schema.parse(MAPPER.readTree(parser), names);
    } else {
      try {
        schema = (Schema) PARSE_METHOD.invoke(null, MAPPER.readTree(parser), names,
                allowUndefinedLogicalTypes, validateNames, validateDefaults);
      } catch (IllegalAccessException | InvocationTargetException ex) {
        throw new SchemaParseException(ex);
      }
    }
    if (schema == null) {
      throw new NullPointerException();
    }
    return schema;
  }

  public static void parseLogicalType(final Schema schema, final boolean allowUndefinedLogicalTypes) {
    if (schema.getLogicalType() == null) {
      if (allowUndefinedLogicalTypes) {
        LogicalType lt = LogicalTypes.fromSchemaIgnoreInvalid(schema);
        if (lt != null) {
          lt.addToSchema(schema);
        }
      } else {
        LogicalType lt = LogicalTypes.fromSchema(schema);
        if (lt != null) {
          lt.addToSchema(schema);
        }
      }
    }
  }
}
