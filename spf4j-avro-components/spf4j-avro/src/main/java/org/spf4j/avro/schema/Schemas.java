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
import com.google.common.collect.Maps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.apache.avro.Schema;
import org.apache.avro.ImmutableSchema;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.reflect.ExtendedReflectData;
import org.apache.avro.specific.SpecificData;
import org.spf4j.base.CharSequences;
import org.spf4j.ds.IdentityHashSet;

/**
 * Avro Schema utilities, to traverse...
 *
 * @author zoly
 */
@Beta
@ParametersAreNonnullByDefault
@SuppressFBWarnings("AI_ANNOTATION_ISSUES_NEEDS_NULLABLE") // false positive
public final class Schemas {

  private Schemas() {
  }

  @Nonnull
  public static void deprecations(final Schema schema,
          final BiConsumer<String, String> toPut) {
    String dMsg = schema.getProp("deprecated");
    if (dMsg != null) {
      toPut.accept(schema.getFullName(), dMsg);
    }
    switch (schema.getType()) {
      case ARRAY:
        deprecations(schema.getElementType(), toPut);
        break;
      case MAP:
        deprecations(schema.getValueType(), toPut);
        break;
      case RECORD:
        for (Schema.Field field : schema.getFields()) {
          String msg = field.getProp("deprecated");
          if (msg != null) {
            toPut.accept(schema.getFullName() + '.' + field.name(), msg);
          }
          deprecations(field.schema(), toPut);
        }
        break;
      default:
        // do nothing
    }

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

  public static boolean isNullableUnion(final Schema schema) {
    if (!(schema.getType() == Schema.Type.UNION)) {
      return false;
    }
    for (Schema ss : schema.getTypes()) {
      if (ss.getType() == Schema.Type.NULL) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  public static Schema nullableUnionSchema(final Schema schema) {
    if (!(schema.getType() == Schema.Type.UNION)) {
      return null;
    }
    List<Schema> types = schema.getTypes();
    if (types.size() != 2) {
      return null;
    }
    if (types.get(0).getType() == Schema.Type.NULL) {
      return types.get(1);
    } else if (types.get(1).getType() == Schema.Type.NULL) {
      return types.get(0);
    } else {
      return null;
    }
  }

  public static Schema getSubSchema(final Schema schema, final CharSequence path) {
    return getSubSchema(schema, path, 0);
  }

  public static Schema getSubSchema(final Schema schema, final CharSequence path, final int at) {
    int length = path.length();
    if (at >= length) {
      return schema;
    }
    int to = CharSequences.indexOf(path, at, length, '.');
    if (to < 0) {
      to = length;
    }

    String part = CharSequences.subSequence(path, at, to).toString().trim();
    switch (schema.getType()) {
      case ARRAY:
        if ("[]".equals(part)) {
          if (to == length) {
            return schema.getElementType();
          }
          return getSubSchema(schema.getElementType(), path, to + 1);
        } else {
          throw new IllegalArgumentException("Invalid path " + path + " at " + at + ", " + schema);
        }
      case MAP:
        if ("{}".equals(part)) {
          if (to == length) {
            return schema.getValueType();
          }
          return getSubSchema(schema.getValueType(), path, to + 1);
        } else {
          throw new IllegalArgumentException("Invalid path " + path + " at " + at + ", " + schema);
        }
      case RECORD:
        for (Schema.Field field : schema.getFields()) {
          if (part.equals(field.name()) || field.aliases().contains(part)) {
            if (to == length) {
              return field.schema();
            }
            return getSubSchema(field.schema(), path, to + 1);
          }
        }
        throw new IllegalArgumentException("Invalid path " + path + " at " + at + ", " + schema);
      default:
        throw new IllegalArgumentException("Invalid path " + path + " at " + at + ", " + schema);
    }

  }

  public static Schema projectRecord(final Schema schema, final int[] projection) {
    List<Field> fields = schema.getFields();
    List<Field> nFields = new ArrayList<>(projection.length);
    for (int i = 0; i < projection.length; i++) {
      Field of = fields.get(projection[i]);
      Field nfield = new Schema.Field(of.name(), of.schema(),
              of.doc(), of.defaultVal(), of.order());
      String dep = of.getProp("deprecated");
      if (dep != null) {
        nfield.addProp("deprecated", dep);
      }
      nFields.add(nfield);
    }
    if (isSameFields(fields, nFields)) {
      return schema;
    }
    Schema rec = Schema.createRecord(schema.getName(), schema.getDoc(),
            "_p." + schema.getNamespace(), schema.isError());
    rec.setFields(nFields);
    String dep = schema.getProp("deprecated");
    if (dep != null) {
      rec.addProp("deprecated", dep);
    }
    return rec;
  }


  @Nullable
  public static Schema project(final Schema schema, final CharSequence... paths) {
    return project(schema, Arrays.asList(paths));
  }

  /**
   * create a schema projection(partial schema) from a destination schema.
   *
   * @param schema the schema to project.
   * @param paths the paths to elements (paths to record fields) included in the subschema.
   * @return the subschema.
   */

  @Nullable
  public static Schema project(final Schema schema, final List<? extends CharSequence> paths) {
    int length = paths.size();
    if (length == 0 || (length == 1 && paths.get(0).length() == 0)) {
      return schema;
    }
    List<CharSequence> seqs;
    switch (schema.getType()) {
      case ARRAY:
        seqs = new ArrayList<>(length);
        for (CharSequence path : paths) {
          String part = getFirstRef(path);
          if ("[]".equals(part)) {
            if (part.length() == path.length()) {
              return schema;
            }
            seqs.add(part.substring(part.length() + 1));
          } else {
            return null;
          }
        }
        if (seqs.isEmpty()) {
          return null;
        }
        return Schema.createArray(project(schema.getElementType(), seqs));
      case MAP:
        seqs = new ArrayList<>(length);
        for (CharSequence path : paths) {
          String part = getFirstRef(path);
          if ("{}".equals(part)) {
            if (part.length() == path.length()) {
              return schema;
            }
            seqs.add(part.substring("{}".length() + 1));
          }
        }
        if (seqs.isEmpty()) {
          return null;
        }
        return Schema.createMap(project(schema.getElementType(), seqs));
      case RECORD:
        List<Field> fields = schema.getFields();
        List<Schema.Field> nFields = new ArrayList<>(fields.size());
        List<CharSequence> tPaths = new LinkedList<>(paths);
        do {
          Field extract = extract(fields, tPaths);
          if (extract == null) {
            return null;
          }
          nFields.add(extract);
        } while (!tPaths.isEmpty());
        if (isSameFields(fields, nFields)) {
          return schema;
        }
        Schema rec = Schema.createRecord(schema.getName(), schema.getDoc(),
                "_p." + schema.getNamespace(), schema.isError());
        rec.setFields(nFields);
        String dep = schema.getProp("deprecated");
        if (dep != null) {
          rec.addProp("deprecated", dep);
        }
        return rec;
      case UNION:
        List<Schema> types = schema.getTypes();
        List<Schema> nTypes = new ArrayList<>(types.size());
        for (Schema us : types) {
          if (us.getType() == Schema.Type.NULL) {
            nTypes.add(us);
          } else {
            Schema project = project(us, paths);
            if (project != null) {
              nTypes.add(project);
            }
          }
        }
        return Schema.createUnion(nTypes);
      default:
        return null;
    }
  }

  public static boolean isSameFields(final List<Schema.Field> f1s, final List<Schema.Field> f2s) {
    int l1 = f1s.size();
    int l2 = f2s.size();
    if (l1 != l2) {
      return false;
    }
    for (int i = 0; i < l1; i++) {
      Schema.Field f1 = f1s.get(i);
      Schema.Field f2 = f2s.get(i);
      if (!f1.name().equals(f2.name())
              || !f1.schema().equals(f2.schema())) {
        return false;
      }
    }
    return true;
  }


  /**
   * Project a avro object to a destination.
   * @param toSchema the object schema
   * @param fromSchema the destination schema
   * @param object the object to project.
   * @return the projected object.
   */
  @Nullable
  public static IndexedRecord project(final Schema toSchema, final Schema fromSchema,
          @Nullable final IndexedRecord object) {
    return (IndexedRecord) project(toSchema, fromSchema, (Object) object);
  }

  /**
   * Project a avro object to a destination.
   * @param toSchema the object schema
   * @param fromSchema the destination schema
   * @param object the object to project.
   * @return the projected object.
   */
  @Nullable
  @SuppressFBWarnings("URV_UNRELATED_RETURN_VALUES")
  public static Object project(final Schema toSchema, final Schema fromSchema, @Nullable final Object object) {
    if (toSchema == fromSchema) {
      return object;
    }
    Schema.Type type = toSchema.getType();
    if (fromSchema.getType() != type) {
      throw new IllegalArgumentException("Unable to project " + object + " from "  + fromSchema +  " to " + toSchema);
    }
    switch (type) {
      case INT:
      case LONG:
      case FLOAT:
      case STRING:
      case ENUM:
      case FIXED:
      case BOOLEAN:
      case BYTES:
      case DOUBLE:
        return object;
      case NULL:
        return null;
      case ARRAY:
        List from = (List) object;
        List to = new ArrayList(from.size());
        for (Object o : from) {
          to.add(project(toSchema.getElementType(), fromSchema.getElementType(), o));
        }
        return to;
      case MAP:
        Map<String, Object> fromMap = (Map<String, Object>) object;
        Map<String, Object> toMap = Maps.newLinkedHashMapWithExpectedSize(fromMap.size());
        for (Map.Entry<String, Object> entry : fromMap.entrySet()) {
          toMap.put(entry.getKey(), project(toSchema.getValueType(), fromSchema.getValueType(), entry.getValue()));
        }
        return toMap;
      case UNION:
        if (object == null) {
          return null;
        }
        Schema tSchema = Schemas.nullableUnionSchema(toSchema);
        if (tSchema != null) {
          return project(tSchema, tSchema, object);
        } else {
          Schema objReflSchema = ExtendedReflectData.get().getSchema(object.getClass());
          for (Schema matching : toSchema.getTypes()) {
            if (matching.getType() == objReflSchema.getType()) {
              return project(matching, objReflSchema, object);
            }
          }
        }
        throw new IllegalArgumentException("Unable to project " + object + " to " + toSchema);

      case RECORD:
        GenericData.Record record = new SpecificData.Record(toSchema);
        GenericRecord fromRec = (GenericRecord) object;
        for (Field field : toSchema.getFields()) {
          record.put(field.pos(), fromRec.get(field.name()));
        }
        return record;
      default:
        throw new IllegalStateException("Unsupported type " + type);
    }
  }



  @Nullable
  private static Schema.Field extract(final Iterable<Field> fields, final Iterable<CharSequence> paths) {
    Iterator<CharSequence> iterator = paths.iterator();
    if (iterator.hasNext()) {
      List<CharSequence> proj = new ArrayList<>(2);
      CharSequence path = iterator.next();
      String part = getFirstRef(path);
      Field field = getField(part, fields);
      if (field == null) {
        return null;
      }
      iterator.remove();
      if (part.length() == path.length()) {
        proj.add("");
      } else {
        proj.add(path.subSequence(part.length() + 1, path.length()));
      }
      while (iterator.hasNext()) {
        path = iterator.next();
        part = getFirstRef(path);
        if (isNamed(part, field)) {
          if (part.length() == path.length()) {
            proj.add("");
          } else {
            proj.add(path.subSequence(part.length() + 1, path.length()));
          }
          iterator.remove();
        }
      }
      Schema.Field nfield = new Schema.Field(field.name(),
                    project(field.schema(), proj),
                    field.doc(), field.defaultVal(), field.order());
      String dep = field.getProp("deprecated");
      if (dep != null) {
        nfield.addProp("deprecated", dep);
      }
      return nfield;
    } else {
      return null;
    }
  }


  @Nullable
  public static Schema.Field getField(final String name, final Iterable<Field> fields) {
    for (Schema.Field field : fields) {
      if (isNamed(name, field)) {
        return field;
      }
    }
    return null;
  }

  public static boolean isNamed(final String name, final Field field) {
    if (name.equals(field.name()) || field.aliases().contains(name)) {
      return true;
    }
    return false;
  }

  private static String getFirstRef(final CharSequence path) {
    int length = path.length();
    int to = CharSequences.indexOf(path, 0, length, '.');
    String part;
    if (to < 0) {
      part = path.toString();
    } else {
      part = path.subSequence(0, to).toString();
    }
    return part;
  }

  public static Schema dateString() {
    Schema schema = Schema.create(Schema.Type.STRING);
    schema.addProp("logicalType", "date");
    schema.setLogicalType(LogicalTypes.fromSchema(schema));
    return schema;
  }

  public static Schema instantString() {
    Schema schema = Schema.create(Schema.Type.STRING);
    schema.addProp("logicalType", "instant");
    schema.setLogicalType(LogicalTypes.fromSchema(schema));
    return schema;
  }

  public static Schema temporalString() {
    Schema schema = Schema.create(Schema.Type.STRING);
    schema.addProp("logicalType", "any_temporal");
    schema.setLogicalType(LogicalTypes.fromSchema(schema));
    return schema;
  }


}
