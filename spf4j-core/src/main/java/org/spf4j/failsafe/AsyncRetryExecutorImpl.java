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
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.spf4j.failsafe.concurrent.RetryExecutor;

/**
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY")
final class AsyncRetryExecutorImpl<T, C extends Callable<? extends T>>
        implements AsyncRetryExecutor<T, C> {

  private final RetryPolicy<T, C> retryPolicy;

  private final RetryExecutor executor;

  AsyncRetryExecutorImpl(final RetryPolicy<T, C> retryPolicy,
          final RetryExecutor executor) {
    this.executor = executor;
    this.retryPolicy = retryPolicy;
  }


  @Override
  public <R extends T, W extends C> Future<R> submit(final W pwhat,
          final long startTimeNanos, final long deadlineNanos) {
    return (Future<R>) executor.submit(pwhat, retryPolicy.getRetryPredicate(startTimeNanos, deadlineNanos));
  }

  public <W extends C> void execute(final W pwhat, final long startTimeNanos, final long deadlineNanos) {
    executor.execute(pwhat, retryPolicy.getRetryPredicate(startTimeNanos, deadlineNanos));
  }

  @Override
  public String toString() {
    return "AsyncRetryPolicy{" + "executor=" + executor + '}';
  }

  @Override
  public <R extends T, W extends C, EX extends Exception> R call(final W pwhat, final Class<EX> exceptionClass,
          final long startNanos, final long deadlineNanos) throws InterruptedException, TimeoutException, EX {
    return retryPolicy.call(pwhat, exceptionClass, startNanos, deadlineNanos);
  }



}
