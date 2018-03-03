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
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Zoltan Farkas
 */
class DefaultRetryPredicate<T> implements RetryPredicate<T, Callable<T>> {

  private static final Logger LOG = LoggerFactory.getLogger(RetryPredicate.class);

  private final Function<Object, RetryDelaySupplier> defaultBackoffSupplier;

  private final PartialRetryPredicate<T, Callable<T>> [] predicates;

  DefaultRetryPredicate(
          final Supplier<Function<Object, RetryDelaySupplier>> defaultBackoffSupplierSupplier,
          final Supplier<PartialRetryPredicate<T, Callable<T>>> ... predicates) {
    this.defaultBackoffSupplier = defaultBackoffSupplierSupplier.get();
    this.predicates = new PartialRetryPredicate[predicates.length];
    for (int i = 0; i < predicates.length; i++) {
      this.predicates[i] = predicates[i].get();
    }
  }

  @Override
  public RetryDecision<T, Callable<T>> getDecision(final T value, final Callable<T> what) {

    for (PartialRetryPredicate<T, Callable<T>> predicate : predicates) {
      RetryDecision<T, Callable<T>> decision = predicate.getDecision(value, what);
      if (decision != null) {
        if (decision.getDecisionType() == RetryDecision.Type.Retry) {
          Callable<?> newCallable = decision.getNewCallable();
          LOG.debug("Result {} for {} retrying {}", value, what, newCallable);
          if (decision.getDelayNanos() < 0) {
            RetryDelaySupplier backoff = defaultBackoffSupplier.apply(value);
            return (RetryDecision) RetryDecision.retry(backoff.nextDelay(), newCallable);
          } else {
            return decision;
          }
        } else {
          return decision;
        }
      }
    }
    return RetryDecision.abort();
  }

  @Override
  public RetryDecision<T, Callable<T>> getExceptionDecision(Exception value, Callable<T> what) {
    for (PartialRetryPredicate<T, Callable<T>> predicate : predicates) {
      RetryDecision<T, Callable<T>> decision = predicate.getExceptionDecision(value, what);
      if (decision != null) {
        if (decision.getDecisionType() == RetryDecision.Type.Retry) {
          Callable<?> newCallable = decision.getNewCallable();
          LOG.debug("Result {} for {} retrying {}", value, what, newCallable);
          if (decision.getDelayNanos() < 0) {
            RetryDelaySupplier backoff = defaultBackoffSupplier.apply(value);
            return (RetryDecision) RetryDecision.retry(backoff.nextDelay(), newCallable);
          } else {
            return decision;
          }
        } else {
          return decision;
        }
      }
    }
    return RetryDecision.abort();

  }

}
