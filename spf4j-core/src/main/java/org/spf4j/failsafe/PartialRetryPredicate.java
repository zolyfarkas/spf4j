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
 * Retry predicate that can make a retry decision. (or no decision at all)
 * @author Zoltan Farkas
 */
public interface PartialRetryPredicate<T, C extends Callable<? extends T>> {

  /**
   * Get the RetryDecision for the result value returned by Callable C.
   * @param value the operation result.
   * @param what the operation.
   * @return
   */
  @Nullable
  default RetryDecision<T, C> getDecision(@Nullable T value, @Nonnull C what) {
    return null;
  }


  @Nullable
  default RetryDecision<T, C> getExceptionDecision(@Nonnull Exception value, @Nonnull C what) {
    return null;
  }

}
