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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.CheckReturnValue;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.TimeSource;

/**
 * @author Zoltan Farkas
 */
public interface PolicyExecutor<T, C extends Callable<? extends T>> {

  @CheckReturnValue
  <R extends T, W extends C, EX extends Exception> R call(W pwhat, Class<EX> exceptionClass,
          long startNanos, long deadlineNanos)
          throws InterruptedException, TimeoutException, EX;

  @CheckReturnValue
  default <R extends T, W extends C, EX extends Exception> R call(W pwhat, Class<EX> exceptionClass,
          long deadlineNanos)
          throws InterruptedException, TimeoutException, EX {
    return call(pwhat, exceptionClass, TimeSource.nanoTime(), deadlineNanos);
  }

  @CheckReturnValue
  default <R extends T, W extends C, EX extends Exception> R call(W pwhat, Class<EX> exceptionClass)
          throws InterruptedException, TimeoutException, EX {
    long nanoTime = TimeSource.nanoTime();
    return call(pwhat, exceptionClass, nanoTime, ExecutionContexts.getContextDeadlineNanos());
  }

  @CheckReturnValue
  default <R extends T, W extends C, EX extends Exception> R call(W pwhat, Class<EX> exceptionClass,
          long timeout, TimeUnit tu)
          throws InterruptedException, TimeoutException, EX {
    long nanoTime = TimeSource.nanoTime();
    return call(pwhat, exceptionClass, nanoTime,
            ExecutionContexts.computeDeadline(nanoTime, ExecutionContexts.current(), tu, timeout));
  }

  default <W extends C, EX extends Exception> void run(W pwhat, Class<EX> exceptionClass)
          throws InterruptedException, TimeoutException, EX {
    T res = call(pwhat, exceptionClass);
    if (res != null) {
      throw new IllegalStateException("result must be null not " + res);
    }
  }

  default <W extends C, EX extends Exception> void run(W pwhat, Class<EX> exceptionClass, long deadlineNanos)
          throws InterruptedException, TimeoutException, EX {
    T res = call(pwhat, exceptionClass, deadlineNanos);
    if (res != null) {
      throw new IllegalStateException("result must be null not " + res);
    }
  }

  default <W extends C, EX extends Exception> void run(W pwhat, Class<EX> exceptionClass,
          long timeout, TimeUnit tu)
          throws InterruptedException, TimeoutException, EX {
     run(pwhat, exceptionClass, ExecutionContexts.computeDeadline(ExecutionContexts.current(), tu, timeout));
  }


}
