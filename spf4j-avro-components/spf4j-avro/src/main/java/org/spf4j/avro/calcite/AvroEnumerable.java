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
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;
import org.apache.avro.generic.IndexedRecord;
import org.apache.calcite.DataContext;
import org.apache.calcite.linq4j.AbstractEnumerable;
import org.apache.calcite.linq4j.Enumerator;
import org.spf4j.base.CloseableIterator;

/**
 * @author Zoltan Farkas
 */
class AvroEnumerable extends AbstractEnumerable<Object[]> {

  private final Supplier<CloseableIterator<IndexedRecord>> stream;
  private final Supplier<Boolean> cancelFlag;
  private final int rowLength;

  AvroEnumerable(final org.apache.avro.Schema componentType,
          final DataContext root,
          final Supplier<CloseableIterator<IndexedRecord>> stream) {
    this.rowLength = componentType.getFields().size();
    this.stream = stream;
    Supplier<Boolean> contextFlag = DataContext.Variable.CANCEL_FLAG.get(root);
    cancelFlag = contextFlag == null ? () -> Boolean.FALSE : contextFlag;
  }

  public Enumerator<Object[]> enumerator() {
    return new Enumerator<Object[]>() {
      private Object[] current = null;

      private CloseableIterator<IndexedRecord> iterator = stream.get();

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
          throw new CancellationException("Operation cancelled on " + stream + " at " + Arrays.toString(current));
        }
        if (iterator.hasNext()) {
          IndexedRecord ir = iterator.next();
          current = new Object[rowLength];
          IndexedRecords.copy(ir, current);
          return true;
        } else {
          current = null;
          return false;
        }
      }

      @Override
      public void reset() {
        iterator.close();
        iterator = stream.get();
        current = null;
      }

      @Override
      public void close() {
        iterator.close();
      }
    };
  }

  @Override
  public String toString() {
    return "AvroEnumerable{" + "stream=" + stream + ", rowLength=" + rowLength + '}';
  }


}
