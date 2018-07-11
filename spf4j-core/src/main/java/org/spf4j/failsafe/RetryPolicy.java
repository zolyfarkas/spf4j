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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;
import org.spf4j.base.Throwables;
import org.spf4j.failsafe.concurrent.DefaultContextAwareRetryExecutor;
import org.spf4j.failsafe.concurrent.RetryExecutor;

/**
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
@SuppressFBWarnings("FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY")
public class RetryPolicy<T, C extends Callable<? extends T>> implements SyncRetryExecutor<T, C> {

  private static final RetryPolicy<Object, Callable<? extends Object>> DEFAULT;

  static {
    RetryPolicy p;
    String policySupplierClass = System.getProperty("spf4j.failsafe.defaultRetryPolicySupplier");
    if (policySupplierClass == null) {
      p = RetryPolicy.newBuilder()
              .withDefaultThrowableRetryPredicate()
              .withRetryOnException(Exception.class, 2) // will retry any other exception twice.
              .build();
    } else {
      try {
        p = ((Supplier<RetryPolicy>) Class.forName(policySupplierClass).newInstance()).get();
      } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
        throw new ExceptionInInitializerError(ex);
      }
    }
    DEFAULT = p;
  }

  private static final RetryPolicy<Object, Callable<? extends Object>> NO_RETRY
          = RetryPolicy.newBuilder().build();



  private final TimedSupplier<RetryPredicate<T, C>> retryPredSupplier;

  private final int maxExceptionChain;

  RetryPolicy(final TimedSupplier<RetryPredicate<T, C>> retryPredicate,
          final int maxExceptionChain) {
    this.retryPredSupplier = retryPredicate;
    this.maxExceptionChain = maxExceptionChain;
  }

  public static <T, C extends Callable<? extends T>> RetryPolicy<T, C> noRetryPolicy() {
    return (RetryPolicy<T, C>) NO_RETRY;
  }

  public static <T, C extends Callable<? extends T>> RetryPolicy<T, C> defaultPolicy() {
    return (RetryPolicy<T, C>) DEFAULT;
  }

  @Override
  public final <R extends T, W extends C, EX extends Exception> R call(
          final W pwhat, final Class<EX> exceptionClass, final long startNanos, final long deadlineNanos)
          throws InterruptedException, TimeoutException, EX {
    return (R) SyncRetryExecutor.call(pwhat, getRetryPredicate(startNanos, deadlineNanos),
            exceptionClass, maxExceptionChain);
  }

  public final AsyncRetryExecutor<T, C> async(final RetryExecutor exec) {
    return new AsyncRetryExecutorImpl<>(this, exec);
  }


  public final AsyncRetryExecutor async() {
    return async(DefaultContextAwareRetryExecutor.instance());
  }

  public final RetryPredicate<T, C> getRetryPredicate(final long startTimeNanos, final long deadlineNanos) {
    return new TimeoutRetryPredicate(retryPredSupplier.get(startTimeNanos, deadlineNanos), deadlineNanos);
  }

  /**
   * @return string representation of policy.
   */
  @Override
  public String toString() {
    return "RetryPolicy{" + "retryPredicate=" + retryPredSupplier
            + ", maxExceptionChain=" + maxExceptionChain + '}';
  }

  public static final class Builder<T, C extends Callable<? extends T>> {

    private static final int MAX_EX_CHAIN_DEFAULT = Integer.getInteger("spf4j.failsafe.defaultMaxExceptionChain", 10);

    private static final long DEFAULT_MAX_DELAY_NANOS
            = TimeUnit.MILLISECONDS.toNanos(Long.getLong("spf4j.failsafe.defaultMaxRetryDelayMillis", 5000));

    private static final long DEFAULT_INITIAL_DELAY_NANOS
            = Long.getLong("spf4j.failsafe.defaultInitialRetryDelayNanos", 10000);

    private static final int DEFAULT_INITIAL_NODELAY_RETRIES
            = Integer.getInteger("spf4j.failsafe.defaultInitialNoDelayRetries", 3);

    private static final int DEFAULT_MAX_NR_RETRIES
            = Integer.getInteger("spf4j.failsafe.defaultMaxNrRetries", 1000);

    private int maxExceptionChain = MAX_EX_CHAIN_DEFAULT;

    private final List<TimedSupplier<? extends PartialResultRetryPredicate<T, C>>> resultPredicates;

    private final List<TimedSupplier<? extends PartialExceptionRetryPredicate<T, C>>> exceptionPredicates;

    private int nrInitialRetries;

    private long startDelayNanos;

    private long maxDelayNanos;

    private double jitterFactor;

    private Logger log;

    private Builder() {
      this.nrInitialRetries = DEFAULT_INITIAL_NODELAY_RETRIES;
      this.startDelayNanos = DEFAULT_INITIAL_DELAY_NANOS;
      this.maxDelayNanos = DEFAULT_MAX_DELAY_NANOS;
      this.jitterFactor = 0.2;
      this.resultPredicates = new ArrayList<>(2);
      this.exceptionPredicates = new ArrayList<>(2);
      this.log = null;
    }

    @CheckReturnValue
    public Builder<T, C> withRetryLogger(final Logger plog) {
      this.log = plog;
      return this;
    }

    @CheckReturnValue
    public Builder<T, C> withoutRetryLogger() {
      this.log = NOPLogger.NOP_LOGGER;
      return this;
    }



    @CheckReturnValue
    public Builder<T, C> withDefaultThrowableRetryPredicate() {
      return withDefaultThrowableRetryPredicate(DEFAULT_MAX_NR_RETRIES);
    }

    @CheckReturnValue
    public Builder<T, C> withDefaultThrowableRetryPredicate(final int maxNrRetries) {
      return withExceptionPartialPredicate((e, c) -> Throwables.isRetryable(e) ? RetryDecision.retryDefault(c) : null,
              maxNrRetries);
    }

    @CheckReturnValue
    public Builder<T, C> withRetryOnException(final Class<? extends Exception> clasz) {
      return withRetryOnException(clasz, DEFAULT_MAX_NR_RETRIES);
    }

    @CheckReturnValue
    public Builder<T, C> withRetryOnException(final Class<? extends Exception> clasz, final int maxRetries) {
      return withExceptionPartialPredicate((e, c)
              -> clasz.isAssignableFrom(e.getClass())
              ? RetryDecision.retryDefault(c) : null, maxRetries);
    }

    @CheckReturnValue
    public Builder<T, C> withRetryOnException(final Class<? extends Exception> clasz,
            final long maxTime, final TimeUnit tu) {
      return withExceptionPartialPredicate((e, c)
              -> clasz.isAssignableFrom(e.getClass())
              ? RetryDecision.retryDefault(c) : null, maxTime, tu);
    }

    @CheckReturnValue
    public Builder<T, C> withExceptionPartialPredicate(final PartialExceptionRetryPredicate<T, C> predicate,
            final long maxTime, final TimeUnit tu) {
      return withExceptionPartialPredicateSupplier((s, d)
              -> {
        TimeLimitedPartialRetryPredicate<T, Exception, C> p =
                new TimeLimitedPartialRetryPredicate<>(s, d, maxTime, tu, predicate);
        return (PartialExceptionRetryPredicate<T, C>) (Exception value, C what) -> p.apply(value, what);
      });
    }

    @CheckReturnValue
    public Builder<T, C> withExceptionPartialPredicate(
            final PartialExceptionRetryPredicate<T, C> predicate) {
      return withExceptionPartialPredicateSupplier(TimedSupplier.constant(predicate));
    }

    @CheckReturnValue
    public <E extends Exception> Builder<T, C> withExceptionPartialPredicate(final Class<E> clasz,
            final PartialTypedExceptionRetryPredicate<T, C, E> predicate) {
      return withExceptionPartialPredicate((e, c) -> {
        if (clasz.isAssignableFrom(e.getClass())) {
          return predicate.getExceptionDecision((E) e, c);
        }
        return null;
      });
    }

    @CheckReturnValue
    public <E extends Exception> Builder<T, C> withExceptionPartialPredicate(final Class<E> clasz,
            final PartialTypedExceptionRetryPredicate<T, C, E> predicate, final int maxRetries) {
      return withExceptionPartialPredicate((e, c) -> {
        if (clasz.isAssignableFrom(e.getClass())) {
          return predicate.getExceptionDecision((E) e, c);
        }
        return null;
      }, maxRetries);
    }

    @CheckReturnValue
    public Builder<T, C> withExceptionPartialPredicate(
            final PartialExceptionRetryPredicate<T, C> predicate,
            final int maxRetries) {
      return withExceptionPartialPredicateSupplier((s, e) -> {
        CountLimitedPartialRetryPredicate<T, Exception, C> p
                = new CountLimitedPartialRetryPredicate<T, Exception, C>(maxRetries, predicate);
        return (Exception value, C what) -> p.apply(value, what);
      });
    }

    @CheckReturnValue
    public Builder<T, C> withExceptionPartialPredicateSupplier(
            final Supplier<PartialExceptionRetryPredicate<T, C>> predicateSupplier) {
      return withExceptionPartialPredicateSupplier((s, e) -> predicateSupplier.get());
    }

    @Deprecated
    @CheckReturnValue
    public Builder<T, C> withExceptionStatefulPartialPredicate(
            final Supplier<PartialExceptionRetryPredicate<T, C>> predicateSupplier) {
      return Builder.this.withExceptionPartialPredicateSupplier(predicateSupplier);
    }

    @CheckReturnValue
    public Builder<T, C> withExceptionPartialPredicateSupplier(
            final TimedSupplier<PartialExceptionRetryPredicate<T, C>> predicateSupplier) {
      exceptionPredicates.add(predicateSupplier);
      return this;
    }

    @Deprecated
    @CheckReturnValue
    public Builder<T, C> withExceptionStatefulPartialPredicate(
            final TimedSupplier<PartialExceptionRetryPredicate<T, C>> predicateSupplier) {
      return withExceptionPartialPredicateSupplier(predicateSupplier);
    }


    @CheckReturnValue
    public Builder<T, C> withRetryOnResult(final T result, final int maxRetries) {
      return withResultPartialPredicate((r, c)
              -> Objects.equals(result, r)
              ? RetryDecision.retryDefault(c) : null, maxRetries);
    }


    @CheckReturnValue
    public Builder<T, C> withResultPartialPredicate(
            final PartialResultRetryPredicate<T, C> predicate) {
      resultPredicates.add(TimedSupplier.constant(predicate));
      return this;
    }

    @CheckReturnValue
    public Builder<T, C> withResultPartialPredicate(
            final PartialResultRetryPredicate<T, C> predicate,
            final int maxRetries) {
      return withResultPartialPredicateSupplier((s, e) -> {
        CountLimitedPartialRetryPredicate<T, T, C> p = new CountLimitedPartialRetryPredicate<>(maxRetries, predicate);
        return (T value, C what) -> p.apply(value, what);
      });
    }

    @CheckReturnValue
    public Builder<T, C> withResultPartialPredicateSupplier(
            final Supplier<PartialResultRetryPredicate<T, C>> predicateSupplier) {
      return withResultPartialPredicateSupplier((s, e) -> predicateSupplier.get());
    }

    @Deprecated
    @CheckReturnValue
    public Builder<T, C> withResultStatefulPartialPredicate(
            final Supplier<PartialResultRetryPredicate<T, C>> predicateSupplier) {
      return Builder.this.withResultPartialPredicateSupplier(predicateSupplier);
    }

    @CheckReturnValue
    public Builder<T, C> withResultPartialPredicateSupplier(
            final TimedSupplier<PartialResultRetryPredicate<T, C>> predicateSupplier) {
      resultPredicates.add(predicateSupplier);
      return this;
    }

    @Deprecated
    @CheckReturnValue
    public Builder<T, C> withResultStatefulPartialPredicate(
            final TimedSupplier<PartialResultRetryPredicate<T, C>> predicateSupplier) {
      return withResultPartialPredicateSupplier(predicateSupplier);
    }

    @CheckReturnValue
    public Builder<T, C> withJitterFactor(final double jitterfactor) {
      if (jitterFactor > 1 || jitterFactor < 0) {
        throw new IllegalArgumentException("Invalid jitter factor " + jitterfactor);
      }
      this.jitterFactor = jitterfactor;
      return this;
    }

    @CheckReturnValue
    public Builder<T, C> withInitialRetries(final int retries) {
      this.nrInitialRetries = retries;
      return this;
    }

    @CheckReturnValue
    public Builder<T, C> withInitialDelay(final long delay, final TimeUnit unit) {
      this.startDelayNanos = unit.toNanos(delay);
      return this;
    }

    @CheckReturnValue
    public Builder<T, C> withMaxDelay(final long delay, final TimeUnit unit) {
      this.maxDelayNanos = unit.toNanos(delay);
      return this;
    }

    public Builder<T, C> withMaxExceptionChain(final int maxExChain) {
      maxExceptionChain = maxExChain;
      return this;
    }

    @CheckReturnValue
    public RetryPolicy<T, C> build() {
      TimedSupplier[] rps = resultPredicates.toArray(new TimedSupplier[resultPredicates.size()]);
      TimedSupplier[] eps = exceptionPredicates.toArray(new TimedSupplier[exceptionPredicates.size()]);
      TimedSupplier<RetryPredicate<T, C>> retryPredicate
              = (s, e) -> new DefaultRetryPredicate(log, s, e, () -> new TypeBasedRetryDelaySupplier<>(
              (x) -> new JitteredDelaySupplier(new FibonacciRetryDelaySupplier(nrInitialRetries,
                      startDelayNanos, maxDelayNanos), jitterFactor)), rps, eps);
      return new RetryPolicy<>(retryPredicate, maxExceptionChain);
    }

    @CheckReturnValue
    public AsyncRetryExecutor<T, C> buildAsync() {
      return buildAsync(DefaultContextAwareRetryExecutor.instance());
    }

    @CheckReturnValue
    public AsyncRetryExecutor<T, C> buildAsync(final RetryExecutor es) {
       return build().async(es);
    }

  }

  /**
   * Create a retry policy builder.
   *
   * @param <T> the Type returned by the retried callables.
   * @param <C> the type of the Callable's returned.
   * @return
   */
  @CheckReturnValue
  public static <T, C extends Callable<? extends T>> Builder<T, C> newBuilder() {
    return new Builder<>();
  }

}
