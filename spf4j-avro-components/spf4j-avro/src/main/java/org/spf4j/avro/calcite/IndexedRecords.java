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
import java.math.BigDecimal;
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
      to[pos] = fromAvroToCalcite(val, fs);
    }
  }

  @Nullable
  public static Object fromAvroToCalcite(@Nullable final Object avro, @Nonnull final Schema schema) {
    LogicalType logicalType = schema.getLogicalType();
    if (logicalType != null) {
      switch (logicalType.getName()) {
        case "date":
          return ((LocalDate) avro).toEpochDay();
        case "instant":
          return ((Instant) avro).toEpochMilli();
        case "decimal":
          return ((Number) avro).doubleValue();
        default:
          return avro;
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
        return avro;
      case ARRAY:
        Schema elType = schema.getElementType();
        Collection<Object> col = (Collection<Object>) avro;
        return col.stream().map((x) -> fromAvroToCalcite(x, elType))
                .collect(Collectors.toCollection(() -> new ArrayList<>(col.size())));
      case MAP:
        Schema valueType = schema.getValueType();
        Map<String, Object> map = (Map<String, Object>) avro;
        return map.entrySet().stream()
                .collect(Collectors.toMap((x) -> x.getKey(), (x) -> fromAvroToCalcite(x.getValue(), valueType),
                        (u, v) -> {
                          throw new IllegalStateException(String.format("Duplicate key %s", u));
                        },
                        () -> Maps.newHashMapWithExpectedSize(map.size())));
      case RECORD:
        Object[] res = new Object[schema.getFields().size()];
        copyRecord((IndexedRecord) avro, res);
        return res;
      case UNION:
        Schema nSchema = org.spf4j.avro.schema.Schemas.nullableUnionSchema(schema);
        if (nSchema == null) {
          throw new UnsupportedOperationException("Not supported union " + schema);
        }
        if (avro == null) {
          return null;
        }
        return fromAvroToCalcite(avro, nSchema);
      default:
        throw new UnsupportedOperationException("Not supported schema " + schema);
    }
  }

  @Nullable
  public static Object fromCalciteToAvro(final Schema schema, @Nullable final Object calc) {
    LogicalType logicalType = schema.getLogicalType();
    if (logicalType != null) {
      switch (logicalType.getName()) {
        case "date":
          return LocalDate.ofEpochDay((Integer) calc);
        case "instant":
          return Instant.ofEpochMilli((Long) calc);
        case "decimal":
          return BigDecimal.valueOf(((Number) calc).doubleValue());
        default:
          return calc;
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
        return calc;
      case ARRAY:
        Schema elType = schema.getElementType();
        Collection<Object> col = (Collection<Object>) calc;
        return col.stream().map((x) -> fromCalciteToAvro(elType, x))
                .collect(Collectors.toCollection(() -> new ArrayList<>(col.size())));
      case MAP:
        Schema valueType = schema.getValueType();
        Map<String, Object> map = (Map<String, Object>) calc;
        return map.entrySet().stream()
                .collect(Collectors.toMap((x) -> x.getKey(), (x) -> fromCalciteToAvro(valueType, x.getValue()),
                        (u, v) -> {
                          throw new IllegalStateException(String.format("Duplicate key %s", u));
                        },
                        () -> Maps.newHashMapWithExpectedSize(map.size())));
      case RECORD:
        return fromRecord(schema, (Object[]) calc);
      case UNION:
        Schema nSchema = org.spf4j.avro.schema.Schemas.nullableUnionSchema(schema);
        if (nSchema == null) {
          throw new UnsupportedOperationException("Not supported union " + schema);
        }
        if (calc == null) {
          return null;
        }
        return fromCalciteToAvro(nSchema, calc);
      default:
        throw new UnsupportedOperationException("Not supported schema " + schema);
    }
  }

  public static GenericRecord fromRecord(final Schema schema, final Object[] from) {
    GenericData.Record record = new GenericData.Record(schema);
    for (Schema.Field field : schema.getFields()) {
      int pos = field.pos();
      Schema fs = field.schema();
      record.put(pos, fromCalciteToAvro(fs, from[pos]));

    }
    return record;
  }

}
