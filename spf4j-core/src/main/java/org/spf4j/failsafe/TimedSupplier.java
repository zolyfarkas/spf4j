/*
 * Copyright 2018 SPF4J.
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
package org.spf4j.failsafe;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.function.Supplier;

/**
 * @author Zoltan Farkas
 */
@FunctionalInterface
public interface TimedSupplier<T> {

  T get(long startTimeNanos, long deadlineNanos);

  static <T> TimedSupplier<T> constant(T value) {
    return new TimedSupplier<T>() {
      @Override
      public T get(final long startTimeNanos, final long deadlineNanos) {
        return value;
      }

      @Override
      public String toString() {
        return "ConstTimedSupplier{" + value + '}';
      }
    };
  }

  @SuppressFBWarnings("FII_USE_METHOD_REFERENCE")
  static <T> TimedSupplier<T> fromSupplier(Supplier<T> supplier) {
       return (s, e) -> supplier.get();
  }


}
