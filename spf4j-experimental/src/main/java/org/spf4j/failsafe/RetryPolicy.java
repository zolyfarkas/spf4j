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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
public class RetryPolicy<T, C extends Callable<T>> {

  private static final int MAX_EX_CHAIN_DEFAULT = Integer.getInteger("spf4j.failsafe.maxExceptionChain", 10);

  private final RetryPredicate<T, C> retryOnReturnVal;
  private final RetryPredicate<Exception, C> retryOnException;
  private final int maxExceptionChain;

  public RetryPolicy(final RetryPredicate<T, C> retryOnReturnVal,
          final RetryPredicate<Exception, C> retryOnException, final int maxExceptionChain) {
    this.retryOnReturnVal = retryOnReturnVal;
    this.retryOnException = retryOnException;
    this.maxExceptionChain = maxExceptionChain;
  }

  public RetryPolicy(final RetryPredicate<Exception, C> retryOnException) {
    this(RetryPredicate.NORETRY, retryOnException, MAX_EX_CHAIN_DEFAULT);
  }

  public final <EX extends Exception> T execute(final C pwhat, final Class<EX> exceptionClass)
          throws InterruptedException, TimeoutException, EX {
    return Retry.execute(pwhat, getRetryOnReturnVal(), getRetryOnException(), exceptionClass, maxExceptionChain);
  }

  public RetryPredicate<T, C> getRetryOnReturnVal() {
    return retryOnReturnVal.newInstance();
  }

  public RetryPredicate<Exception, C> getRetryOnException() {
    return retryOnException.newInstance();
  }


  public static final class Builder<T, C extends Callable<T>> {

    private static final long DEFAULT_MAX_DELAY_NANOS = TimeUnit.SECONDS.toNanos(5);

    private static final long DEFAULT_INITIAL_DELAY_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

    private static final int DEFAULT_INITIAL_NODELAY_RETRIES = 3;

    public final class PredicateBuilder<A, B extends Callable> {

      private Consumer<RetryPredicate<A, B>> consumer;

      private int nrInitialRetries;

      private long startDelayNanos;

      private long maxDelayNanos;

      private PredicateBuilder(final Consumer<RetryPredicate<A, B>> consumer) {
        this.consumer = consumer;
        this.nrInitialRetries = DEFAULT_INITIAL_NODELAY_RETRIES;
        this.startDelayNanos = DEFAULT_INITIAL_DELAY_NANOS;
        this.maxDelayNanos = DEFAULT_MAX_DELAY_NANOS;
      }

      private final List<PartialRetryPredicate<A, B>> predicates = new ArrayList<>(2);

      @CheckReturnValue
      public PredicateBuilder<A, B> withPartialPredicate(final PartialRetryPredicate<A, B> predicate) {
        predicates.add(predicate);
        return this;
      }

      @CheckReturnValue
      public PredicateBuilder<A, B> withInitialRetries(final int retries) {
        this.nrInitialRetries = retries;
        return this;
      }

      @CheckReturnValue
      public PredicateBuilder<A, B> withInitialDelay(final long delay, final TimeUnit unit) {
        this.startDelayNanos = unit.toNanos(delay);
        return this;
      }

      @CheckReturnValue
      public PredicateBuilder<A, B> withMaxDelay(final long delay, final TimeUnit unit) {
        this.maxDelayNanos = unit.toNanos(delay);
        return this;
      }


      @CheckReturnValue
      public Builder<T, C> finishPredicate() {
         consumer.accept(new DefaultRetryPredicate(() -> new TypeBasedRetryDelaySupplier<>(
                      (x) -> new RandomizedBackoff(new FibonacciRetryDelaySupplier(nrInitialRetries,
                      startDelayNanos, maxDelayNanos))),
                 predicates.toArray(new PartialRetryPredicate[predicates.size()])));
         return Builder.this;
      }

    }

    private RetryPredicate<T, C> resultPredicate = null;
    private RetryPredicate<Exception, C> exceptionPredicate = null;

    private int maxExceptionChain = MAX_EX_CHAIN_DEFAULT;

    public Builder<T, C> withMaxExceptionChain(final int maxExChain) {
      maxExceptionChain = maxExChain;
      return this;
    }

    @CheckReturnValue
    public  PredicateBuilder<T, C> resultPredicateBuilder() {
      return new PredicateBuilder<T, C>((x) -> resultPredicate = x);
    }

    @CheckReturnValue
    public  PredicateBuilder<Exception, C> exceptionPredicateBuilder() {
      return new PredicateBuilder<Exception, C>((x) -> exceptionPredicate = x);
    }


    @CheckReturnValue
    @SuppressWarnings("unchecked")
    public RetryPolicy<T, C> build(final Class<? extends C> clasz) {
      RetryPredicate<T, C> rp = resultPredicate == null ? RetryPredicate.NORETRY
              : resultPredicate;
      RetryPredicate<Exception, C> ep = exceptionPredicate == null ? RetryPredicate.NORETRY
              : exceptionPredicate;
      if (TimeoutCallable.class.isAssignableFrom(clasz)) {
        rp = new TimeoutRetryPredicate(rp);
        ep = new TimeoutRetryPredicate(ep);
      }
      return new RetryPolicy<>(rp, ep, maxExceptionChain);
    }

  }


  @CheckReturnValue
  public static <T, C extends Callable<T>> Builder<T, C> newBuilder() {
    return new Builder();
  }

}
