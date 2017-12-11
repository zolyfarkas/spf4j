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

import java.net.SocketException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Throwables;

/**
 * @author Zoltan Farkas
 */
public class DefaultRetryPredicate implements RetryPredicate<Exception, Callable<?>> {

  private static final Logger LOG = LoggerFactory.getLogger(RetryPredicate.class);

  private static final long DEFAULT_MAX_DELAY_NANOS = TimeUnit.SECONDS.toNanos(5);

  private static final long DEFAULT_INITIAL_DELAY_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

  private static final int DEFAULT_INITIAL_NODELAY_RETRIES = 3;

  private final Predicate<Exception> defaultPredicate;

  private final Map<Class<? extends Exception>, BackoffDelay> typeBackoff;

  private final Function<Class<? extends Exception>, BackoffDelay> backoffSupplier;

  private final PartialRetryPredicate<Exception, Callable<?>> [] predicates;

  public DefaultRetryPredicate() {
    this(DefaultRetryPredicate::isRecoverable, (e) -> new RandomizedBackoff(
            new FibonacciBackoff(DEFAULT_INITIAL_NODELAY_RETRIES,
            DEFAULT_INITIAL_DELAY_NANOS, DEFAULT_MAX_DELAY_NANOS)));
  }

  public DefaultRetryPredicate(final Predicate<Exception> predicate,
          final Function<Class<? extends Exception>, BackoffDelay> backoffSupplier,
          final PartialRetryPredicate<Exception, Callable<?>> ... predicates) {
    this.defaultPredicate = predicate;
    this.typeBackoff = new HashMap<>(4);
    this.backoffSupplier = backoffSupplier;
    this.predicates = predicates;
  }

  @Override
  public RetryPredicate<Exception, Callable<?>> newInstance() {
    return new DefaultRetryPredicate();
  }

  public static final boolean isRecoverable(final Exception value) {
    Throwable rootCause = com.google.common.base.Throwables.getRootCause(value);
    if (rootCause instanceof RuntimeException) {
      return false;
    }
    Throwable e = Throwables.firstCause(value,
            (ex) -> (ex instanceof SQLTransientException
            || ex instanceof SQLRecoverableException
            || ex instanceof SocketException
            || ex instanceof TimeoutException));
    if (e != null) {
      return true;
    } else {
      return false;
    }

  }

  @Override
  public RetryDecision getDecision(final Exception value, final Callable what) {

    for (PartialRetryPredicate<Exception, Callable<?>> predicate : predicates) {
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
    if (defaultPredicate.test(value)) {
      LOG.debug("Exception encountered, retrying {}...", what, value);
      BackoffDelay backoff = typeBackoff.computeIfAbsent(value.getClass(), backoffSupplier);
      return RetryDecision.retry(backoff.nextDelay(), what);
    } else {
      return RetryDecision.abort(value);
    }
  }

}
