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

import org.spf4j.base.TimeSource;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author Zoltan Farkas
 */
public class TimeoutRetryPredicate<T, C extends TimeoutCallable> implements RetryPredicate<T, C> {

  private final RetryPredicate<T, C> predicate;

  public TimeoutRetryPredicate(final RetryPredicate<T, C> predicate) {
    this.predicate = predicate;
  }

  @Override
  public RetryDecision<C> getDecision(final T value, final C what) {
    RetryDecision<C> decision = predicate.getDecision(value, what);
    if (decision.getDecisionType() == RetryDecision.Type.Retry) {
      long timeToDeadlineNanos = TimeSource.getTimeToDeadline(what.getDeadlineNanos(), TimeUnit.NANOSECONDS);
      if (timeToDeadlineNanos < decision.getDelayNanos()) {
        return RetryDecision.abort(new TimeoutException("Time to deadline not enough " + timeToDeadlineNanos + " ns"));
      }
    }
    return decision;
  }

  @Override
  public TimeoutRetryPredicate<T, C> newInstance() {
    return new TimeoutRetryPredicate<T, C>(predicate.newInstance());
  }

}
