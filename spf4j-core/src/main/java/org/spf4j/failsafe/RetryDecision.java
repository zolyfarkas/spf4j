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
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.spf4j.base.Either;

/**
 *
 * @author Zoltan Farkas
 */
public interface RetryDecision<T, C extends Callable<? extends T>> {

  AbortRetryDecision ABORT = new AbortRetryDecision();

  class AbortRetryDecision<T, C extends Callable<? extends T>> implements RetryDecision {

    private final Either<Throwable, T> result;

    AbortRetryDecision(final Throwable exception) {
      this.result =  Either.left(exception);
    }

    AbortRetryDecision(final T value) {
      this.result =  Either.right(value);
    }

    AbortRetryDecision() {
       this.result = null;
    }

    @Override
    public Type getDecisionType() {
      return Type.Abort;
    }

    @Override
    public long getDelayNanos() {
      throw new UnsupportedOperationException();
    }

    @Override
    public C getNewCallable() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Either<Throwable, T> getResult() {
      return result;
    }

    @Override
    public String toString() {
      return "ABORT(" + result + ')';
    }
  }


  /**
   * @return abort retry decision, the execution will abort with the last return or exception.
   */
  @CheckReturnValue
  static RetryDecision abort() {
    return ABORT;
  }

/**
   * Abort operation with a custom Exception.
   * @param exception the custom exception.
   * @return a Abort decision with a custom Exception.
   */
  @CheckReturnValue
  static RetryDecision abortThrow(@Nonnull final Throwable exception) {
    return new AbortRetryDecision(exception);
  }

  /**
   * Abort operation and return the provided Object.
   * @param result
   * @return
   */
  @CheckReturnValue
  static <T, C extends Callable<? extends T>> RetryDecision<T, C> abortReturn(final T result) {
    return new AbortRetryDecision(result);
  }

  @CheckReturnValue
  static <T, C extends Callable<? extends T>> RetryDecision<T, C> retry(
          @Nonnegative final long time, final TimeUnit unit, @Nonnull final C callable) {
    return retry(unit.toNanos(time), callable);
  }

  @CheckReturnValue
  static <T, C extends Callable<? extends T>> RetryDecision<T, C> retry(
          @Nonnegative final long retryNanos, @Nonnull final C callable) {
   return new RetryDecision<T, C>() {
     @Override
     public Type getDecisionType() {
       return Type.Retry;
     }

     @Override
     public long getDelayNanos() {
       return retryNanos;
     }

     @Override
     public Either<Throwable, T> getResult() {
       throw new UnsupportedOperationException();
     }

     @Override
     public C getNewCallable() {
       return callable;
     }

     @Override
     public String toString() {
       return "RETRY(" + retryNanos + "ns, " + callable + ')';
     }

   };
  }

  /**
   * The default retry algo will be used.
   * @param <T>
   * @param <C>
   * @param callable
   * @return
   */
  @CheckReturnValue
  static <T, C extends Callable<? extends T>> RetryDecision<T, C> retryDefault(@Nonnull final C callable) {
   return new RetryDecision<T, C>() {
     @Override
     public Type getDecisionType() {
       return Type.Retry;
     }

     @Override
     public long getDelayNanos() {
       return -1;
     }

     @Override
     public Either<Throwable, T> getResult() {
       throw new UnsupportedOperationException();
     }

     @Override
     public C getNewCallable() {
       return callable;
     }

     @Override
     public String toString() {
       return "RETRY(" + callable + ')';
     }

   };
  }

  enum Type {
    /** Do not retry operation*/
    Abort,
    /** Retry operation */
    Retry
  }


  @CheckReturnValue
  @Nonnull
  Type getDecisionType();

  /**
   * @return The delay in nanoseconds, can be negative, if delay value is not decided.
   * negative value can be returned only by a Partial retry predicate.
   */
  @CheckReturnValue
  long getDelayNanos();

  /**
   * @return The result returned or thrown if applicable. When null the last result(or throwable) is returned.
   */
  @CheckReturnValue
  @Nullable
  Either<Throwable, T> getResult();

  /**
   * A retry decision the decides to retry, can provide a New Callable to retry.
   * @return the Callable that will be retried.
   */
  @CheckReturnValue
  @Nonnull
  C getNewCallable();


}
