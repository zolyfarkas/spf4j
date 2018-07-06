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
import org.spf4j.base.TimeSource;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Zoltan Farkas
 */
final class TimeoutRetryPredicate<T, C extends Callable<T>> implements RetryPredicate<T, C> {

  private final RetryPredicate<T, C> predicate;


  private final long deadlineNanos;

  TimeoutRetryPredicate(final RetryPredicate<T, C> predicate, final long deadlineNanos) {
    this.predicate = predicate;
    this.deadlineNanos = deadlineNanos;
  }

  @Override
  public RetryDecision<T, C> getDecision(final T value, final C what) {
    RetryDecision<T, C> decision = predicate.getDecision(value, what);
    if (decision.getDecisionType() == RetryDecision.Type.Retry) {
      long timeToDeadlineNanos = TimeSource.getTimeToDeadline(deadlineNanos, TimeUnit.NANOSECONDS);
      if (timeToDeadlineNanos < decision.getDelayNanos()) {
         return (RetryDecision) RetryDecision.abortThrow(new TimeoutException("Time to deadline not enough "
                  + timeToDeadlineNanos + " ns, last result = " + value));
      }
    }
    return decision;
  }

  @Override
  public RetryDecision<T, C> getExceptionDecision(final Exception value, final C what) {
    RetryDecision<T, C> decision = predicate.getExceptionDecision(value, what);
    if (decision.getDecisionType() == RetryDecision.Type.Retry) {
      long timeToDeadlineNanos = TimeSource.getTimeToDeadline(deadlineNanos, TimeUnit.NANOSECONDS);
      if (timeToDeadlineNanos < decision.getDelayNanos()) {
          return (RetryDecision) RetryDecision.abortThrow(new TimeoutException("Time to deadline not enough "
                  + timeToDeadlineNanos + " ns "));
      }
    }
    return decision;
  }

  @Override
  public String toString() {
    return "TimeoutRetryPredicate{" + "predicate=" + predicate + ", deadlineNanos=" + deadlineNanos + '}';
  }

}
