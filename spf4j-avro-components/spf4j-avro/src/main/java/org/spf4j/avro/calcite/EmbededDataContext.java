/*
 * Copyright 2019 SPF4J.
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
package org.spf4j.avro.calcite;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nullable;
import org.apache.avro.Schema;
import org.apache.calcite.DataContext;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.linq4j.QueryProvider;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.tools.Frameworks;
import org.spf4j.avro.schema.Schemas;
import org.spf4j.security.AbacSecurityContext;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("STT_TOSTRING_MAP_KEYING")
public final class EmbededDataContext implements DataContext {

  public static final String DEPRECATIONS = "deprecated-access";

  public static final String SECURITY_CONTEXT = "security-context";


  public static void addDeprecations(final Schema schema,
          final DataContext ctx) {
    final Map<String, String> deprecations = new HashMap<>(4);
    Schemas.deprecations(schema, deprecations::put);
    addDeprecations(schema.getFullName(), deprecations, ctx);
  }

  public static void addDeprecations(final String schemaName, final Map<String, String> deprecations,
          final DataContext ctx) {
    if (deprecations.isEmpty()) {
      return;
    }
    Map<String, String> depr = (Map<String, String>) ctx.get(DEPRECATIONS);
    if (depr == null) {
      return;
    }
    for (Map.Entry<String, String> entry : deprecations.entrySet()) {
      depr.put(schemaName + '.' + entry.getKey(), entry.getValue());
    }
  }

  private final JavaTypeFactory typeFact;

  private final ConcurrentMap<String, Object> data;

  public EmbededDataContext(final JavaTypeFactory typeFact, @Nullable final AbacSecurityContext ctx) {
    this.typeFact = typeFact;
    this.data = new ConcurrentHashMap<>();
    this.data.put(DEPRECATIONS, new HashMap<Object, Object>(4));
    if (ctx != null) {
      this.data.put(SECURITY_CONTEXT, ctx);
    }
  }

  public SchemaPlus getRootSchema() {
    return Frameworks.createRootSchema(true);
  }

  public JavaTypeFactory getTypeFactory() {
    return (JavaTypeFactory) typeFact;
  }

  public QueryProvider getQueryProvider() {
    return null;
  }

  public Object get(final String name) {
    return data.get(name);
  }

  public Object put(final String name, final Object value) {
    return data.put(name, value);
  }

  @Override
  public String toString() {
    return "EmbededDataContext{" + "typeFact=" + typeFact + '}';
  }

}
