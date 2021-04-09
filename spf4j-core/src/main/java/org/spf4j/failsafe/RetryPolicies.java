/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Additionally licensed with:
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
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.spf4j.base.Either;
import org.spf4j.base.ResultMatchers;
import org.spf4j.failsafe.avro.RetryParams;
import org.spf4j.failsafe.avro.RetryRule;

/**
 * a Factory with retry named retry Rules.
 *
 * @author Zoltan Farkas
 */
public final class RetryPolicies {

  private static final Map<String,
          Either<PartialExceptionRetryPredicateSupplier, PartialResultRetryPredicateSupplier>> REGISTRY;

  static {
    REGISTRY = load();
  }

  private RetryPolicies() { }

  private static Map<String,
         Either<PartialExceptionRetryPredicateSupplier, PartialResultRetryPredicateSupplier>> load() {
    Map<String, Either<PartialExceptionRetryPredicateSupplier, PartialResultRetryPredicateSupplier>> result
            = new HashMap<>();
    for (PartialExceptionRetryPredicateSupplier s
            : ServiceLoader.load(PartialExceptionRetryPredicateSupplier.class)) {
      result.put(s.getName(), Either.left(s));
    }
    for (PartialResultRetryPredicateSupplier s
            : ServiceLoader.load(PartialResultRetryPredicateSupplier.class)) {
      result.put(s.getName(), Either.right(s));
    }
    return result;
  }

  public static Either<PartialExceptionRetryPredicateSupplier, PartialResultRetryPredicateSupplier>
          getRetryRule(final String name) {
    return REGISTRY.get(name);
  }

  public static Either<PartialExceptionRetryPredicateSupplier, PartialResultRetryPredicateSupplier>
          getRetryRule(final RetryRule rule) {
    return getRetryRule(rule.getName());
  }

  @Nullable
  public static <T, C extends Callable<? extends T>>
          Either<TimedSupplier<PartialExceptionRetryPredicate<T, C>>,
         TimedSupplier<PartialResultRetryPredicate<T, C>>> getResultRetryPredicateSupplier(
                  final String resultName, final RetryParams rp) {
    Either<Predicate<Throwable>, Predicate<Object>> resPred = ResultMatchers.getThrowableResultMatcher(resultName);
    if (resPred == null) {
      return null;
    }

    if (resPred.isLeft()) {
      Predicate<Throwable> predicate = resPred.getLeft();
      return Either.left(new TimedSupplier<PartialExceptionRetryPredicate<T, C>>() {
        @Override
        public PartialExceptionRetryPredicate<T, C> get(final long startTimeNanos, final long deadlineNanos) {
          RetryDelaySupplier ds = new JitteredDelaySupplier(
                  new FibonacciRetryDelaySupplier(rp.getNrInitialImmediateRetries(),
                  rp.getStartDelayNanos(), rp.getMaxDelayNanos()), rp.getRetryDelayJitter());
          CountLimitedPartialRetryPredicate clp
                  = new CountLimitedPartialRetryPredicate<T, Throwable, C>(rp.getMaxNrRetries(),
                          new PartialExceptionRetryPredicate<T, C>() {
                    @Override
                    public RetryDecision getExceptionDecision(final Throwable value, final Callable what) {

                      if (predicate.test(value)) {
                        return RetryDecision.retry(ds.nextDelay(), what);
                      }
                      return null;
                    }

                  });
          TimeLimitedPartialRetryPredicate tlp
                  = new TimeLimitedPartialRetryPredicate<T, Throwable, C>(startTimeNanos, deadlineNanos,
                          rp.getMaxTimeToRetryNanos(), TimeUnit.NANOSECONDS, rp.getMaxTimeToRetryFactor(), clp);

          return (Throwable value, C what) -> tlp.apply(value, what);

        }
      });
    } else {
      Predicate<Object> predicate = resPred.getRight();
      return Either.right(new TimedSupplier<PartialResultRetryPredicate<T, C>>() {
        @Override
        public PartialResultRetryPredicate<T, C> get(final long startTimeNanos, final long deadlineNanos) {
          RetryDelaySupplier ds = new JitteredDelaySupplier(
                  new FibonacciRetryDelaySupplier(rp.getNrInitialImmediateRetries(),
                  rp.getStartDelayNanos(), rp.getMaxDelayNanos()), rp.getRetryDelayJitter());
          CountLimitedPartialRetryPredicate clp
                  = new CountLimitedPartialRetryPredicate<T, T, C>(rp.getMaxNrRetries(),
                          new PartialResultRetryPredicate<T, C>() {
                    @Override
                    public RetryDecision getDecision(final Object value, final Callable what) {

                      if (predicate.test(value)) {
                        return RetryDecision.retry(ds.nextDelay(), what);
                      }
                      return null;
                    }

                  });
          TimeLimitedPartialRetryPredicate tlp
                  = new TimeLimitedPartialRetryPredicate<T, T, C>(startTimeNanos, deadlineNanos,
                          rp.getMaxTimeToRetryNanos(), TimeUnit.NANOSECONDS, rp.getMaxTimeToRetryFactor(), clp);

          return (T value, C what) -> tlp.apply(value, what);

        }
      });
    }

  }


  public static <T, C extends Callable<? extends T>> RetryPolicy<T, C> create(
          final org.spf4j.failsafe.avro.RetryPolicy policy) {
   RetryPolicy.Builder<T, C> builder = RetryPolicy.newBuilder();
   builder.withMaxExceptionChain(policy.getMaxSupressedExceptions());
   for (Map.Entry<String, RetryParams> entry : policy.getResponse2RetryParams().entrySet()) {
     String reasonName = entry.getKey();
     Either<TimedSupplier<PartialExceptionRetryPredicate<T, C>>,
         TimedSupplier<PartialResultRetryPredicate<T, C>>> result =
             getResultRetryPredicateSupplier(reasonName, entry.getValue());
     if (result == null) {
       Logger.getLogger(RetryPolicies.class.getName())
               .log(Level.WARNING, "No reason: {0} defined, ignoring.", reasonName);
       continue;
     }
     if (result.isLeft()) {
       TimedSupplier<PartialExceptionRetryPredicate<T, C>> ets = result.getLeft();
       builder.withExceptionPartialPredicateSupplier(ets);
     } else {
       TimedSupplier<PartialResultRetryPredicate<T, C>> rts = result.getRight();
       builder.withResultPartialPredicateSupplier(rts);
     }
   }
   return builder.build();
  }

}
