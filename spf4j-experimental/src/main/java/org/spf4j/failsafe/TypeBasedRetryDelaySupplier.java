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
 * A retry Delay supplier that will provide the same instance of a RetryDelaySupplier for a
 * particular java class.
 * 
 * @author Zoltan Farkas
 */
public final class TypeBasedRetryDelaySupplier<T> implements Function<T, RetryDelaySupplier> {

  private final Map<Class<T>, RetryDelaySupplier> delays;

  private final Function<Class<T>, RetryDelaySupplier> supplier;

  public TypeBasedRetryDelaySupplier(
          final Function<Class<T>, RetryDelaySupplier> supplier) {
    this.delays = new HashMap<>(4);
    this.supplier = supplier;
  }


  @Override
  public RetryDelaySupplier apply(final T t) {
    Class<T> clasz = (Class<T>) t.getClass();
    return delays.computeIfAbsent(clasz,
            (x) -> supplier.apply(x));
  }

  @Override
  public String toString() {
    return "TypeBasedBackoffSupplier{" + "delays=" + delays + ", supplier=" + supplier + '}';
  }


}
