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
import java.time.Instant;
import java.time.LocalDate;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.IndexedRecord;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("UCC_UNRELATED_COLLECTION_CONTENTS") // this is how a calcite row is...
public final class IndexedRecords {

  private IndexedRecords() { }

  public static void copy(final IndexedRecord from,  final Object[] to) {
    for (Schema.Field field : from.getSchema().getFields()) {
      int pos = field.pos();
      Object val = from.get(pos);
      Schema fs = field.schema();
      if (fs.getType() == Schema.Type.UNION) {
        fs = org.spf4j.avro.schema.Schemas.nullableUnionSchema(fs);
        if (fs == null) {
          throw new IllegalArgumentException("Unsupported field " + field);
        }
      }
      LogicalType logicalType = fs.getLogicalType();
      if (logicalType != null) {
        switch (logicalType.getName()) {
          case "date":
            to[pos] = ((LocalDate) val).toEpochDay();
            break;
          case "instant":
            to[pos] = ((Instant) val).toEpochMilli();
            break;
          default:
            to[pos] = val;
        }
      } else {
        if (fs.getType() == Schema.Type.RECORD) {
          Object[] recArr = new Object[fs.getFields().size()];
          copy((IndexedRecord) val, recArr);
          to[pos] = recArr;
        } else {
          to[pos] = val;
        }
      }
    }
  }

  public static GenericRecord from(final Schema schema,  final Object[] from) {
    GenericData.Record record = new GenericData.Record(schema);
    for (Schema.Field field : schema.getFields()) {
      int pos = field.pos();
      Schema fs = field.schema();
      if (fs.getType() == Schema.Type.UNION) {
        fs = org.spf4j.avro.schema.Schemas.nullableUnionSchema(fs);
        if (fs == null) {
          throw new IllegalArgumentException("Unsupported field " + field);
        }
      }
      LogicalType logicalType = fs.getLogicalType();
      if (logicalType != null) {
        switch (logicalType.getName()) {
          case "date":
            record.put(pos, LocalDate.ofEpochDay((Integer) from[pos]));
            break;
          case "instant":
            record.put(pos, Instant.ofEpochMilli((Long) from[pos]));
            break;
          default:
            record.put(pos, from[pos]);
        }
      } else {
        switch (fs.getType()) {
          case RECORD:
              Object recVal = from[pos];
              if (recVal instanceof IndexedRecord || recVal == null) {
                record.put(pos, recVal);
              } else {
                record.put(pos, from(fs, (Object[]) recVal));
              }
              break;
          default:
            record.put(pos, from[pos]);
            break;
        }
      }
    }
    return record;
  }

}
