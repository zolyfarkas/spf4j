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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import org.apache.avro.Schema;

/**
 * A visitor that recursively visits a schema and returns a map like:
 * java class name -> Schema
 * for every Schema encountered that has a java class implementation.
 * @author zoly
 */
//CHECKSTYLE IGNORE MissingSwitchDefault FOR NEXT 200 LINES
public final class SchemasWithClasses implements SchemaVisitor<Map<String, Schema>> {

  private final Map<String, Schema> schemas = new HashMap<>();

  @Override
  @SuppressFBWarnings("SF_SWITCH_NO_DEFAULT")
  public SchemaVisitorAction visitTerminal(final Schema schema) {
    switch (schema.getType()) {
      case FIXED:
      case ENUM:
        schemas.put(SchemaUtils.getJavaClassName(schema), schema);
        break;
    }
    return SchemaVisitorAction.CONTINUE;
  }

  @Override
  public SchemaVisitorAction visitNonTerminal(final Schema schema) {
    if (schema.getType() == Schema.Type.RECORD) {
      schemas.put(SchemaUtils.getJavaClassName(schema), schema);
    }
    return SchemaVisitorAction.CONTINUE;
  }

  @Override
  public SchemaVisitorAction afterVisitNonTerminal(final Schema terminal) {
    return SchemaVisitorAction.CONTINUE;
  }

  @Override
  public Map<String, Schema> get() {
    return schemas;
  }

  @Override
  public String toString() {
    return "SchemasWithClasses{" + "schemas=" + schemas + '}';
  }


}
