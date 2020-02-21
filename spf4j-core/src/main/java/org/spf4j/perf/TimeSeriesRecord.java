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

import com.google.common.annotations.Beta;
import java.time.Instant;
import java.util.Iterator;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.spf4j.tsdb2.avro.Aggregation;

/**
 * @author Zoltan Farkas
 */
public interface TimeSeriesRecord extends GenericRecord {

  String MEASUREMENT_TYPE_PROP = "measurementType";
  String AGGREGATION_TYPE_PROP = "aggregation";
  String UNIT_TYPE_PROP = "unit";
  String FREQ_MILLIS_REC_PROP = "frequencyMillis";

  Instant getTimeStamp();

  long getLongValue(String column);

  double getDoubleValue(String column);

  static int getFrequencyMillis(final Schema schema) {
    Number freq = (Number) schema.getObjectProp(FREQ_MILLIS_REC_PROP);
    if (freq == null) {
      return -1;
    }
    return freq.intValue();
  }

  static String getUnit(final Schema schema) {
    String unit = (String) schema.getObjectProp(UNIT_TYPE_PROP);
    if (unit == null) {
      return "N/A";
    }
    return unit;
  }

  static TimeSeriesRecord from(final GenericRecord rec) {
    return new TimeSeriesRecord() {
      @Override
      public Instant getTimeStamp() {
        return (Instant) rec.get(0);
      }

      @Override
      public long getLongValue(final String column) {
        return ((Number) rec.get(column)).longValue();
      }

      @Override
      public double getDoubleValue(final String column) {
        return ((Number) rec.get(column)).doubleValue();
      }

      @Override
      public void put(final String key, final Object v) {
        rec.put(key, v);
      }

      @Override
      public Object get(final String key) {
        return rec.get(key);
      }

      @Override
      public void put(final int i, final Object v) {
        rec.put(i, v);
      }

      @Override
      public Object get(final int i) {
        return rec.get(i);
      }

      @Override
      public Schema getSchema() {
        return rec.getSchema();
      }
    };
  }

  /**
   * Temporary, until better implementation.
   * @param accumulator
   * @param r2
   */
  @Beta
  default void accumulate(final TimeSeriesRecord r2) {
    Schema recSchema = getSchema();
    Iterator<Schema.Field> it = recSchema.getFields().iterator();
    it.next();
    put(0, r2.get(0));
    while (it.hasNext()) {
      Schema.Field nf = it.next();
      int pos = nf.pos();
      Aggregation agg;
      String prop = nf.schema().getProp(AGGREGATION_TYPE_PROP);
      if (prop != null) {
        agg = Aggregation.valueOf(prop);
      } else {
        agg = inferAggregationFromName(nf, recSchema);
      }
      switch (agg) {
        case SUM:
          put(pos, ((Long) get(pos)) + ((Long) r2.get(pos)));
          break;
        case MIN:
          put(pos, Math.min((Long) get(pos), ((Long) r2.get(pos))));
          break;
        case MAX:
          put(pos, Math.max((Long) get(pos), ((Long) r2.get(pos))));
          break;
        case FIRST:
          break;
        case LAST:
        case UNKNOWN:
          put(pos, ((Long) r2.get(pos)));
          break;
        default:
          throw new UnsupportedOperationException("Unsupported aggregation: " + agg);
      }
    }
  }



  static Aggregation inferAggregationFromName(final Schema.Field nf, final Schema recSchema) {
    Aggregation agg;
    switch (nf.name()) {
      case "count":
      case "total":
        agg = Aggregation.SUM;
        break;
      case "min":
        agg = Aggregation.MIN;
        break;
      case "max":
        agg = Aggregation.MAX;
        break;
      default:
        String mType = recSchema.getProp(MEASUREMENT_TYPE_PROP);
        if (mType != null) {
          switch (mType) {
            case "COUNTER":
            case "SUMMARY":
              agg = Aggregation.SUM;
              break;
            default:
              agg = Aggregation.LAST;
          }
        } else {
          agg = Aggregation.LAST;
        }
    }
    return agg;
  }



}
