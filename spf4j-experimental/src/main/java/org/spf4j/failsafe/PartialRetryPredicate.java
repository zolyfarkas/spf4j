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

import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 *
 * @author Zoltan Farkas
 */
public interface PartialRetryPredicate<T, C extends Callable> {

  /**
   * Get the RetryDecision for the result value returned by Callable C.
   * @param value the operation result.
   * @param what the operation.
   * @return
   */
  @Nullable
  RetryDecision<C> getDecision(@Nullable T value, @Nonnull C what);

  /**
   * Factory method for a predicate. Predicates can be stateful or not (default).
   * @return a new instance of predicate.
   */
  @Nonnull
  default PartialRetryPredicate<T, C> newInstance() {
    return this;
  }


}
