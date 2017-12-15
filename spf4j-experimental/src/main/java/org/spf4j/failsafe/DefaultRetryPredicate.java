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
import java.util.concurrent.Callable;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Zoltan Farkas
 */
class DefaultRetryPredicate<T> implements RetryPredicate<T, Callable<?>> {

  private static final Logger LOG = LoggerFactory.getLogger(RetryPredicate.class);

  private final Map<Class<?>, BackoffDelay> typeBackoff;
  
  private final RetryPredicate<T, Callable<?>> defaultPredicate;

  private final Function<Class<?>, BackoffDelay> backoffSupplier;

  private final PartialRetryPredicate<T, Callable<?>> [] predicates;

  DefaultRetryPredicate(final RetryPredicate<T, Callable<?>> defaultPredicate,
          final Function<Class<?>, BackoffDelay> backoffSupplier,
          final PartialRetryPredicate<T, Callable<?>> ... predicates) {
    this.defaultPredicate = defaultPredicate;
    this.typeBackoff = new HashMap<>(4);
    this.backoffSupplier = backoffSupplier;
    this.predicates = predicates;
  }

  @Override
  public RetryPredicate<T, Callable<?>> newInstance() {
    return new DefaultRetryPredicate(defaultPredicate, backoffSupplier, predicates);
  }

  @Override
  public RetryDecision getDecision(final T value, final Callable<?> what) {

    for (PartialRetryPredicate<T, Callable<?>> predicate : predicates) {
      RetryDecision<Callable<?>> decision = predicate.getDecision(value, what);
      if (decision != null) {
        LOG.debug("Exception encountered, retrying {}...", what, value);
        if (decision.getDecisionType() == RetryDecision.Type.Retry && decision.getDelayNanos() < 0) {
          BackoffDelay backoff = typeBackoff.computeIfAbsent(value.getClass(), backoffSupplier);
          return RetryDecision.retry(backoff.nextDelay(), what);
        } else {
          return decision;
        }
      }
    }
    RetryDecision<Callable<?>> decision = defaultPredicate.getDecision(value, what);
    if (decision.getDecisionType() == RetryDecision.Type.Retry && decision.getDelayNanos() < 0) {
      LOG.debug("Exception encountered, retrying {}...", what, value);
      BackoffDelay backoff = typeBackoff.computeIfAbsent(value.getClass(), backoffSupplier);
      return RetryDecision.retry(backoff.nextDelay(), what);
    } else {
      return decision;
    }
  }

}
