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
package org.spf4j.base;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import java.io.Closeable;
import java.util.Iterator;

/**
 * @author Zoltan Farkas
 */
@CleanupObligation
public interface CloseableIterable<T> extends Closeable, Iterable<T> {


  void close();

  static <T> CloseableIterable<T> from(final CloseableIterator<T> iterator) {
    return new CloseableIterable<T>() {
      @Override
      public void close() {
        iterator.close();
      }

      @Override
      public Iterator<T> iterator() {
        return iterator;
      }

    };
  }


  static <T> CloseableIterable<T> from(final Iterable<T> it) {
    return new CloseableIterable<T>() {
      @Override
      public void close() {
        // nothing to close;
      }

      @Override
      public Iterator<T> iterator() {
        return it.iterator();
      }
    };
  }

}
