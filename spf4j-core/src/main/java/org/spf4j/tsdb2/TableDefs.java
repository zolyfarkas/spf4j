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
package org.spf4j.tsdb2;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.spf4j.avro.AvroCompatUtils;
import org.spf4j.perf.TimeSeriesRecord;
import org.spf4j.tsdb2.avro.ColumnDef;
import org.spf4j.tsdb2.avro.DataRow;
import org.spf4j.tsdb2.avro.MeasurementType;
import org.spf4j.tsdb2.avro.Observation;
import org.spf4j.tsdb2.avro.TableDef;
import org.spf4j.tsdb2.avro.Type;

/**
 * @author Zoltan Farkas
 */
public final class TableDefs {

  private static final Schema INSTANT_SCHEMA
          = new Schema.Parser().parse("{\"type\":\"string\",\"logicalType\":\"instant\"}");

  private TableDefs() {
  }

  public static Schema createSchema(final TableDef td) {
    Schema recSchema = AvroCompatUtils.createRecordSchema(td.getName(), td.getDescription(), null, false, false);
    List<ColumnDef> columns = td.getColumns();
    List<Schema.Field> fields = new ArrayList<>(columns.size() + 1);
    fields.add(AvroCompatUtils.createField("ts", INSTANT_SCHEMA, "Measurement time stamp", null, true, false,
            Schema.Field.Order.IGNORE));
    for (ColumnDef cd : columns) {
      Type type = cd.getType();
      switch (type) {
        case DOUBLE:
          Schema schema = Schema.create(Schema.Type.DOUBLE);
          schema.addProp(TimeSeriesRecord.UNIT_TYPE_PROP, cd.getUnitOfMeasurement());
          schema.addProp(TimeSeriesRecord.AGGREGATION_TYPE_PROP, cd.getAggregation().toString());
          fields.add(AvroCompatUtils.createField(cd.getName(), schema, cd.getDescription(), null, true, false,
                  Schema.Field.Order.IGNORE));
          break;
        case LONG:
          schema = Schema.create(Schema.Type.LONG);
          schema.addProp(TimeSeriesRecord.UNIT_TYPE_PROP, cd.getUnitOfMeasurement());
          schema.addProp(TimeSeriesRecord.AGGREGATION_TYPE_PROP, cd.getAggregation().toString());
          fields.add(AvroCompatUtils.createField(cd.getName(), schema, cd.getDescription(), null, true, false,
                  Schema.Field.Order.IGNORE));
          break;
        default:
          throw new IllegalStateException("Invalid data type " + type);
      }
    }
    recSchema.setFields(fields);
    int sampleTime = td.getSampleTime();
    if (sampleTime > 0) {
      recSchema.addProp(TimeSeriesRecord.FREQ_MILLIS_REC_PROP, sampleTime);
    }
    recSchema.addProp(TimeSeriesRecord.MEASUREMENT_TYPE_PROP, getMeasurementType(td));
    return recSchema;
  }

  public static TimeSeriesRecord toRecord(final Schema rSchema, final long baseTs, final DataRow row) {
    GenericRecord rec = new GenericData.Record(rSchema);
    long ts = baseTs + row.getRelTimeStamp();
    rec.put(0, Instant.ofEpochMilli(ts));
    List<Long> nrs = row.getData();
    List<Schema.Field> fields = rSchema.getFields();
    for (int i = 1, l = fields.size(), j = 0; i < l; i++, j++) {
      Schema.Type type = fields.get(i).schema().getType();
      switch (type) {
        case DOUBLE:
          rec.put(i, Double.longBitsToDouble(nrs.get(j)));
          break;
        case LONG:
          rec.put(i, nrs.get(j));
          break;
        default:
          throw new IllegalStateException("Unsupported data type: " + type);
      }
    }
    return TimeSeriesRecord.from(rec);
  }


 public static TimeSeriesRecord toRecord(final Schema rSchema, final long baseTs, final Observation row) {
    GenericRecord rec = new GenericData.Record(rSchema);
    rec.put(0, Instant.ofEpochMilli(baseTs + row.getRelTimeStamp()));
    List<Long> nrs = row.getData();
    List<Schema.Field> fields = rSchema.getFields();
    for (int i = 1, l = fields.size(), j = 0; i < l; i++, j++) {
      Schema.Type type = fields.get(i).schema().getType();
      switch (type) {
        case DOUBLE:
          rec.put(i, Double.longBitsToDouble(nrs.get(j)));
          break;
        case LONG:
          rec.put(i, nrs.get(j));
          break;
        default:
          throw new IllegalStateException("Unsupported data type: " + type);
      }
    }
    return TimeSeriesRecord.from(rec);
  }

  @SuppressFBWarnings("STT_STRING_PARSING_A_FIELD")
  public static MeasurementType getMeasurementType(final TableDef info) {
    MeasurementType measurementType = info.getMeasurementType();
    if (measurementType != MeasurementType.UNTYPED) {
      return measurementType;
    }
    boolean hasCount = false;
    for (ColumnDef colDef : info.getColumns()) {
      String colName = colDef.getName();
      if (colName.startsWith("Q") && colName.contains("_")) {
        return MeasurementType.HISTOGRAM;
      } else if ("sum".equals(colName)) {
        return MeasurementType.SUMMARY;
      } else if ("count".equals(colName)) {
        hasCount = true;
      }
    }
    if (hasCount) {
      return MeasurementType.COUNTER;
    }
    return MeasurementType.UNTYPED;
  }

}
