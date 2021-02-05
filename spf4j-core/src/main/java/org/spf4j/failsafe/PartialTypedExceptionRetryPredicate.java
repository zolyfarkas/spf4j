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

import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A partial retry predicate for a Callable&lt;T&gt&
 * @author Zoltan Farkas
 * @param <T> the return type for the Callable that is retried.
 * @param <C> the Callable that is retried.
 * @param <E> The Exception type that this predicate applies to.
 */
@FunctionalInterface
public interface PartialTypedExceptionRetryPredicate<T, C extends Callable<? extends T>, E extends Throwable>
        extends BiFunction<E, C, RetryDecision<T, C>> {

  /**
   * @param value the exception instance to evaluate.
   * @param what the callable that failed with the exception.
   * @return the retry decision, null implies that this predicate made no decision,
   * and will defer to the next predicate
   */
  @Nullable
  RetryDecision<T, C> getExceptionDecision(@Nonnull E value, @Nonnull C what);

  @Override
  default RetryDecision<T, C> apply(final E t, final C u) {
    return getExceptionDecision(t, u);
  }

}
