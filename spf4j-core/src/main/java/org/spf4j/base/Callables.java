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
package org.spf4j.base;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Utility class for executing stuff with retry logic.
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
//CHECKSTYLE IGNORE RedundantThrows FOR NEXT 2000 LINES
public final class Callables {

  /**
   * @deprecated use RetryPolicy
   */
  @Deprecated
  public static final SimpleRetryPredicate<?> RETRY_FOR_NULL_RESULT = new SimpleRetryPredicate<Object>() {
    @Override
    public SimpleAction apply(final Object input) {
      return (input != null) ? SimpleAction.ABORT : SimpleAction.RETRY;
    }
  };

  /**
   * A decent default retry predicate. It might retry exceptions that might not be retriable.. (like IO exceptions
   * thrown by parser libraries for parsing issues...)
   */
  /**
   * @deprecated use RetryPolicy
   */
  @Deprecated
  public static final AdvancedRetryPredicate<Exception> DEFAULT_EXCEPTION_RETRY
          = new DefaultAdvancedRetryPredicateImpl();

  /**
   * @deprecated use RetryPolicy
   */
  @Deprecated
  public static final Predicate<Exception> DEFAULT_EXCEPTION_RETRY_PREDICATE
          = new Predicate<Exception>() {

    @Override
    @SuppressFBWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    public boolean test(final Exception t) {
      return DEFAULT_EXCEPTION_RETRY.apply(t) != AdvancedAction.ABORT;
    }

  };

  private Callables() { }




  /**
   * @deprecated use RetryPolicy
   */
  @Deprecated
  @Nullable
  public static <T, EX extends Exception> T executeWithRetry(final TimeoutCallable<T, EX> what,
          final int nrImmediateRetries,
          final int maxRetryWaitMillis, final Class<EX> exceptionClass)
          throws InterruptedException, EX, TimeoutException {
    return executeWithRetry(what, nrImmediateRetries, maxRetryWaitMillis,
            (TimeoutRetryPredicate<? super T, T>) TimeoutRetryPredicate.NORETRY_FOR_RESULT,
            DEFAULT_EXCEPTION_RETRY, exceptionClass);
  }

  /**
   * @deprecated use RetryPolicy
   */
  @Deprecated
  @Nullable
  public static <T> T executeWithRetry(final TimeoutCallable<T, RuntimeException> what,
          final int nrImmediateRetries, final int maxRetryWaitMillis)
          throws InterruptedException, TimeoutException {
    return executeWithRetry(what, nrImmediateRetries, maxRetryWaitMillis,
            (TimeoutRetryPredicate<? super T, T>) TimeoutRetryPredicate.NORETRY_FOR_RESULT,
            DEFAULT_EXCEPTION_RETRY, RuntimeException.class);
  }


  /**
   * @deprecated use RetryPolicy
   */
  @Deprecated
  @Nullable
  public static <T, EX extends Exception> T executeWithRetry(final TimeoutCallable<T, EX> what,
          final int nrImmediateRetries,
          final int maxRetryWaitMillis,
          final AdvancedRetryPredicate<Exception> retryOnException,
          final Class<EX> exceptionClass)
          throws InterruptedException, EX, TimeoutException {
    return executeWithRetry(what, nrImmediateRetries, maxRetryWaitMillis,
            (TimeoutRetryPredicate<? super T, T>) TimeoutRetryPredicate.NORETRY_FOR_RESULT,
            retryOnException, exceptionClass);
  }

  /**
   * @deprecated use RetryPolicy
   */
  @Deprecated
  @Nullable
  public static <T> T executeWithRetry(final TimeoutCallable<T, RuntimeException> what,
          final int nrImmediateRetries, final int maxRetryWaitMillis,
          final AdvancedRetryPredicate<Exception> retryOnException)
          throws InterruptedException, TimeoutException {
    return executeWithRetry(what, nrImmediateRetries, maxRetryWaitMillis,
            (TimeoutRetryPredicate<? super T, T>) TimeoutRetryPredicate.NORETRY_FOR_RESULT,
            retryOnException, RuntimeException.class);
  }

  /**
   * After the immediate retries are done, delayed retry with randomized Fibonacci values up to the specified max is
   * executed.
   *
   * @param <T> - the type returned by the Callable that is retried.
   * @param <EX> - the Exception thrown by the retried callable.
   * @param what - the callable to retry.
   * @param nrImmediateRetries - the number of immediate retries.
   * @param maxWaitMillis - maximum wait time in between retries.
   * @param retryOnReturnVal - predicate to control retry on return value;
   * @param retryOnException - predicate to retry on thrown exception.
   * @return the result of the callable.
   * @throws java.lang.InterruptedException - thrown if interrupted.
   * @throws EX - the exception declared to be thrown by the callable.
   * @deprecated use RetryPolicy
   */
  @Deprecated
  @Nullable
  public static <T, EX extends Exception> T executeWithRetry(final TimeoutCallable<T, EX> what,
          final int nrImmediateRetries, final int maxWaitMillis,
          final TimeoutRetryPredicate<? super T, T> retryOnReturnVal,
          final AdvancedRetryPredicate<Exception> retryOnException,
          final Class<EX> exceptionClass)
          throws InterruptedException, EX, TimeoutException {
    long deadline = what.getDeadline();
    return executeWithRetry(what,  new TimeoutRetryPredicate2RetryPredicate<>(deadline, retryOnReturnVal),
            new FibonacciBackoffRetryPredicate<>(retryOnException, nrImmediateRetries,
                    maxWaitMillis / 100, maxWaitMillis, Callables::rootClass, deadline,
                    () -> System.currentTimeMillis(), TimeUnit.MILLISECONDS),
    exceptionClass);
  }

  /**
   * @deprecated use RetryPolicy
   */
  @Deprecated
  private static final class RetryData {

    private int immediateLeft;

    private long p1;

    private long p2;

    private final long maxDelay;

    RetryData(final int immediateLeft, final long p1, final long maxDelay) {
      this.immediateLeft = immediateLeft;
      if (p1 < 1) {
        this.p1 = 0;
        this.p2 = 1;
      } else {
        this.p1 = p1;
        this.p2 = p1;
      }
      this.maxDelay = maxDelay;
    }

    private long nextDelay() {
      if (immediateLeft > 0) {
        immediateLeft--;
        return 0;
      } else if (p2 > maxDelay) {
        return maxDelay;
      } else {
        long result = p2;
        p2 = p1 + p2;
        p1 = result;
        return result;
      }
    }

  }

  /**
   * @deprecated use RetryPolicy
   */
  @Deprecated
  public static Class rootClass(final Exception f) {
    return com.google.common.base.Throwables.getRootCause(f).getClass();
  }

  /**
   * @deprecated use RetryPolicy
   */
  @Deprecated
  public static final class FibonacciBackoffRetryPredicate<T, R> implements RetryPredicate<T, R> {

    private final IntMath.XorShift32 random;

    private final AdvancedRetryPredicate<T> arp;

    private final int nrImmediateRetries;

    private final long maxWaitUnits;

    private final long minWaitUnits;

    private Map<Object, RetryData> retryRegistry;

    private final Function<T, ?> mapper;

    private final long deadline;

    private final LongSupplier currTimeSuplier;

    private final TimeUnit tu;

    public FibonacciBackoffRetryPredicate(final AdvancedRetryPredicate<T> arp,
            final int nrImmediateRetries, final long minWaitUnits, final long maxWaitUnits,
            final Function<T, ?> mapper, final long deadline, final LongSupplier currTimeSuplier,
            final TimeUnit tu) {
      this.arp = arp;
      this.nrImmediateRetries = nrImmediateRetries;
      this.maxWaitUnits = maxWaitUnits;
      this.minWaitUnits = minWaitUnits;
      retryRegistry = null;
      this.mapper = mapper;
      this.random = new IntMath.XorShift32();
      this.deadline = deadline;
      this.currTimeSuplier = currTimeSuplier;
      this.tu = tu;
    }

    private RetryData getRetryData(final T value, final AdvancedAction action) {
      Object rootCauseClass = mapper.apply(value);
      RetryData data = retryRegistry.get(rootCauseClass);
      if (data == null) {
        data = createRetryData(action);
        retryRegistry.put(rootCauseClass, data);
      }
      return data;
    }

    private RetryData createRetryData(final AdvancedAction action) {
      if (action == AdvancedAction.RETRY_DELAYED) {
        return new RetryData(0, minWaitUnits, maxWaitUnits);
      } else {
        return new RetryData(nrImmediateRetries, minWaitUnits, maxWaitUnits);
      }
    }

    @Override
    public RetryDecision<R> getDecision(final T value, final Callable<R> callable) {
      long currentTime = currTimeSuplier.getAsLong();
      if (currentTime > deadline) {
        return RetryDecision.abort(new TimeoutException("Deadline " + Instant.ofEpochMilli(deadline)
                + " passed, current time is " + Instant.ofEpochMilli(currentTime)));
      }
      if (retryRegistry == null) {
        retryRegistry = new HashMap<>();
      }
      AdvancedAction action = arp.apply(value, deadline);
      switch (action) {
        case ABORT:
          return RetryDecision.abort();
        case RETRY_IMMEDIATE:
          return RetryDecision.retry(0, callable);
        case RETRY_DELAYED:
        case RETRY:
          RetryData retryData = getRetryData(value, action);
          final long nextDelay = retryData.nextDelay();
          long delay = Math.min(nextDelay, deadline - currentTime);
          if (delay > 0) {
            delay = Math.abs(random.nextInt()) % delay;
          }
          if (currentTime + delay > deadline) {
            return RetryDecision.abort(new TimeoutException("No time left for retry "
                    + Instant.ofEpochMilli(deadline) + ' ' + tu
                + " passed, current time is " + Instant.ofEpochMilli(currentTime) + ' ' + tu));
          }
          return RetryDecision.retry(tu.toMillis(delay), callable);
        default:
          throw new UnsupportedOperationException("Unsupperted Retry Action " + action);

      }
    }

  }

  /**
   * @deprecated use RetryPolicy
   */
  @Deprecated
  @Nullable
  public static <T, EX extends Exception> T executeWithRetry(final TimeoutCallable<T, EX> what,
          final TimeoutRetryPredicate<? super T, T> retryOnReturnVal,
          final TimeoutRetryPredicate<Exception, T> retryOnException, final Class<EX> exceptionClass)
          throws InterruptedException, EX, TimeoutException {
    final long deadline = what.getDeadline();
    return executeWithRetry(what,
            new TimeoutRetryPredicate2RetryPredicate<>(deadline, retryOnReturnVal),
            new TimeoutRetryPredicate2RetryPredicate<>(deadline, retryOnException), exceptionClass);
  }

  /**
   * @deprecated use RetryPolicy
   */
  @Deprecated
  public abstract static class TimeoutCallable<T, EX extends Exception> implements CheckedCallable<T, EX> {

    private final long mdeadline;


    public TimeoutCallable(final int timeoutMillis) {
      mdeadline = overflowSafeAdd(System.currentTimeMillis(), timeoutMillis);
    }

    public TimeoutCallable(final long deadline) {
      mdeadline = deadline;

    }

    @Override
    public final T call() throws EX, InterruptedException, TimeoutException {
      return call(mdeadline);
    }

    /**
     * @param deadline millis since epoch.
     */
    public abstract T call(long deadline) throws EX, InterruptedException, TimeoutException;

    public final long getDeadline() {
      return mdeadline;
    }

  }

  /**
   * @deprecated use RetryPolicy
   */
  @Deprecated
  public enum AdvancedAction {
    RETRY, // Retry based on default policy. (can be immediate or delayed)
    RETRY_IMMEDIATE, // Do immediate retry
    RETRY_DELAYED, // Do delayed retry
    ABORT // Abort, no retry, return last value/exception
  }

  /**
   * @deprecated use RetryPolicy
   */
  @Deprecated
  public interface AdvancedRetryPredicate<T> {

    default AdvancedAction apply(final T value, final long deadline) {
      return apply(value);
    }

    AdvancedAction apply(T value);

    AdvancedRetryPredicate<?> NO_RETRY = new AdvancedRetryPredicate<Object>() {
      @Override
      public AdvancedAction apply(final Object value) {
        return AdvancedAction.ABORT;
      }
    };

  }

  /**
   * @deprecated use RetryPolicy
   */
  @Deprecated
  public static final class RetryDecision<R> {

    private static final RetryDecision ABORT = new RetryDecision(Type.Abort, -1, null, null);

    public enum Type {
      Abort, Retry
    }

    private final Type decisionType;

    private final long delayMillis;

    private final Exception exception;

    private final Callable<R> newCallable;

    private RetryDecision(final Type decisionType, final long delayMillis,
            final Exception exception, final Callable<R> newCallable) {
      this.decisionType = decisionType;
      this.delayMillis = delayMillis;
      this.exception = exception;
      this.newCallable = newCallable;
    }

    public static RetryDecision abort(final Exception exception) {
      return new RetryDecision(Type.Abort, -1, exception, null);
    }

    public static <R> RetryDecision<R> retry(final long retryMillis, @Nonnull final Callable<R> callable) {
      return new RetryDecision(Type.Retry, retryMillis, null, callable);
    }

    public static RetryDecision abort() {
      return ABORT;
    }

    public Type getDecisionType() {
      return decisionType;
    }

    public long getDelayMillis() {
      return delayMillis;
    }

    public Exception getException() {
      return exception;
    }

    @Nonnull
    public Callable<R> getNewCallable() {
      return newCallable;
    }

  }

  /**
   * @deprecated use RetryPolicy
   */
  @Deprecated
  public interface RetryPredicate<T, R> {

    /**
     * the number or millis of delay until the next retry, or -1 for abort.
     *
     * @param value
     * @return
     */
    @Nonnull
    RetryDecision<R> getDecision(T value, @Nonnull Callable<R> callable);

    RetryPredicate<Object, Object> NORETRY_DELAY_PREDICATE = new RetryPredicate<Object, Object>() {
      @Override
      public RetryDecision getDecision(final Object value, final Callable callable) {
         return RetryDecision.abort();
      }
    };
  }


  /**
   * @deprecated use RetryPolicy
   */
  @Deprecated
  @Nullable
  public static <T, EX extends Exception> T executeWithRetry(final TimeoutCallable<T, EX> what,
          final TimeoutRetryPredicate<Exception, T> retryOnException, final Class<EX> exceptionClass)
          throws InterruptedException, EX, TimeoutException {
    return executeWithRetry(what, (TimeoutRetryPredicate<T, T>) TimeoutRetryPredicate.NORETRY_FOR_RESULT,
            retryOnException, exceptionClass);
  }

  /**
   * @deprecated use RetryPolicy
   */
  @Deprecated
  public interface TimeoutRetryPredicate<T, R> {

    RetryDecision<R> getDecision(T value, long deadlineMillis, Callable<R> what);

    TimeoutRetryPredicate NORETRY_FOR_RESULT = new TimeoutRetryPredicate<Object, Object>() {

      @Override
      public RetryDecision<Object> getDecision(final Object value, final long deadline, final Callable<Object> what) {
        return RetryDecision.abort();
      }

    };

  }

  /**
   * @deprecated use RetryPolicy
   */
  @Deprecated
  static final class TimeoutRetryPredicate2RetryPredicate<T, R>
          implements  RetryPredicate<T, R> {

    private final long deadline;

    private final TimeoutRetryPredicate<T, R> predicate;

    TimeoutRetryPredicate2RetryPredicate(final long deadline, final TimeoutRetryPredicate<T, R> predicate) {
      this.deadline = deadline;
      this.predicate = predicate;
    }


    @Override
    public RetryDecision<R> getDecision(final T value, final Callable<R> callable) {
      return predicate.getDecision(value, deadline, callable);
    }

  }

  /**
   * A callable that will be retried.
   *
   * @param <T> - the type of the object returned by this callable.
   * @param <EX> - the exception type returned by this callable.
   */
  public interface CheckedCallable<T, EX extends Exception> extends Callable<T> {

    /**
     * the method that is retried.
     *
     * @return
     * @throws EX
     * @throws InterruptedException
     * @throws java.util.concurrent.TimeoutException
     */
    @Override
    T call() throws EX, InterruptedException, TimeoutException;

  }

  /**
   * @deprecated use RetryPolicy
   */
  @Deprecated
  public enum SimpleAction {
    RETRY, ABORT
  }

  /**
   * @deprecated use RetryPolicy
   */
  @Deprecated
  public interface SimpleRetryPredicate<T> {

    SimpleAction apply(T value)
            throws TimeoutException, InterruptedException;
  }

  /**
   * Naive implementation of execution with retry logic. a callable will be executed and retry attempted in current
   * thread if the result and exception predicates. before retry, a callable can be executed that can abort the retry
   * and finish the function with the previous result.
   *
   * @param <T> - The type of callable to retry result;
   * @param <EX> - the exception thrown by the callable to retry.
   * @param pwhat - the callable to retry.
   * @param retryOnReturnVal - the predicate to control retry on return value.
   * @param retryOnException - the predicate to return on retry value.
   * @return the result of the retried callable if successful.
   * @throws java.lang.InterruptedException - thrown if retry interrupted.
   * @throws EX - the exception thrown by callable.
   * @deprecated use RetryPolicy
   */
  @Deprecated
  @SuppressFBWarnings({ "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", "MDM_THREAD_YIELD" })
  @Nullable
  public static <T, EX extends Exception> T executeWithRetry(
          final CheckedCallable<T, EX> pwhat,
          final RetryPredicate<? super T, T> retryOnReturnVal,
          final RetryPredicate<Exception, T> retryOnException,
          final Class<EX> exceptionClass)
          throws InterruptedException, TimeoutException, EX {
    Callable<T> what = pwhat;
    T result = null;
    Exception lastEx = null; // last exception
    try {
      result = what.call();
    } catch (InterruptedException ex1) {
      throw ex1;
    } catch (Exception e) { // only EX and RuntimeException
      lastEx = e;
    }
    Exception lastExChain = lastEx; // last exception chained with all previous exceptions
    RetryDecision decision = null;
    //CHECKSTYLE IGNORE InnerAssignment FOR NEXT 5 LINES
    while ((lastEx != null
            && (decision = retryOnException.getDecision(lastEx, what)).getDecisionType()
                == RetryDecision.Type.Retry)
            || (lastEx == null && (decision = retryOnReturnVal.getDecision(result, what)).getDecisionType()
                == RetryDecision.Type.Retry)) {
      if (Thread.interrupted()) {
        throw new InterruptedException();
      }
      long delayMillis = decision.getDelayMillis();
      if (delayMillis > 0) {
        Thread.sleep(delayMillis);
      }
      what = decision.getNewCallable();
      result = null;
      lastEx = null;
      try {
        result = what.call();
      } catch (InterruptedException ex1) {
        throw ex1;
      } catch (Exception e) { // only EX and RuntimeException
        lastEx = e;
        if (lastExChain != null) {
          lastExChain = Throwables.suppress(lastEx, lastExChain);
        } else {
          lastExChain = lastEx;
        }
      }
    }
    if (decision == null) {
      throw new IllegalStateException("Decission should have ben initialized " + lastEx + ", " + result);
    }
    if (decision.getDecisionType() == RetryDecision.Type.Abort) {
        Exception ex = decision.getException();
        if (ex != null) {
          lastEx = ex;
          if (lastExChain != null) {
            lastExChain = Throwables.suppress(lastEx, lastExChain);
          } else {
            lastExChain = lastEx;
          }
        }
    }
    if (lastEx != null) {
      if (lastExChain instanceof RuntimeException) {
        throw (RuntimeException) lastExChain;
      } else if (lastExChain instanceof TimeoutException) {
        throw (TimeoutException) lastExChain;
      } else if (lastExChain == null) {
        return null;
      } else if (exceptionClass.isAssignableFrom(lastExChain.getClass())) {
        throw (EX) lastExChain;
      } else {
        throw new UncheckedExecutionException(lastExChain);
      }
    }
    return result;
  }

  public static <T> Callable<T> synchronize(final Callable<T> callable) {
    return new Callable<T>() {

      @Override
      public synchronized T call() throws Exception {
        return callable.call();
      }
    };
  }

  /**
   * This is a duplicate of guava Callables.threadRenaming ... will have to review for deprecation/removal.
   */
  public static <T> Callable<T> withName(final Callable<T> callable, final String name) {
    return new Callable<T>() {

      @Override
      public T call() throws Exception {
        Thread currentThread = Thread.currentThread();
        String origName = currentThread.getName();
        try {
          currentThread.setName(origName + '[' + name + ']');
          return callable.call();
        } finally {
          currentThread.setName(origName);
        }
      }

      @Override
      public String toString() {
        return name;
      }

    };
  }

  static long overflowSafeAdd(final long currentTime, final long timeout) {
    if (currentTime < 0) {
      throw new IllegalArgumentException("Time must be positive, not " + currentTime);
    }
    if (timeout < 0) {
      return currentTime;
    }
    long result = currentTime + timeout;
    if ((currentTime ^ timeout) < 0 || (currentTime ^ result) >= 0) {
      return result;
    } else {
      return Long.MAX_VALUE;
    }
  }

  public static <V> Callable<V> memorized(final Callable<V> source) {
    return new MemorizedCallable<>(source);
  }

  public static <V> Callable<V> constant(final V value) {
    return new ConstCallable(value);
  }

  public static Callable<Void> from(final Runnable value) {
    return () -> {
      value.run();
      return null;
    };
  }

  private static final class ConstCallable<V> implements Callable<V> {

    private final V value;

    ConstCallable(final V value) {
      this.value = value;
    }

    @Override
    public V call() {
      return value;
    }

    @Override
    public String toString() {
      return "ConstCallable{" + value + '}';
    }
  }

  @Deprecated
  private static final class DefaultAdvancedRetryPredicateImpl implements AdvancedRetryPredicate<Exception> {

    @Override
    public AdvancedAction apply(@Nonnull final Exception input) {
      if (Throwables.isRetryable(input)) {
        Logger.getLogger(DefaultAdvancedRetryPredicateImpl.class.getName())
                .log(Level.FINE, "Exception encountered, retrying...", input);
        return AdvancedAction.RETRY;
      }
      return AdvancedAction.ABORT;
    }
  }



}
