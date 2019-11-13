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

import com.google.common.collect.Iterables;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.apache.calcite.DataContext;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.schema.ProjectableFilterableTable;
import org.apache.calcite.schema.Statistic;
import org.apache.calcite.schema.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.avro.AvroDataSet;
import org.spf4j.avro.SqlPredicate;
import org.spf4j.avro.schema.Schemas;
import org.spf4j.base.CloseableIterable;
import org.spf4j.base.CloseableIterator;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.security.SecurityContext;

/**
 * An avro table when the data is provided by the provided Supplier of CloseableIterable of IndexedRecord
 *
 * @author Zoltan Farkas
 */
public final class AvroDataSetAsProjectableFilterableTable extends AbstractAvroTable
        implements ProjectableFilterableTable {

  private static final Logger LOG = LoggerFactory.getLogger(AvroDataSetAsProjectableFilterableTable.class);

  private final AvroDataSet<? extends IndexedRecord> dataSet;

  public AvroDataSetAsProjectableFilterableTable(final AvroDataSet<? extends IndexedRecord> dataSet) {
    super(dataSet.getElementSchema());
    this.dataSet = dataSet;
  }

  @Override
  public Statistic getStatistic() {
    long rowCountStatistic = dataSet.getRowCountStatistic();
    if (rowCountStatistic >= 0) {
      return Statistics.of(rowCountStatistic, Collections.EMPTY_LIST);
    } else {
      return Statistics.of(Collections.EMPTY_LIST);
    }
  }

  private  CloseableIterable<IndexedRecord> project(final CloseableIterable<IndexedRecord> iterable,
          @Nullable final int[] projection) {
    if (projection == null) {
      return iterable;
    }
    Schema schema = getComponentType();
    Schema nschema = Schemas.projectRecord(schema, projection);
    Iterable<IndexedRecord> transformed = Iterables.transform(iterable, (x) -> Schemas.project(nschema, schema, x));
    return CloseableIterable.from(transformed, iterable);
  }

  private void deprecations(@Nullable final int[] projection,
          final DataContext ctx) {
    Schema schema = getComponentType();
    if (projection == null) {
       EmbededDataContext.addDeprecations(schema, ctx);
    } else {
       Schema nschema = Schemas.projectRecord(schema, projection);
       EmbededDataContext.addDeprecations(nschema, ctx);
    }
  }


  @Override
  @SuppressWarnings({"unchecked", "unchecked"})
  public Enumerable<Object[]> scan(final DataContext root, final List<RexNode> filters,
          @Nullable final int[] projection) {
    LOG.debug("Filtered+Projected Table scan of {} with filter {} and projection {}", dataSet.getName(),
            filters, projection);
    deprecations(projection, root);
    RelDataType rowType = this.getRowType(root.getTypeFactory());
    Long timeoutMillis = DataContext.Variable.TIMEOUT.get(root);
    if (timeoutMillis == null) {
      timeoutMillis = ExecutionContexts.getTimeToDeadlineUnchecked(TimeUnit.MILLISECONDS);
    }
    SecurityContext sc = (SecurityContext) root.get("SecurityContext");
    if (sc == null) {
      sc = SecurityContext.NOAUTH;
    }
    CloseableIterable<IndexedRecord> it;
    Set<AvroDataSet.Feature> features = dataSet.getFeatures();
    if (features.contains(AvroDataSet.Feature.FILTERABLE)) {
      SqlRowPredicate predicate = null;
      try {
         if (!filters.isEmpty()) {
           predicate = new SqlRowPredicate(filters, rowType);
         }
      } catch (RuntimeException ex) {
        LOG.debug("Unable to resolve filter {}", filters, ex);
      }
      if (predicate != null) {
        if (features.contains(AvroDataSet.Feature.PROJECTABLE)) {
          List<String> projectionString = SqlConverters.projectionToString(projection, rowType);
          it = dataSet.getData(predicate, projectionString, sc, timeoutMillis, TimeUnit.MINUTES);
        } else {
          it = project(dataSet.getData(predicate, null, sc, timeoutMillis, TimeUnit.MINUTES), projection);
        }
        filters.clear();
      } else if (features.contains(AvroDataSet.Feature.PROJECTABLE)) {
        List<String> projectionString = SqlConverters.projectionToString(projection, rowType);
        it = dataSet.getData((SqlPredicate) null, projectionString, sc, timeoutMillis, TimeUnit.MINUTES);
      } else {
        it = project(dataSet.getData((SqlPredicate) null, null, sc, timeoutMillis, TimeUnit.MINUTES), projection);
      }
    } else if (features.contains(AvroDataSet.Feature.PROJECTABLE)) {
      List<String> projectionString = SqlConverters.projectionToString(projection, rowType);
      it = dataSet.getData((SqlPredicate) null, projectionString, sc, timeoutMillis, TimeUnit.MINUTES);
    } else {
      it = project(dataSet.getData((SqlPredicate) null, null, sc, timeoutMillis, TimeUnit.MINUTES), projection);
    }
    return new AvroEnumerable(projection == null ? rowType.getFieldCount() : projection.length, root, () -> {
        return CloseableIterator.from((Iterator<IndexedRecord>) it.iterator(), it);
      });
  }

  @Override
  public String toString() {
    return "AvroDataSetAsProjectableFilterableTable{" + "dataSet=" + dataSet + '}';
  }

}
