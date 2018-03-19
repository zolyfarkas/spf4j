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
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.base.Throwables;
import org.spf4j.failsafe.concurrent.DefaultContextAwareRetryExecutor;
import org.spf4j.failsafe.concurrent.RetryExecutor;

/**
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
@SuppressFBWarnings("FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY")
public final class RetryPolicy<T, C extends Callable<? extends T>> implements PolicyExecutor<T, C> {

  private static final Supplier<PartialExceptionRetryPredicate<Object, Callable<Object>>> DEFAULT_TRANSIENT
          = () -> new PartialExceptionRetryPredicate<Object, Callable<Object>>() {
    @Override
    public RetryDecision getExceptionDecision(final Exception value, final Callable what) {
      return Throwables.isRetryable(value) ? RetryDecision.retryDefault(what) : null;
    }
  };

  private static final RetryPolicy DEFAULT = RetryPolicy.newBuilder()
          .withDefaultThrowableRetryPredicate()
          .withRetryOnException(Exception.class, 2) // will retry any other exception twice.
          .build();

  private final Supplier<RetryPredicate<T, C>> retryPredicate;

  private final Supplier<RetryExecutor> execSupplier;

  private final int maxExceptionChain;

  private RetryPolicy(final Supplier<RetryPredicate<T, C>> retryPredicate,
          final Supplier<RetryExecutor> execSupplier,
          final int maxExceptionChain) {
    this.retryPredicate = retryPredicate;
    this.maxExceptionChain = maxExceptionChain;
    this.execSupplier = execSupplier;
  }

  public static <T> RetryPolicy<T, Callable<? extends T>> defaultPolicy() {
    return DEFAULT;
  }

  @Override
  public <R extends T, W extends C, EX extends Exception> R call(
          final W pwhat, final Class<EX> exceptionClass)
          throws InterruptedException, TimeoutException, EX {
    return (R) SyncRetry.call(pwhat, getRetryPredicate(), exceptionClass, maxExceptionChain);
  }

  @Override
  public <R extends T, W extends C, EX extends Exception> R call(
          final W pwhat, final Class<EX> exceptionClass, final long deadlineNanos)
          throws InterruptedException, TimeoutException, EX {
    return (R) SyncRetry.call(pwhat, getRetryPredicate(deadlineNanos), exceptionClass, maxExceptionChain);
  }


  @Override
  public <R extends T, W extends Callable<R>> Future<R> submit(final W pwhat) {
    return (Future<R>) execSupplier.get().submit((Callable) pwhat, (RetryPredicate) getRetryPredicate());
  }

  @Override
  public <R extends T, W extends Callable<R>> Future<R> submit(final W pwhat, final long deadlineNanos) {
    return (Future<R>) execSupplier.get().submit((Callable) pwhat, (RetryPredicate) getRetryPredicate(deadlineNanos));
  }

  public RetryPredicate<T, C> getRetryPredicate() {
    return retryPredicate.get();
  }

  public RetryPredicate<T, C> getRetryPredicate(final long deadlineNanos) {
    return new TimeoutRetryPredicate(retryPredicate.get(), deadlineNanos);
  }

  @Override
  public String toString() {
    return "RetryPolicy{" + "retryPredicate=" + retryPredicate + ", execSupplier=" + execSupplier
            + ", maxExceptionChain=" + maxExceptionChain + '}';
  }

  public static final class Builder<T, C extends Callable<? extends T>> {

    private static final int MAX_EX_CHAIN_DEFAULT = Integer.getInteger("spf4j.failsafe.defaultMaxExceptionChain", 10);

    private static final long DEFAULT_MAX_DELAY_NANOS
            = TimeUnit.MILLISECONDS.toNanos(Long.getLong("spf4j.failsafe.defaultMaxRetryDelayMillis", 5000));

    private static final long DEFAULT_INITIAL_DELAY_NANOS
            = Long.getLong("spf4j.failsafe.defaultInitialRetryDelayNanos", 1000);

    private static final int DEFAULT_INITIAL_NODELAY_RETRIES
            = Integer.getInteger("spf4j.failsafe.defaultInitialNoDelayRetries", 3);

    private int maxExceptionChain = MAX_EX_CHAIN_DEFAULT;

    private Supplier<RetryExecutor> execSupplier = () -> DefaultContextAwareRetryExecutor.instance();

    private final List<Supplier<? extends PartialResultRetryPredicate<T, C>>> resultPredicates;

    private final List<Supplier<? extends PartialExceptionRetryPredicate<T, C>>> exceptionPredicates;

    private int nrInitialRetries;

    private long startDelayNanos;

    private long maxDelayNanos;

    private double jitterFactor;

    private Builder() {
      this.nrInitialRetries = DEFAULT_INITIAL_NODELAY_RETRIES;
      this.startDelayNanos = DEFAULT_INITIAL_DELAY_NANOS;
      this.maxDelayNanos = DEFAULT_MAX_DELAY_NANOS;
      this.jitterFactor = 0.2;
      this.resultPredicates = new ArrayList<>(2);
      this.exceptionPredicates = new ArrayList<>(2);
    }

    @CheckReturnValue
    public Builder<T, C> withDefaultThrowableRetryPredicate() {
      exceptionPredicates.add((Supplier) DEFAULT_TRANSIENT);
      return this;
    }

    @CheckReturnValue
    public Builder<T, C> withRetryOnException(final Class<? extends Exception> clasz) {
      return withExceptionPartialPredicate((Exception e, C c)
              -> clasz.isAssignableFrom(e.getClass())
              ? RetryDecision.retryDefault(c) : null);
    }

    @CheckReturnValue
    public Builder<T, C> withRetryOnException(final Class<? extends Exception> clasz, final int maxRetries) {
      return withExceptionPartialPredicate((e, c)
              -> clasz.isAssignableFrom(e.getClass())
              ? RetryDecision.retryDefault(c) : null, maxRetries);
    }

    @CheckReturnValue
    public Builder<T, C> withExceptionPartialPredicate(
            final PartialExceptionRetryPredicate<T, C> predicate) {
      exceptionPredicates.add(() -> predicate);
      return this;
    }

    @CheckReturnValue
    public Builder<T, C> withExceptionPartialPredicate(
            final PartialExceptionRetryPredicate<T, C> predicate,
            final int maxRetries) {
      exceptionPredicates.add(() -> {
        CountLimitedPartialRetryPredicate<T, Exception, C> p
                = new CountLimitedPartialRetryPredicate<T, Exception, C>(maxRetries, predicate);
        return (Exception value, C what) -> p.apply(value, what);
      });
      return this;
    }

    @CheckReturnValue
    public Builder<T, C> withExceptionStatefulPartialPredicate(
            final Supplier<PartialExceptionRetryPredicate<T, C>> predicateSupplier) {
      exceptionPredicates.add(predicateSupplier);
      return this;
    }

    @CheckReturnValue
    public Builder<T, C> withResultPartialPredicate(
            final PartialResultRetryPredicate<T, C> predicate) {
      resultPredicates.add(() -> predicate);
      return this;
    }

    @CheckReturnValue
    public Builder<T, C> withResultPartialPredicate(
            final PartialResultRetryPredicate<T, C> predicate,
            final int maxRetries) {
      resultPredicates.add(() -> {
        CountLimitedPartialRetryPredicate<T, T, C> p = new CountLimitedPartialRetryPredicate<>(maxRetries, predicate);
        return (T value, C what) -> p.apply(value, what);
      });
      return this;
    }

    @CheckReturnValue
    public Builder<T, C> withResultStatefulPartialPredicate(
            final Supplier<PartialResultRetryPredicate<T, C>> predicateSupplier) {
      resultPredicates.add(predicateSupplier);
      return this;
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

    public Builder<T, C> withExecutorService(final RetryExecutor es) {
      execSupplier = () -> es;
      return this;
    }

    @CheckReturnValue
    @SuppressWarnings("unchecked")
    public RetryPolicy<T, C> build() {
      Supplier[] rps = resultPredicates.toArray(new Supplier[resultPredicates.size()]);
      Supplier[] eps = exceptionPredicates.toArray(new Supplier[exceptionPredicates.size()]);
      Supplier<RetryPredicate<T, C>> retryPredicate =
              () -> new DefaultRetryPredicate(() -> new TypeBasedRetryDelaySupplier<>(
              (x) -> new JitteredDelaySupplier(new FibonacciRetryDelaySupplier(nrInitialRetries,
                      startDelayNanos, maxDelayNanos), jitterFactor)), rps, eps);
      return new RetryPolicy<>(retryPredicate, execSupplier, maxExceptionChain
      );
    }

  }

  /**
   * Create a retry policy builder.
   * @param <T> the Type returned by the retried callables.
   * @param <C> the type of the Callable's returned.
   * @return
   */
  @CheckReturnValue
  public static <T, C extends Callable<T>> Builder<T, C> newBuilder() {
    return new Builder<>();
  }

}
