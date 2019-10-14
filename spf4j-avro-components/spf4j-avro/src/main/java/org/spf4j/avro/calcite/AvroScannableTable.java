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

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import org.apache.avro.generic.IndexedRecord;
import org.apache.calcite.DataContext;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.config.CalciteConnectionConfig;
import org.apache.calcite.linq4j.AbstractEnumerable;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.rel.RelCollations;
import org.apache.calcite.rel.RelFieldCollation;
import org.apache.calcite.rel.RelFieldCollation.Direction;
import org.apache.calcite.rel.RelFieldCollation.NullDirection;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.ScannableTable;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.Statistic;
import org.apache.calcite.schema.Statistics;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.util.ImmutableBitSet;
import org.spf4j.base.CloseableIterable;

/**
 * @author Zoltan Farkas
 */
public final class AvroScannableTable implements ScannableTable {

  private final org.apache.avro.Schema componentType;
  private final Supplier<CloseableIterable<IndexedRecord>> stream;

  public AvroScannableTable(final org.apache.avro.Schema componentType,
          final Supplier<CloseableIterable<IndexedRecord>> stream) {
    if (componentType.getType() != org.apache.avro.Schema.Type.RECORD) {
      throw new IllegalArgumentException("Invalid table compoent " + componentType);
    }
    this.componentType = componentType;
    this.stream = stream;
  }

  @Override
  public RelDataType getRowType(final RelDataTypeFactory typeFactory) {
    return Types.from((JavaTypeFactory) typeFactory, componentType, new HashMap<>());
  }

  @Override
  public Statistic getStatistic() {
    Direction dir = Direction.ASCENDING;
    RelFieldCollation collation = new RelFieldCollation(0, dir, NullDirection.UNSPECIFIED);
    return Statistics.of(5, ImmutableList.of(ImmutableBitSet.of(0)),
            ImmutableList.of(RelCollations.of(collation)));
  }

  @Override
  public Schema.TableType getJdbcTableType() {
    return Schema.TableType.STREAM;
  }

  @Override
  public boolean isRolledUp(final String column) {
    return false;
  }

  @Override
  public boolean rolledUpColumnValidInsideAgg(final String column, final SqlCall call,
          final SqlNode parent, final CalciteConnectionConfig config) {
    return false;
  }

  @Override
  public Enumerable<Object[]> scan(final DataContext root) {
    return new AbstractEnumerable<Object[]>() {
      public Enumerator<Object[]> enumerator() {
        return new Enumerator<Object[]>() {
          private Object[] current = null;

          private CloseableIterable<IndexedRecord> iterable = stream.get();

          private Iterator<IndexedRecord> iterator = iterable.iterator();

          @Override
          public Object[] current() {
            if (current == null) {
              throw new IllegalStateException("Use moveNext on " + this);
            }
            return current;
          }

          @Override
          public boolean moveNext() {
            if (iterator.hasNext()) {
              IndexedRecord ir = iterator.next();
              List<org.apache.avro.Schema.Field> fields = ir.getSchema().getFields();
              int size = fields.size();
              current = new Object[size];
              for (org.apache.avro.Schema.Field f : fields) {
                int pos = f.pos();
                current[pos] = ir.get(pos);
              }
              return true;
            } else {
              current = null;
              return false;
            }
          }

          @Override
          public void reset() {
            iterable.close();
            iterable = stream.get();
            iterator = iterable.iterator();
            current = null;
          }

          @Override
          public void close() {
            iterable.close();
          }
        };
      }
    };

  }

  @Override
  public String toString() {
    return "AvroScannableTable{" + "componentType=" + componentType + ", stream=" + stream + '}';
  }

}
