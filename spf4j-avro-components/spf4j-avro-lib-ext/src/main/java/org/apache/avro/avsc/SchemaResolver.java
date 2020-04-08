/*
 * Copyright 2015 The Apache Software Foundation.
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
package org.apache.avro.avsc;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.avro.Schema;

/**
 * Utility class to resolve schemas that are unavailable at the time they are referenced in the IDL.
 */
public final class SchemaResolver {

  private SchemaResolver() {
  }

  private static final String UR_SCHEMA_ATTR = "org.apache.avro.compiler.idl.unresolved.name";

  private static final String UR_SCHEMA_NAME = "UnresolvedSchema";

  private static final String UR_SCHEMA_NS = "org.apache.avro.compiler";

  private static final AtomicInteger COUNTER = new AtomicInteger();

  /**
   * Create a schema to represent a "unresolved" schema.
   * (used to represent a schema where the definition is not known at the time)
   * This concept might be generalizable...
   *
   * @param name
   * @return
   */
  public static Schema unresolvedSchema(final String name) {
    Schema schema = Schema.createRecord(UR_SCHEMA_NAME + '_' + COUNTER.getAndIncrement(), "unresolved schema",
        UR_SCHEMA_NS, false, Collections.EMPTY_LIST);
    schema.addProp(UR_SCHEMA_ATTR, name);
    return schema;
  }

  /**
   * Is this a unresolved schema.
   *
   * @param schema
   * @return
   */
  static boolean isUnresolvedSchema(final Schema schema) {
    return (schema.getType() == Schema.Type.RECORD && schema.getProp(UR_SCHEMA_ATTR) != null
        && schema.getName().startsWith(UR_SCHEMA_NAME)
        && UR_SCHEMA_NS.equals(schema.getNamespace()));
  }

  /**
   * get the unresolved schema name.
   *
   * @param schema
   * @return
   */
  @SuppressFBWarnings("AI_ANNOTATION_ISSUES_NEEDS_NULLABLE")
  static String getUnresolvedSchemaName(final Schema schema) {
    if (!isUnresolvedSchema(schema)) {
      throw new IllegalArgumentException("Not a unresolved schema: " + schema);
    }
    return schema.getProp(UR_SCHEMA_ATTR);
  }

  /**
   * Will clone the provided protocol while resolving all unreferenced schemas
   *
   * @param protocol
   * @return
   */
  public static List<Schema> resolve(final Map<String, Schema> schemas, final boolean allowUndefinedLogicalTypes) {
    final Collection<Schema> types = schemas.values();
    // replace unresolved schemas.
    List<Schema> newSchemas = new ArrayList(types.size());
    IdentityHashMap<Schema, Schema> replacements = new IdentityHashMap<Schema, Schema>();
    for (Schema schema : types) {
      newSchemas.add(Schemas.visit(schema, new ResolvingVisitor(schema, replacements,
              schemas::get, allowUndefinedLogicalTypes)));
    }
    return newSchemas;
  }

}
