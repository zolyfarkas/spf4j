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

import com.google.common.annotations.Beta;
import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.apache.avro.Schema;
import org.apache.avro.ImmutableSchema;
import org.apache.avro.Schema.Field;
import org.spf4j.ds.IdentityHashSet;

/**
 * Avro Schema utilities, to traverse...
 * @author zoly
 */
@Beta
@ParametersAreNonnullByDefault
@SuppressFBWarnings("AI_ANNOTATION_ISSUES_NEEDS_NULLABLE") // false positive
public final class Schemas {

  private Schemas() {
  }

  @Nonnull
  public static ImmutableSchema immutable(final Schema schema) {
    if (schema instanceof ImmutableSchema) {
      return (ImmutableSchema) schema;
    }
    return visit(schema, new ImmutableCloningVisitor(schema, false));
  }

  @Nonnull
  public static ImmutableSchema immutable(final Schema schema, final boolean withSerializationSignificatAttrsonly) {
    return visit(schema, new ImmutableCloningVisitor(schema, withSerializationSignificatAttrsonly));
  }

  /**
   * depth first visit.
   *
   * @param start
   * @param visitor
   */
  public static <T> T visit(final Schema start, final SchemaVisitor<T> visitor) {
    // Set of Visited Schemas
    IdentityHashSet<Schema> visited = new IdentityHashSet<>();
    // Stack that contains the Schams to process and afterVisitNonTerminal functions.
    // Deque<Either<Schema, Supplier<SchemaVisitorAction>>>
    // Using either has a cost which we want to avoid...
    Deque<Object> dq = new ArrayDeque<>();
    dq.addLast(start);
    Object current;
    while ((current = dq.pollLast()) != null) {
      if (current instanceof Supplier) {
        // we are executing a non terminal post visit.
        SchemaVisitorAction action = ((Supplier<SchemaVisitorAction>) current).get();
        switch (action) {
          case CONTINUE:
            break;
          case SKIP_SUBTREE:
            throw new UnsupportedOperationException();
          case SKIP_SIBLINGS:
            while (dq.getLast() instanceof Schema) {
               dq.removeLast();
            }
            break;
          case TERMINATE:
            return visitor.get();
          default:
            throw new UnsupportedOperationException("Invalid action " + action);
        }
      } else {
        Schema schema = (Schema) current;
        boolean terminate;
        if (!visited.contains(schema)) {
          Schema.Type type = schema.getType();
          switch (type) {
            case ARRAY:
              terminate = visitNonTerminal(visitor, schema, dq, Collections.singletonList(schema.getElementType()));
              visited.add(schema);
              break;
            case RECORD:
              terminate = visitNonTerminal(visitor, schema, dq,
                      Lists.transform(Lists.reverse(schema.getFields()), Field::schema));
              visited.add(schema);
              break;
            case UNION:
              terminate = visitNonTerminal(visitor, schema, dq, schema.getTypes());
              visited.add(schema);
              break;
            case MAP:
              terminate = visitNonTerminal(visitor, schema, dq, Collections.singletonList(schema.getValueType()));
              visited.add(schema);
              break;
            case NULL:
            case BOOLEAN:
            case BYTES:
            case DOUBLE:
            case ENUM:
            case FIXED:
            case FLOAT:
            case INT:
            case LONG:
            case STRING:
              terminate = visitTerminal(visitor, schema, dq);
              break;
            default:
              throw new UnsupportedOperationException("Invalid type " + type);
          }

        } else {
          terminate = visitTerminal(visitor, schema, dq);
        }
        if (terminate) {
            return visitor.get();
        }
      }
    }
    return visitor.get();
  }

  private static boolean visitNonTerminal(final SchemaVisitor visitor,
          final Schema schema, final Deque<Object> dq,
          final Iterable<Schema> itSupp) {
    SchemaVisitorAction action = visitor.visitNonTerminal(schema);
    switch (action) {
      case CONTINUE:
        dq.addLast((Supplier<SchemaVisitorAction>) () -> visitor.afterVisitNonTerminal(schema));
        Iterator<Schema> it = itSupp.iterator();
        while (it.hasNext()) {
          Schema child = it.next();
          dq.addLast(child);
        }
        break;
      case SKIP_SUBTREE:
        dq.addLast((Supplier<SchemaVisitorAction>) () -> visitor.afterVisitNonTerminal(schema));
        break;
      case SKIP_SIBLINGS:
        while (!dq.isEmpty() && dq.getLast() instanceof Schema) {
          dq.removeLast();
        }
        break;
      case TERMINATE:
        return true;
      default:
        throw new UnsupportedOperationException("Invalid action " + action + " for " + schema);
    }
    return false;
  }

  private static boolean visitTerminal(final SchemaVisitor visitor, final Schema schema,
          final Deque<Object> dq) {
    SchemaVisitorAction action = visitor.visitTerminal(schema);
    switch (action) {
      case CONTINUE:
        break;
      case SKIP_SUBTREE:
        throw new UnsupportedOperationException("Invalid action " + action + " for " + schema);
      case SKIP_SIBLINGS:
        Object current;
        //CHECKSTYLE:OFF InnerAssignment
        while ((current = dq.getLast()) instanceof Schema) {
          // just skip
        }
        //CHECKSTYLE:ON
        dq.addLast(current);
        break;
      case TERMINATE:
        return true;
      default:
        throw new UnsupportedOperationException("Invalid action " + action + " for " + schema);
    }
    return false;
  }


}
