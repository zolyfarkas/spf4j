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

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

/**
 * @author Zoltan Farkas
 */
@FunctionalInterface
public interface ArrayWriter<T> extends Flushable, Closeable, Consumer<T> {

  void write(T t) throws IOException;

  default void accept(final T t) {
    try {
      write(t);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  default void close() throws IOException {
    // no close default
  }

  default void flush() throws IOException {
    // no flush default
  }

}
