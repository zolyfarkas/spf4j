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

import com.google.common.util.concurrent.UncheckedExecutionException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.spf4j.base.Throwables;

/**
 *
 * @author Zoltan Farkas
 */
public final class Retry {


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
   */
  @SuppressFBWarnings({ "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", "MDM_THREAD_YIELD" })
  public static <T, EX extends Exception, C extends Callable<T>> T execute(
          final C pwhat,
          final RetryPredicate<T, C> retryOnReturnVal,
          final RetryPredicate<Exception, C> retryOnException,
          final Class<EX> exceptionClass,
          final int maxExceptionChain)
          throws InterruptedException, TimeoutException, EX {
    C what = pwhat;
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
    RetryDecision<C> decision = null;
    //CHECKSTYLE IGNORE InnerAssignment FOR NEXT 5 LINES
    while ((lastEx != null
            && (decision = retryOnException.getDecision(lastEx, what)).getDecisionType()
                == RetryDecision.Type.Retry)
            || (lastEx == null && (decision = retryOnReturnVal.getDecision(result, what)).getDecisionType()
                == RetryDecision.Type.Retry)) {
      if (Thread.interrupted()) {
        Thread.currentThread().interrupt();
        throw new InterruptedException();
      }
      long delayNanos = decision.getDelayNanos();
      if (delayNanos > 0) {
        TimeUnit.NANOSECONDS.sleep(delayNanos);
      } else if (delayNanos < 0) {
        throw new IllegalStateException("Invalid retry decision delay: " + delayNanos);
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
          lastExChain = Throwables.suppress(lastEx, lastExChain, maxExceptionChain);
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
            lastExChain = Throwables.suppress(lastEx, lastExChain, maxExceptionChain);
          } else {
            lastExChain = lastEx;
          }
        } else {
          Optional<T> newres = (Optional<T>) decision.getResult();
          if (newres.isPresent()) {
            result = newres.get();
            lastEx = null;
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


}
