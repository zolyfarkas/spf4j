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

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.annotation.CheckReturnValue;
import javax.annotation.concurrent.ThreadSafe;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.TimeSource;

/**
 *
 * @author Zoltan Farkas
 */
@ThreadSafe
public interface AsyncRetryExecutor<T, C extends Callable<? extends T>> extends SyncRetryExecutor<T, C> {

  @CheckReturnValue
  default <R extends T, W extends C> Future<R> submit(W pwhat) {
    long nanoTime = TimeSource.nanoTime();
    return submit(pwhat, nanoTime, ExecutionContexts.getContextDeadlineNanos(nanoTime));
  }

  @CheckReturnValue
  default <R extends T, W extends C> Future<R> submit(W pwhat, long deadlineNanos) {
    return submit(pwhat, TimeSource.nanoTime(), deadlineNanos);
  }

  @CheckReturnValue
  <R extends T, W extends C> Future<R> submit(W pwhat, long startTimeNanos, long deadlineNanos);

  @CheckReturnValue
  default <R extends T, W extends C> Future<R> submit(W pwhat, long timeout, TimeUnit tu) {
    long nanoTime = TimeSource.nanoTime();
    return submit(pwhat, nanoTime, ExecutionContexts.computeDeadline(nanoTime,
            ExecutionContexts.current(), tu, timeout));
  }

  default <W extends C> void execute(W pwhat) {
    long nanoTime = TimeSource.nanoTime();
    execute(pwhat, nanoTime, ExecutionContexts.getContextDeadlineNanos(nanoTime));
  }

  <W extends C> void execute(W pwhat, long startTimeNanos, long deadlineNanos);

  default <W extends C> void execute(W pwhat, long timeout, TimeUnit tu) {
    long nanoTime = TimeSource.nanoTime();
    execute(pwhat, nanoTime, ExecutionContexts.computeDeadline(nanoTime, ExecutionContexts.current(), tu, timeout));
  }


  static <T, C extends Callable<? extends T>> AsyncRetryExecutor<T, C> defaultAsyncRetryExecutor() {
    return (AsyncRetryExecutor<T, C>) DefaultAsyncExecutor.DEFAULT;
  }

  static <T, C extends Callable<? extends T>> AsyncRetryExecutor<T, C> noRetryAsyncExecutor() {
    return (AsyncRetryExecutor<T, C>) DefaultAsyncExecutor.NO_RETRY;
  }


}
