/*
 * Copyright 2017 SPF4J.
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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Zoltan Farkas
 */
public final class TypeBasedBackoffSupplier<T> implements Function<T, BackoffDelay> {

  private final Map<Class<T>, BackoffDelay> delays;

  private final Function<T, BackoffDelay> supplier;

  public TypeBasedBackoffSupplier(final int immediateLeft,
          final Function<T, BackoffDelay> supplier) {
    this.delays = new HashMap<>(4);
    this.supplier = supplier;
  }


  @Override
  public BackoffDelay apply(final T t) {
    return delays.computeIfAbsent((Class<T>) t.getClass(),
            (cl) -> supplier.apply(t));
  }

  @Override
  public String toString() {
    return "TypeBasedBackoffSupplier{" + "delays=" + delays + ", supplier=" + supplier + '}';
  }


}
