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

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.apache.avro.generic.IndexedRecord;
import org.apache.calcite.DataContext;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.schema.ProjectableFilterableTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.avro.AvroDataSet;
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
  @SuppressWarnings({"unchecked", "unchecked"})
  public Enumerable<Object[]> scan(final DataContext root, final List<RexNode> filters,
          @Nullable final int[] projection) {
    LOG.debug("Filtered+Projected Table scan of {} with filter {} and projection {}", dataSet.getName(),
            filters, projection);
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
         predicate = new SqlRowPredicate(filters, rowType);
      } catch (RuntimeException ex) {
        LOG.debug("Unable to resulve filter {}", filters);
      }
      if (predicate != null) {
        if (features.contains(AvroDataSet.Feature.PROJECTABLE)) {
          List<String> projectionString = SqlConverters.projectionToString(projection, rowType);
          it = dataSet.getData((Predicate) predicate, projectionString, sc, timeoutMillis, TimeUnit.MINUTES);
        } else {
          it = dataSet.getData((Predicate) predicate, null, sc, timeoutMillis, TimeUnit.MINUTES);
        }
        filters.clear();
      } else if (features.contains(AvroDataSet.Feature.PROJECTABLE)) {
        List<String> projectionString = SqlConverters.projectionToString(projection, rowType);
        it = dataSet.getData((Predicate) null, projectionString, sc, timeoutMillis, TimeUnit.MINUTES);
      } else {
        it = dataSet.getData((Predicate) null, null, sc, timeoutMillis, TimeUnit.MINUTES);
      }
    } else if (features.contains(AvroDataSet.Feature.PROJECTABLE)) {
      List<String> projectionString = SqlConverters.projectionToString(projection, rowType);
      it = dataSet.getData((Predicate) null, projectionString, sc, timeoutMillis, TimeUnit.MINUTES);
    } else {
      it = dataSet.getData((Predicate) null, null, sc, timeoutMillis, TimeUnit.MINUTES);
    }
    return new AvroEnumerable(getComponentType(), root, () -> {
      return CloseableIterator.from((Iterator<IndexedRecord>) it.iterator(), it);
    }
    );

  }

  @Override
  public String toString() {
    return "AvroDataSetAsProjectableFilterableTable{" + "dataSet=" + dataSet + '}';
  }

}
