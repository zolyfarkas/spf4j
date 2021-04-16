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
import com.google.common.collect.Sets;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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
import org.apache.avro.logical_types.InstantLogicalType;
import org.apache.avro.logical_types.Temporal;
import org.apache.avro.reflect.ExtendedReflectData;
import org.apache.avro.specific.SpecificData;
import org.spf4j.avro.AvroCompatUtils;
import org.spf4j.base.CharSequences;
import org.spf4j.ds.IdentityHashSet;
import org.spf4j.io.csv.CharSeparatedValues;
import org.spf4j.io.csv.CsvParseException;

/**
 * Avro Schema utilities, to traverse...
 *
 * @author zoly
 */
@Beta
@ParametersAreNonnullByDefault
@SuppressFBWarnings("AI_ANNOTATION_ISSUES_NEEDS_NULLABLE") // false positive
public final class Schemas {

  private static final CharSeparatedValues SCHEMA_PATH_CSV = new CharSeparatedValues('.');

  private Schemas() {
  }

  public static void diff(final Schema s1, final Schema s2, final Consumer<SchemaDiff> diffs) {
    diff("", s1, s2, diffs, new IdentityHashSet<Schema>());
  }

  private static void diff(final String path, final Schema s1,
          final Schema s2, final Consumer<SchemaDiff> diffs, final Set<Schema> visited) {
    if (visited.contains(s1)) {
      return;
    }
    Schema.Type type1 = s1.getType();
    if (s2.getType() != type1) {
      diffs.accept(SchemaDiff.of(path, s1, s2, SchemaDiff.Type.DIFFERENT_TYPES));
      return;
    } else {
      switch (type1) {
        case BOOLEAN:
        case BYTES:
        case DOUBLE:
        case FLOAT:
        case INT:
        case LONG:
        case NULL:
        case STRING:
          break;
        case FIXED:
          if (s1.getFixedSize() != s2.getFixedSize()) {
            diffs.accept(SchemaDiff.of(path, s1, s2, SchemaDiff.Type.DIFFERENT_FIXED_SIZE));
          }
          if (!s1.getFullName().equals(s2.getFullName())) {
            diffs.accept(SchemaDiff.of(path, s1, s2, SchemaDiff.Type.DIFFERENT_NAMES));
          }
          break;
        case ARRAY:
          visited.add(s1);
          diff(pathAdd(path, "[]"), s1.getElementType(), s2.getElementType(), diffs, visited);
          break;
        case MAP:
          visited.add(s1);
          diff(pathAdd(path, "{}"), s1.getValueType(), s2.getValueType(), diffs, visited);
          break;
        case ENUM:
          if (!s1.getFullName().equals(s2.getFullName())) {
            diffs.accept(SchemaDiff.of(path, s1, s2, SchemaDiff.Type.DIFFERENT_NAMES));
          }
          List<String> s1s = s1.getEnumStringSymbols();
          List<String> s2s = s2.getEnumStringSymbols();
          if (!(s1s.containsAll(s2s)
                  && s2s.containsAll(s1s))) {
            diffs.accept(SchemaDiff.of(path, s1, s2, SchemaDiff.Type.DIFFERENT_ENUM_VALUES));
          }
          break;
        case UNION:
          visited.add(s1);
          for (Schema s : s1.getTypes()) {
            Schema os = getFromUnion(s, s2);
            if (os == null) {
              diffs.accept(SchemaDiff.of(path, s, null, SchemaDiff.Type.SCHEMA_MISSING_RIGHT));
            } else {
              diff(path, s, os, diffs, visited);
            }
          }
          for (Schema s : s2.getTypes()) {
            Schema os = getFromUnion(s, s2);
            if (os == null) {
              diffs.accept(SchemaDiff.of(path, null, s, SchemaDiff.Type.SCHEMA_MISSING_LEFT));
            }
          }
          break;
        case RECORD:
          visited.add(s1);
         if (!s1.getFullName().equals(s2.getFullName())) {
            diffs.accept(SchemaDiff.of(path, s1, s2, SchemaDiff.Type.DIFFERENT_NAMES));
          }
          for (Schema.Field field1 : s1.getFields()) {
            Field field2 = s2.getField(field1.name());
            if (field2 == null) {
              diffs.accept(SchemaDiff.of(path,
                      field1, null, SchemaDiff.Type.FIELD_MISSING_RIGHT));
            } else {
              diff(pathAdd(path, field1.name()), field1.schema(), field2.schema(), diffs, visited);
              if (!Objects.equals(field1.defaultVal(), field2.defaultVal())) {
                diffs.accept(SchemaDiff.of(path,
                        field1, field2, SchemaDiff.Type.DIFFERENT_FIELD_DEFAULTS));
              }
              if (!field1.getObjectProps().equals(field2.getObjectProps())) {
                diffs.accept(SchemaDiff.of(path,
                        field1, field2, SchemaDiff.Type.DIFFERENT_FIELD_PROPERTIES));
              }
              if (!Objects.equals(field1.doc(), field2.doc())) {
                diffs.accept(SchemaDiff.of(path,
                        field1, field2, SchemaDiff.Type.DIFFERRENT_FIELD_DOC));
              }
            }
          }
          for (Schema.Field field2 : s2.getFields()) {
            Field field1 = s1.getField(field2.name());
            if (field1 == null) {
              diffs.accept(SchemaDiff.of(path, null, field2, SchemaDiff.Type.FIELD_MISSING_LEFT));
            }
          }
          break;
          default:
            throw new IllegalStateException("Invalid Schema " + s1);
      }
    }
    if (!Objects.equals(s1.getLogicalType(), s2.getLogicalType())) {
      diffs.accept(SchemaDiff.of(path, s1, s2, SchemaDiff.Type.DIFFERENT_LOGICAL_TYPES));
    }
    if (!s1.getObjectProps().equals(s2.getObjectProps())) {
      diffs.accept(SchemaDiff.of(path, s1, s2, SchemaDiff.Type.DIFFERENT_SCHEMA_PROPERTIES));
    }
    if (!Objects.equals(s1.getDoc(), s2.getDoc())) {
      diffs.accept(SchemaDiff.of(path, s1, s2, SchemaDiff.Type.DIFFERRENT_SCHEMA_DOC));
    }
  }

  private static String pathAdd(final String p, final String ref) {
    if (p.isEmpty()) {
      return SCHEMA_PATH_CSV.toCsvElement(ref);
    } else {
      return p + '.' + SCHEMA_PATH_CSV.toCsvElement(ref);
    }
  }

  @Nullable
  public static Schema getFromUnion(final Schema what, final Schema unionSchema) {
    String fullName = what.getFullName();
    for (Schema s : unionSchema.getTypes()) {
      if (fullName.equals(s.getFullName())) {
        return s;
      }
    }
    return null;
  }


  public static void deprecations(final Schema schema,
          final BiConsumer<String, String> toPut) {
    visit(schema, new DeprecationVisitor(toPut));
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
    if (schema.getType() != Schema.Type.UNION) {
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

  @Nullable
  public static Schema getSchemaFromUnionByName(final Schema unionSchema, final String name) {
    if (!(unionSchema.getType() == Schema.Type.UNION)) {
      return name.equals(unionSchema.getFullName()) ? unionSchema : null;
    }
    List<Schema> types = unionSchema.getTypes();
    for (Schema schema : types) {
      if (name.equals(schema.getFullName())) {
        return schema;
      }
    }
    return null;
  }

  @Nullable
  public static Schema getSubSchema(final Schema schema, final CharSequence path) {
    if (path.length() == 0) {
      return schema;
    }
    List<String> parsedPath;
    try {
      parsedPath = SCHEMA_PATH_CSV.readRow(CharSequences.reader(path));
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    } catch (CsvParseException ex) {
      throw new IllegalArgumentException("Invalid path " + path, ex);
    }
    Schema result = schema;
    for (String part : parsedPath) {
      result = getSegment(result, part);
      if (result == null) {
        return null;
      }
    }
    return result;
  }

  @Nullable
  private static Schema getSegment(final Schema result, final String part) {
    switch (result.getType()) {
        case ARRAY:
          if ("[]".equals(part)) {
            return result.getElementType();
          }
          break;
        case MAP:
          if ("{}".equals(part)) {
            return result.getValueType();
          }
          break;
        case UNION:
          for (Schema us : result.getTypes()) {
            Schema ur = getSegment(us, part);
            if (ur != null) {
              return ur;
            }
          }
          break;
        case RECORD:
          Field field = result.getField(part);
          if (field == null) {
            for (Schema.Field f : result.getFields()) {
              if (f.aliases().contains(part)) {
                field = f;
              }
            }
          }
          if (field != null) {
            return field.schema();
          }
          break;
        default:
          return null;
      }
      return null;
  }

  public static Schema projectRecord(final Schema schema, final int[] projection) {
    List<Field> fields = schema.getFields();
    List<Field> nFields = new ArrayList<>(projection.length);
    for (int i = 0; i < projection.length; i++) {
      Field of = fields.get(projection[i]);
      Field nfield = new Schema.Field(of, of.schema());
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
    final List<List<String>> p = new ArrayList<>(paths.size());
    for (CharSequence cs : paths) {
      try {
        p.add(SCHEMA_PATH_CSV.readRow(CharSequences.reader(cs)));
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      } catch (CsvParseException ex) {
        throw new IllegalArgumentException("Invalid projection path " + cs, ex);
      }
    }
    return projectInternal(schema, p);
  }

  @Nullable
  private static Schema projectInternal(final Schema schema, final List<List<String>> paths) {
    int length = paths.size();
    if (length == 0) {
      return schema;
    }
    if (length == 1) {
       List<String> first = paths.get(0);
       if (first.isEmpty() || (first.size() == 1 && first.get(0).isEmpty())) {
         return schema;
       }
    }
    List<List<String>> seqs;
    switch (schema.getType()) {
      case ARRAY:
        seqs = new ArrayList<>(length);
        for (List<String> path : paths) {
          String part = path.get(0);
          if ("[]".equals(part)) {
            int pSize = path.size();
            if (pSize == 1) {
              return schema;
            }
            seqs.add(path.subList(1, pSize));
          } else {
            return null;
          }
        }
        if (seqs.isEmpty()) {
          return null;
        }
        return Schema.createArray(projectInternal(schema.getElementType(), seqs));
      case MAP:
        seqs = new ArrayList<>(length);
        for (List<String>  path : paths) {
          String part = path.get(0);
          if ("{}".equals(part)) {
            int pSize = path.size();
            if (1 == pSize) {
              return schema;
            }
            seqs.add(path.subList(1, pSize));
          }
        }
        if (seqs.isEmpty()) {
          return null;
        }
        return Schema.createMap(projectInternal(schema.getElementType(), seqs));
      case RECORD:
        List<Field> fields = schema.getFields();
        List<Schema.Field> nFields = new ArrayList<>(fields.size());
        List<List<String>> tPaths = new LinkedList<>(paths);
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
        Schema rec = AvroCompatUtils.createRecordSchema(schema.getName(), schema.getDoc(),
                "_p." + schema.getNamespace(), schema.isError(), false);
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
            Schema project = projectInternal(us, paths);
            if (project != null) {
              nTypes.add(project);
            }
          }
        }
        return Schema.createUnion(nTypes);
      default:
        if (paths.contains(Collections.emptyList())) {
          return schema;
        } else {
          return null;
        }
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
        Schema toElementType = toSchema.getElementType();
        Schema fromElementType = fromSchema.getElementType();
        for (Object o : from) {
          to.add(project(toElementType, fromElementType, o));
        }
        return to;
      case MAP:
        Map<String, Object> fromMap = (Map<String, Object>) object;
        Map<String, Object> toMap = Maps.newLinkedHashMapWithExpectedSize(fromMap.size());
        Schema toValueType = toSchema.getValueType();
        Schema fromValueType = fromSchema.getValueType();
        for (Map.Entry<String, Object> entry : fromMap.entrySet()) {
          toMap.put(entry.getKey(), project(toValueType, fromValueType, entry.getValue()));
        }
        return toMap;
      case UNION:
        ExtendedReflectData rftor = ExtendedReflectData.get();
        int unionIdx = rftor.resolveUnion(fromSchema, object);
        Schema objSchema = fromSchema.getTypes().get(unionIdx);
        String name = rftor.getSchemaName(objSchema);
        Set<String> aliases = objSchema.getAliases();
        if (aliases.isEmpty()) {
          for (Schema toMatch : toSchema.getTypes()) {
            if (name.equals(rftor.getSchemaName(toMatch)) || toMatch.getAliases().contains(name)) {
              return project(toMatch, objSchema, object);
            }
          }
        } else {
          for (Schema toMatch : toSchema.getTypes()) {
            String toMatchName = rftor.getSchemaName(toMatch);
            if (name.equals(toMatchName) || aliases.contains(toMatchName)
                    || !Sets.intersection(aliases, toMatch.getAliases()).isEmpty()) {
              return project(toMatch, objSchema, object);
            }
          }
        }
        throw new IllegalArgumentException("Unable to project " + object + " to " + toSchema);

      case RECORD:
        GenericData.Record record = new SpecificData.Record(toSchema);
        GenericRecord fromRec = (GenericRecord) object;
        Schema frSchema = fromRec.getSchema();
        for (Field field : toSchema.getFields()) {
          Field frField = getPos(field, frSchema);
          int toPos = field.pos();
          if (frField == null) {
            record.put(toPos, field.defaultVal());
          } else {
            record.put(toPos, project(field.schema(), frField.schema(), fromRec.get(frField.pos())));
          }
        }
        return record;
      default:
        throw new IllegalStateException("Unsupported type " + type);
    }
  }


  @Nullable
  public static Schema.Field getPos(final Schema.Field field, final Schema recordSchema) {
    Field rf = recordSchema.getField(field.name());
    if (rf == null) {
      for (String alias : field.aliases()) {
        rf = recordSchema.getField(alias);
        if (rf != null) {
          return rf;
        }
      }
      return null;
    } else {
      return rf;
    }
  }


  @Nullable
  private static Schema.Field extract(final Iterable<Field> fields, final Iterable<List<String>> paths) {
    Iterator<List<String>> iterator = paths.iterator();
    if (iterator.hasNext()) {
      List<List<String>> proj = new ArrayList<>(2);
      List<String> path = iterator.next();
      String part = path.get(0);
      Field field = getField(part, fields);
      if (field == null) {
        return null;
      }
      iterator.remove();
      if (1 == path.size()) {
        proj.add(Collections.EMPTY_LIST);
      } else {
        proj.add(path.subList(1, path.size()));
      }
      while (iterator.hasNext()) {
        path = iterator.next();
        part = path.get(0);
        if (isNamed(part, field)) {
          if (1 == path.size()) {
            proj.add(Collections.EMPTY_LIST);
          } else {
            proj.add(path.subList(1,  path.size()));
          }
          iterator.remove();
        }
      }
      return  new Schema.Field(field, projectInternal(field.schema(), proj));
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

  public static Schema dateString() {
    Schema schema = Schema.create(Schema.Type.STRING);
    LogicalTypes.date().addToSchema(schema);
    return schema;
  }

  public static Schema instantString() {
    Schema schema = Schema.create(Schema.Type.STRING);
    InstantLogicalType.instance().addToSchema(schema);
    return schema;
  }

  public static Schema temporalString() {
    Schema schema = Schema.create(Schema.Type.STRING);
    Temporal.instance().addToSchema(schema);
    return schema;
  }

  private static class DeprecationVisitor implements SchemaVisitor<Void> {

    private final BiConsumer<String, String> toPut;

    DeprecationVisitor(final BiConsumer<String, String> toPut) {
      this.toPut = toPut;
    }

    @Override
    public SchemaVisitorAction visitTerminal(final Schema terminal) {
      if (terminal.getType() == Schema.Type.FIXED) {
        checkDeprecation(terminal);
      }
      // do nothing for base types (will ignore if somebody deprercates an int....)
      return SchemaVisitorAction.CONTINUE;
    }

    public void checkDeprecation(final Schema terminal) {
      String dMsg = terminal.getProp("deprecated");
      if (dMsg != null) {
        toPut.accept(terminal.getFullName(), dMsg);
      }
    }

    @Override
    public SchemaVisitorAction visitNonTerminal(final Schema nonTerminal) {
      checkDeprecation(nonTerminal);
      if  (nonTerminal.getType() == Schema.Type.RECORD) {
        for (Schema.Field field : nonTerminal.getFields()) {
          String msg = field.getProp("deprecated");
          if (msg != null) {
            toPut.accept(nonTerminal.getFullName() + '.' + field.name(), msg);
          }
        }
      }
      return SchemaVisitorAction.CONTINUE;
    }

    @Override
    public SchemaVisitorAction afterVisitNonTerminal(final Schema nonTerminal) {
      return SchemaVisitorAction.CONTINUE;
    }
  }


}
