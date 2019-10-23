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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.avro.generic.IndexedRecord;
import org.apache.calcite.DataContext;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.config.CalciteConnectionConfig;
import org.apache.calcite.interpreter.JaninoRexCompiler;
import org.apache.calcite.interpreter.Scalar;
import org.apache.calcite.interpreter.Spf4jDataContext;
import org.apache.calcite.linq4j.AbstractEnumerable;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.schema.ProjectableFilterableTable;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.Statistic;
import org.apache.calcite.schema.Statistics;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.CloseableIterable;

/**
 * Implementation that will scan a  stream of avro objects.
 * will need to review if QueriableTable is something that fits the bill.
 * https://www.programcreek.com/java-api-examples/?code=vlsi/mat-calcite-plugin/
 * mat-calcite-plugin-master/MatCalcitePlugin/src/com/github/vlsi/mat/calcite/functions/TableFunctions.java
 * @author Zoltan Farkas
 */
public final class AvroProjectableFilterableTable implements  ProjectableFilterableTable {

  private static final Logger LOG = LoggerFactory.getLogger(AvroProjectableFilterableTable.class);

  private final org.apache.avro.Schema componentType;
  private final Supplier<CloseableIterable<IndexedRecord>> stream;

  public AvroProjectableFilterableTable(final org.apache.avro.Schema componentType,
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
  @Nonnull
  public Statistic getStatistic() {
    return Statistics.UNKNOWN;
  }

  @Override
  public Schema.TableType getJdbcTableType() {
    return Schema.TableType.TABLE;
  }

  @Override
  public boolean isRolledUp(final String column) {
    return false;
  }

  @Override
  public boolean rolledUpColumnValidInsideAgg(final String column, final SqlCall call,
          final SqlNode parent, final CalciteConnectionConfig config) {
    return true;
  }

  @Override
  public String toString() {
    return "AvroScannableTable{" + "componentType=" + componentType + ", stream=" + stream + '}';
  }


  @Override
  public Enumerable<Object[]> scan(final DataContext root, final List<RexNode> filters,
          @Nullable final int[] projects) {
    LOG.debug("Filtered+Projected Table scan of {} with filter {} and projection {}", componentType.getName(),
            filters, projects);
    JavaTypeFactory typeFactory = root.getTypeFactory();
    Scalar scalar;
    if (filters.isEmpty()) {
      scalar = null;
    } else {
      RexBuilder rb = new RexBuilder(typeFactory);
      JaninoRexCompiler compiler = new JaninoRexCompiler(rb);
      scalar = compiler.compile(filters, getRowType(typeFactory));
    }
    return new AbstractEnumerableImpl(componentType, root, scalar, projects, stream);
  }

  private static class AbstractEnumerableImpl extends AbstractEnumerable<Object[]> {

    private final Object[] rawRow;
    private final Spf4jDataContext spf4jDataContext;
    private final Scalar scalar;
    private final int[] projects;
    private final Supplier<CloseableIterable<IndexedRecord>> stream;
    private final Supplier<Boolean> cancelFlag;

    AbstractEnumerableImpl(final org.apache.avro.Schema componentType,
            final DataContext root,
            @Nullable final Scalar scalar, final int[] projects,
            final Supplier<CloseableIterable<IndexedRecord>> stream) {
      this.rawRow = new Object[componentType.getFields().size()];
      this.spf4jDataContext = new Spf4jDataContext(root);
      this.scalar = scalar;
      this.projects = projects;
      this.stream = stream;
      Supplier<Boolean> contextFlag = DataContext.Variable.CANCEL_FLAG.get(root);
      cancelFlag = contextFlag == null ? () -> Boolean.FALSE  : contextFlag;
    }

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
          if (cancelFlag.get()) {
            throw new CancellationException("Operation cancelled on " + stream + " at "  + Arrays.toString(current));
          }
          if (iterator.hasNext()) {
            while (true) {
              IndexedRecord ir = iterator.next();
              IndexedRecords.copy(ir, rawRow);
              spf4jDataContext.values = rawRow;
              Boolean match = scalar == null || (Boolean) scalar.execute(spf4jDataContext);
              if (match) {
                break;
              }
              if (!iterator.hasNext()) {
                current = null;
                return false;
              }
            }
            if (projects  == null) {
              current = rawRow.clone();
            } else {
              current = new Object[projects.length];
              for (int i = 0; i < projects.length; i++) {
                current[i] = rawRow[projects[i]];
              }
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
  }

}
