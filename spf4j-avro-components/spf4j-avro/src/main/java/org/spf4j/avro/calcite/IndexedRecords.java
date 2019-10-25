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

import com.google.common.collect.Maps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.IndexedRecord;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings({ "UCC_UNRELATED_COLLECTION_CONTENTS", "URV_UNRELATED_RETURN_VALUES" })
// this is how a calcite row is...
public final class IndexedRecords {

  private IndexedRecords() {
  }

  public static void copyRecord(final IndexedRecord from, final Object[] to) {
    for (Schema.Field field : from.getSchema().getFields()) {
      int pos = field.pos();
      Object val = from.get(pos);
      Schema fs = field.schema();
      to[pos] = copy(val, fs);
    }
  }

  @Nullable
  public static Object copy(@Nullable final Object from, @Nonnull final Schema schema) {
    LogicalType logicalType = schema.getLogicalType();
    if (logicalType != null) {
      switch (logicalType.getName()) {
        case "date":
          return ((LocalDate) from).toEpochDay();
        case "instant":
          return ((Instant) from).toEpochMilli();
        default:
          return from;
      }
    }
    switch (schema.getType()) {
      case BOOLEAN:
      case BYTES:
      case DOUBLE:
      case ENUM:
      case FIXED:
      case FLOAT:
      case INT:
      case LONG:
      case NULL:
      case STRING:
        return from;
      case ARRAY:
        Schema elType = schema.getElementType();
        Collection<Object> col = (Collection<Object>) from;
        return col.stream().map((x) -> copy(x, elType))
                .collect(Collectors.toCollection(() -> new ArrayList<>(col.size())));
      case MAP:
        Schema valueType = schema.getValueType();
        Map<String, Object> map = (Map<String, Object>) from;
        return map.entrySet().stream()
                .collect(Collectors.toMap((x) -> x.getKey(), (x) -> copy(x.getValue(), valueType),
                        (u, v) -> {
                          throw new IllegalStateException(String.format("Duplicate key %s", u));
                        },
                        () -> Maps.newHashMapWithExpectedSize(map.size())));
      case RECORD:
        Object[] res = new Object[schema.getFields().size()];
        copyRecord((IndexedRecord) from, res);
        return res;
      case UNION:
        Schema nSchema = org.spf4j.avro.schema.Schemas.nullableUnionSchema(schema);
        if (nSchema == null) {
          throw new UnsupportedOperationException("Not supported union " + schema);
        }
        if (from == null) {
          return null;
        }
        return copy(from, nSchema);
      default:
        throw new UnsupportedOperationException("Not supported schema " + schema);
    }
  }

  @Nullable
  public static Object from(final Schema schema, @Nullable final Object from) {
    LogicalType logicalType = schema.getLogicalType();
    if (logicalType != null) {
      switch (logicalType.getName()) {
        case "date":
          return LocalDate.ofEpochDay((Integer) from);
        case "instant":
          return Instant.ofEpochMilli((Long) from);
        default:
          return from;
      }
    }
    switch (schema.getType()) {
      case BOOLEAN:
      case BYTES:
      case DOUBLE:
      case ENUM:
      case FIXED:
      case FLOAT:
      case INT:
      case LONG:
      case NULL:
      case STRING:
        return from;
      case ARRAY:
        Schema elType = schema.getElementType();
        Collection<Object> col = (Collection<Object>) from;
        return col.stream().map((x) -> from(elType, x))
                .collect(Collectors.toCollection(() -> new ArrayList<>(col.size())));
      case MAP:
        Schema valueType = schema.getValueType();
        Map<String, Object> map = (Map<String, Object>) from;
        return map.entrySet().stream()
                .collect(Collectors.toMap((x) -> x.getKey(), (x) -> from(valueType, x.getValue()),
                        (u, v) -> {
                          throw new IllegalStateException(String.format("Duplicate key %s", u));
                        },
                        () -> Maps.newHashMapWithExpectedSize(map.size())));
      case RECORD:
        return fromRecord(schema, (Object[]) from);
      case UNION:
        Schema nSchema = org.spf4j.avro.schema.Schemas.nullableUnionSchema(schema);
        if (nSchema == null) {
          throw new UnsupportedOperationException("Not supported union " + schema);
        }
        if (from == null) {
          return null;
        }
        return from(nSchema, from);
      default:
        throw new UnsupportedOperationException("Not supported schema " + schema);
    }
  }

  public static GenericRecord fromRecord(final Schema schema, final Object[] from) {
    GenericData.Record record = new GenericData.Record(schema);
    for (Schema.Field field : schema.getFields()) {
      int pos = field.pos();
      Schema fs = field.schema();
      record.put(pos, from(fs, from[pos]));

    }
    return record;
  }

}
