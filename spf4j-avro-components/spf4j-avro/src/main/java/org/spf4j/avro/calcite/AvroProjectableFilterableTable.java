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

import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.apache.avro.generic.IndexedRecord;
import org.apache.calcite.DataContext;
import org.apache.calcite.interpreter.Scalar;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.schema.ProjectableFilterableTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.CloseableIterator;

/**
 * An avro table when the data is provided by the provided Supplier of CloseableIterable of IndexedRecord
 * @author Zoltan Farkas
 */
public final class AvroProjectableFilterableTable extends AbstractAvroTable
        implements ProjectableFilterableTable {

  private static final Logger LOG = LoggerFactory.getLogger(AvroProjectableFilterableTable.class);

  private final Supplier<CloseableIterator<? extends IndexedRecord>> dataSupplier;

  public AvroProjectableFilterableTable(final org.apache.avro.Schema componentType,
          final Supplier<CloseableIterator<? extends IndexedRecord>> dataSupplier) {
    super(componentType);
    this.dataSupplier = dataSupplier;
  }

  @Override
  public Enumerable<Object[]> scan(final DataContext root, final List<RexNode> filters,
          @Nullable final int[] projection)  {
    org.apache.avro.Schema componentType = getComponentType();
    LOG.debug("Filtered+Projected Table scan of {} with filter {} and projection {}", componentType.getName(),
            filters, projection);
    Scalar filter = InterpreterUtils.toScalar(filters, root, componentType);
    Enumerable<Object[]> result
            = new FilteringProjectingAvroEnumerable(componentType, root, filter, projection, dataSupplier);
    if (filter != null) {
      filters.clear();
    }
    return result;
  }


  @Override
  public String toString() {
    return "AvroProjectableFilterableTable{" + "dataSupplier=" + dataSupplier + '}';
  }

}
