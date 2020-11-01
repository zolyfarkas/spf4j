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
package org.spf4j.perf;

import java.time.Instant;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.spf4j.tsdb2.TableDefs;
import org.spf4j.tsdb2.avro.TableDef;

/**
 *
 * @author Zoltan Farkas
 */
public final class MeasurmentStoreUtils {

  private MeasurmentStoreUtils() { }

  public static Schema toSchema(final MeasurementsInfo info, final int sampleTimeMillis) {
    TableDef td = TableDefs.from(info, sampleTimeMillis, -1);
    return TableDefs.createSchema(td);
  }

  public static TimeSeriesRecord toRecord(final Schema rSchema, final long tsMillis, final long[] row) {
    GenericRecord rec = new GenericData.Record(rSchema);
    rec.put(0, Instant.ofEpochMilli(tsMillis));
    List<Schema.Field> fields = rSchema.getFields();
    for (int i = 1, l = fields.size(), j = 0; i < l; i++, j++) {
      Schema.Type type = fields.get(i).schema().getType();
      switch (type) {
        case DOUBLE:
          rec.put(i, Double.longBitsToDouble(row[j]));
          break;
        case LONG:
          rec.put(i, row[j]);
          break;
        default:
          throw new IllegalStateException("Unsupported data type: " + type);
      }
    }
    return TimeSeriesRecord.from(rec);
  }

}
