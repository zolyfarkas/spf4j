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

/**
 * @author Zoltan Farkas
 */
final class CountLimitedPartialRetryPredicate<T, C extends Callable<T>> implements PartialRetryPredicate<T, C> {

  private final PartialRetryPredicate<T, C> wrapped;
  private int count;

  CountLimitedPartialRetryPredicate(final int maxCount, final PartialRetryPredicate<T, C> wrapped) {
    this.wrapped = wrapped;
    this.count = maxCount;
  }

  @Override
  public RetryDecision<T, C> getDecision(final T value, final C what) {
    RetryDecision<T, C> decision = wrapped.getDecision(value, what);
    if (decision == null) {
      return null;
    }
    if (count <= 0) {
      return RetryDecision.abort();
    }
    if (decision.getDecisionType() == RetryDecision.Type.Retry) {
      count--;
    }
    return decision;
  }

  @Override
  public RetryDecision<T, C> getExceptionDecision(final Exception value, final C what) {
    RetryDecision<T, C> decision = wrapped.getExceptionDecision(value, what);
    if (decision == null) {
      return null;
    }
    if (count <= 0) {
      return RetryDecision.abort();
    }
    if (decision.getDecisionType() == RetryDecision.Type.Retry) {
      count--;
    }
    return decision;
  }

  @Override
  public String toString() {
    return "CountLimitedPartialRetryPredicate{wrapped=" + wrapped
            + ", count=" + count + '}';
  }

}
