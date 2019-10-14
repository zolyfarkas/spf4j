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

import org.apache.calcite.DataContext;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.linq4j.QueryProvider;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;

/**
 * @author Zoltan Farkas
 */
public final class EmbededDataContext implements DataContext {

  private final Planner planner;

  public EmbededDataContext(final Planner planner) {
    this.planner = planner;
  }

  public SchemaPlus getRootSchema() {
    return Frameworks.createRootSchema(true);
  }

  public JavaTypeFactory getTypeFactory() {
    return (JavaTypeFactory) planner.getTypeFactory();
  }

  public QueryProvider getQueryProvider() {
    return null;
  }

  public Object get(final String name) {
    return null;
  }

  @Override
  public String toString() {
    return "EmbededDataContext{" + "planner=" + planner + '}';
  }

}
