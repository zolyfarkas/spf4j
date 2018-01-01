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
class DefaultRetryPredicate<T> implements RetryPredicate<T, Callable<?>> {

  private static final Logger LOG = LoggerFactory.getLogger(RetryPredicate.class);

  private final Function<T, RetryDelaySupplier> defaultBackoffSupplier;

  private final PartialRetryPredicate<T, Callable<?>> [] predicates;

  private final Supplier<Function<T, RetryDelaySupplier>> defaultBackoffSupplierSupplier;

  DefaultRetryPredicate(
          final Supplier<Function<T, RetryDelaySupplier>> defaultBackoffSupplierSupplier,
          final PartialRetryPredicate<T, Callable<?>> ... predicates) {
    this.defaultBackoffSupplier = defaultBackoffSupplierSupplier.get();
    this.defaultBackoffSupplierSupplier = defaultBackoffSupplierSupplier;
    this.predicates = predicates;
  }

  @Override
  public RetryPredicate<T, Callable<?>> newInstance() {
    PartialRetryPredicate<T, Callable<?>> [] p = predicates.clone();
    for (int i = 0; i < p.length; i++) {
      p[i] = p[i].newInstance();
    }
    return new DefaultRetryPredicate(defaultBackoffSupplierSupplier, p);
  }

  @Override
  public RetryDecision getDecision(final T value, final Callable<?> what) {

    for (PartialRetryPredicate<T, Callable<?>> predicate : predicates) {
      RetryDecision<Callable<?>> decision = predicate.getDecision(value, what);
      if (decision != null) {
        if (decision.getDecisionType() == RetryDecision.Type.Retry) {
          LOG.debug("Exception encountered, retrying {}...", what, value);
          if (decision.getDelayNanos() < 0) {
            RetryDelaySupplier backoff = defaultBackoffSupplier.apply(value);
            return RetryDecision.retry(backoff.nextDelay(), what);
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
