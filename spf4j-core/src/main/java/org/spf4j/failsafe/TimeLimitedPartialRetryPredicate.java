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
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.base.TimeSource;
import org.spf4j.base.Timing;

/**
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
final class TimeLimitedPartialRetryPredicate<T, V, C extends Callable<? extends T>>
        implements BiFunction<V, C, RetryDecision<T, C>> {

  private final BiFunction<V, C, RetryDecision<T, C>> wrapped;
  private long deadlineNanos;

  TimeLimitedPartialRetryPredicate(final long startTimeNanos, final long deadlineNanos,
          final long time, final TimeUnit tu,
          final double maxTimeToRetryFactor,
          final BiFunction<V, C, RetryDecision<T, C>> wrapped) {
    this.wrapped = wrapped;
    long ttd = (long) ((deadlineNanos - startTimeNanos) * maxTimeToRetryFactor);
    if (time > 0) {
      long tun = tu.toNanos(time);
      this.deadlineNanos = (ttd < tun) ? startTimeNanos + ttd : startTimeNanos + tun;
    } else {
      this.deadlineNanos = startTimeNanos + ttd;
    }
  }

  @Override
  @Nullable
  public RetryDecision<T, C> apply(final V value, final C what) {
    RetryDecision<T, C> decision = wrapped.apply(value, what);
    if (decision == null) {
      return null;
    }
    if (decision.getDecisionType() == RetryDecision.Type.Abort) {
      return decision;
    }
    long currNanoTime = TimeSource.nanoTime();
    if (currNanoTime >= deadlineNanos) {
      Timing currentTiming = Timing.getCurrentTiming();
      return RetryDecision.abortThrow(new NotEnoughTimeToRetry("Past deadline: "
                + currentTiming.fromNanoTimeToInstant(currNanoTime) + ", deadline = "
                + currentTiming.fromNanoTimeToInstant(deadlineNanos)));
    }
    return decision;
  }

  @Override
  public String toString() {
    return "TimeLimitedPartialRetryPredicate{" + "wrapped=" + wrapped + ", nanosDeadline=" + deadlineNanos + '}';
  }

}
