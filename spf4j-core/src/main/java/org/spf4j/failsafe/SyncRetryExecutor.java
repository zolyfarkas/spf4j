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
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.CheckReturnValue;
import org.spf4j.base.Either;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.Throwables;
import org.spf4j.base.TimeSource;
import org.spf4j.base.UncheckedExecutionException;

/**
 * @author Zoltan Farkas
 */
public interface SyncRetryExecutor<T, C extends Callable<? extends T>> {

  @CheckReturnValue
  <R extends T, W extends C, E extends Exception> R call(W pwhat, Class<E> exceptionClass,
          long startNanos, long deadlineNanos)
          throws InterruptedException, TimeoutException, E;

  @CheckReturnValue
  default <R extends T, W extends C, E extends Exception> R call(W pwhat, Class<E> exceptionClass,
          long deadlineNanos)
          throws InterruptedException, TimeoutException, E {
    return call(pwhat, exceptionClass, TimeSource.nanoTime(), deadlineNanos);
  }

  @CheckReturnValue
  default <R extends T, W extends C, E extends Exception> R call(W pwhat, Class<E> exceptionClass)
          throws InterruptedException, TimeoutException, E {
    long nanoTime = TimeSource.nanoTime();
    return call(pwhat, exceptionClass, nanoTime, ExecutionContexts.getContextDeadlineNanos());
  }

  @CheckReturnValue
  default <R extends T, W extends C, E extends Exception> R call(W pwhat, Class<E> exceptionClass,
          long timeout, TimeUnit tu)
          throws InterruptedException, TimeoutException, E {
    long nanoTime = TimeSource.nanoTime();
    return call(pwhat, exceptionClass, nanoTime,
            ExecutionContexts.computeDeadline(nanoTime, ExecutionContexts.current(), tu, timeout));
  }

  default <W extends C, E extends Exception> void run(W pwhat, Class<E> exceptionClass)
          throws InterruptedException, TimeoutException, E {
    T res = call(pwhat, exceptionClass);
    if (res != null) {
      throw new IllegalStateException("result must be null not " + res);
    }
  }

  default <W extends C, E extends Exception> void run(W pwhat, Class<E> exceptionClass, long deadlineNanos)
          throws InterruptedException, TimeoutException, E {
    T res = call(pwhat, exceptionClass, deadlineNanos);
    if (res != null) {
      throw new IllegalStateException("result must be null not " + res);
    }
  }

  default <E extends Exception, W extends C> void run(W pwhat, Class<E> exceptionClass,
          long timeout, TimeUnit tu)
          throws InterruptedException, TimeoutException, E {
     run(pwhat, exceptionClass, ExecutionContexts.computeDeadline(ExecutionContexts.current(), tu, timeout));
  }


  /**
   * Naive implementation of execution with retry logic. a callable will be executed and retry attempted in current
   * thread if the result and exception predicates. before retry, a callable can be executed that can abort the retry
   * and finish the function with the previous result.
   *
   * @param <T> - The type of callable to retry result;
   * @param <EX> - the exception thrown by the callable to retry.
   * @param pwhat - the callable to retry.
   * @return the result of the retried callable if successful.
   * @throws java.lang.InterruptedException - thrown if retry interrupted.
   * @throws EX - the exception thrown by callable.
   */
  @SuppressFBWarnings({ "MDM_THREAD_YIELD", "ITC_INHERITANCE_TYPE_CHECKING" })
  static <T, E extends Exception, C extends Callable<? extends T>> T call(
          final C pwhat,
          final RetryPredicate<T, C> retryPredicate,
          final Class<E> exceptionClass,
          final int maxExceptionChain)
          throws InterruptedException, TimeoutException, E {
    C what = pwhat;
    T result;
    Exception lastEx; // last exception
    try {
      result = what.call();
      lastEx = null;
    } catch (InterruptedException ex1) {
      throw ex1;
    } catch (Exception e) { // only EX and RuntimeException
      lastEx = e;
      result = null;
    }
    Exception lastExChain = lastEx; // last exception chained with all previous exceptions
    RetryDecision<T, C> decision;
    //CHECKSTYLE IGNORE InnerAssignment FOR NEXT 5 LINES
    while ((lastEx != null)
            ? (decision = retryPredicate.getExceptionDecision(lastEx, what)).getDecisionType()
              == RetryDecision.Type.Retry
            : (decision = retryPredicate.getDecision(result, what)).getDecisionType() == RetryDecision.Type.Retry) {
      if (Thread.interrupted()) {
        InterruptedException ex = new InterruptedException();
        if (lastExChain != null) {
          ex.addSuppressed(lastExChain);
        }
        throw ex;
      }
      long delayNanos = decision.getDelayNanos();
      if (delayNanos > 0) {
        TimeUnit.NANOSECONDS.sleep(delayNanos);
      } else if (delayNanos < 0) {
        throw new IllegalStateException("Invalid retry decision delay: " + delayNanos);
      }
      what = decision.getNewCallable();
      try {
        result = what.call();
        lastEx = null;
      } catch (InterruptedException ex1) {
        if (lastExChain != null) {
          ex1.addSuppressed(lastExChain);
        }
        throw ex1;
      } catch (Exception e) { // only EX and RuntimeException
        lastEx = e;
        result = null;
        if (lastExChain != null) {
          lastExChain = Throwables.suppress(lastEx, lastExChain, maxExceptionChain);
        } else {
          lastExChain = lastEx;
        }
      }
    }
    if (decision.getDecisionType() == RetryDecision.Type.Abort) {
        Either<Exception, T> r = decision.getResult();
        if (r != null) {
          if (r.isLeft()) {
            lastEx = r.getLeft();
            if (lastExChain != null) {
              lastExChain = Throwables.suppress(lastEx, lastExChain, maxExceptionChain);
            } else {
              lastExChain = lastEx;
            }
          } else {
            result = r.getRight();
            lastEx = null;
          }
        }
    } else {
      throw new IllegalStateException("Should not happen, decision =  " + decision);
    }
    if (lastEx != null) {
      if (lastExChain == null) {
        lastExChain = lastEx;
      }
      if (lastExChain instanceof RuntimeException) {
        throw (RuntimeException) lastExChain;
      } else if (lastExChain instanceof TimeoutException) {
        throw (TimeoutException) lastExChain;
      } else if (exceptionClass.isAssignableFrom(lastExChain.getClass())) {
        throw (E) lastExChain;
      } else {
        throw new UncheckedExecutionException(lastExChain);
      }
    }
    return result;
  }


}
