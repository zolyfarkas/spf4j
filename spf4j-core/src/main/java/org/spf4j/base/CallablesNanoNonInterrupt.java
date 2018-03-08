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

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.base.Callables.AdvancedRetryPredicate;
import static org.spf4j.base.Callables.DEFAULT_EXCEPTION_RETRY;
import org.spf4j.base.Callables.FibonacciBackoffRetryPredicate;
import org.spf4j.base.Callables.TimeoutRetryPredicate2RetryPredicate;
import org.spf4j.base.CallablesNano.NanoTimeoutCallable;
import org.spf4j.base.Callables.TimeoutRetryPredicate;
import org.spf4j.base.CallablesNano.TimeoutNanoRetryPredicate;

/**
 * Utility class for executing stuff with retry logic.
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
@Beta
@Deprecated
@SuppressFBWarnings("AI_ANNOTATION_ISSUES_NEEDS_NULLABLE")
//CHECKSTYLE IGNORE RedundantThrows FOR NEXT 2000 LINES
public final class CallablesNanoNonInterrupt {

    private CallablesNanoNonInterrupt() {
    }


    public static <T, EX extends Exception> T executeWithRetry(final NanoTimeoutCallable<T, EX> what,
            final int nrImmediateRetries,
            final long maxRetryWaitNanos, final Class<EX> exceptionClass)
            throws EX {
        return executeWithRetry(what, nrImmediateRetries, maxRetryWaitNanos,
                (TimeoutNanoRetryPredicate<? super T, T>) TimeoutNanoRetryPredicate.NORETRY_FOR_RESULT,
                DEFAULT_EXCEPTION_RETRY, exceptionClass);
    }

    public static <T, EX extends Exception> T executeWithRetry(final NanoTimeoutCallable<T, EX> what,
            final int nrImmediateRetries,
            final long maxRetryWaitNanos,
            final AdvancedRetryPredicate<Exception> retryOnException,
            final Class<EX> exceptionClass)
            throws EX {
        return executeWithRetry(what, nrImmediateRetries, maxRetryWaitNanos,
                (TimeoutNanoRetryPredicate<? super T, T>) TimeoutNanoRetryPredicate.NORETRY_FOR_RESULT,
                retryOnException, exceptionClass);
    }

    /**
     * After the immediate retries are done,
     * delayed retry with randomized Fibonacci values up to the specified max is executed.
     * @param <T> - the type returned by the Callable that is retried.
     * @param <EX> - the Exception thrown by the retried callable.
     * @param what - the callable to retry.
     * @param nrImmediateRetries - the number of immediate retries.
     * @param maxWaitNanos - maximum wait time in between retries.
     * @param retryOnReturnVal - predicate to control retry on return value;
     * @param retryOnException - predicate to retry on thrown exception.
     * @return the result of the callable.
     * @throws EX - the exception declared to be thrown by the callable.
     */
    public static <T, EX extends Exception> T executeWithRetry(final NanoTimeoutCallable<T, EX> what,
            final int nrImmediateRetries, final long maxWaitNanos,
            final TimeoutNanoRetryPredicate<? super T, T> retryOnReturnVal,
            final AdvancedRetryPredicate<Exception> retryOnException,
            final Class<EX> exceptionClass)
            throws EX {
      final long deadline = what.getDeadline();
      try {
        return Callables.executeWithRetry(what, new TimeoutRetryPredicate2RetryPredicate<>(deadline, retryOnReturnVal),
                new FibonacciBackoffRetryPredicate<>(retryOnException, nrImmediateRetries,
                        maxWaitNanos / 100, maxWaitNanos, Callables::rootClass, deadline,
                        () -> TimeSource.nanoTime(), TimeUnit.NANOSECONDS), exceptionClass);
      } catch (InterruptedException ex) {
        throw new UncheckedExecutionException(ex);
      } catch (TimeoutException ex) {
        throw new UncheckedTimeoutException(ex);
      }
    }

    public static <T, EX extends Exception> T executeWithRetry(final NanoTimeoutCallable<T, EX> what,
            final TimeoutNanoRetryPredicate<T, T> retryOnReturnVal,
            final TimeoutNanoRetryPredicate<Exception, T> retryOnException,
            final Class<EX> exceptionClass)
            throws EX {
      try {
        return Callables.executeWithRetry(what, retryOnReturnVal, retryOnException, exceptionClass);
      } catch (InterruptedException ex) {
        throw new UncheckedExecutionException(ex);
      } catch (TimeoutException ex) {
        throw new UncheckedTimeoutException(ex);
      }
    }

   public static <T, EX extends Exception> T executeWithRetry(final NanoTimeoutCallable<T, EX> what,
            final TimeoutNanoRetryPredicate<Exception, T> retryOnException, final Class<EX> exceptionClass)
            throws EX {
      try {
        return Callables.executeWithRetry(what,
                (TimeoutRetryPredicate<T, T>) TimeoutRetryPredicate.NORETRY_FOR_RESULT,
                retryOnException, exceptionClass);
      } catch (InterruptedException ex) {
        throw new UncheckedExecutionException(ex);
      } catch (TimeoutException ex) {
        throw new UncheckedTimeoutException(ex);
      }
    }



}
