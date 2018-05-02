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
package org.spf4j.recyclable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.HandlerNano;
import org.spf4j.base.Throwables;
import org.spf4j.base.UncheckedExecutionException;
import org.spf4j.failsafe.RetryPolicy;

//CHECKSTYLE IGNORE RedundantThrows FOR NEXT 2000 LINES
public final class Template<T, R, E extends Exception> {

  private final RecyclingSupplier<T> pool;
  private final Class<E> exClass;
  private final RetryPolicy<R, Callable<? extends R>> retryPolicy;

  public Template(final RecyclingSupplier<T> pool, final RetryPolicy<R, Callable<? extends R>> retryPolicy,
          final Class<E> exClass) {
    this.pool = pool;
    this.exClass = exClass;
    this.retryPolicy = retryPolicy;
  }

  public R doOnSupplied(final HandlerNano<T, R, E> handler, final long timeout, final TimeUnit tu)
          throws InterruptedException, E, TimeoutException {
    return doOnSupplied(handler, timeout, tu, pool, retryPolicy, exClass);
  }

  public static <T, R, E extends Exception> R doOnSupplied(final HandlerNano<T, R, E> handler,
          final long timeout, final TimeUnit tu,
          final RecyclingSupplier<T> pool, final RetryPolicy<R, Callable<? extends R>> retryPolicy,
          final Class<E> exClass)
          throws E, InterruptedException, TimeoutException {
    try (ExecutionContext ctx = ExecutionContexts.start(handler.toString(), timeout, tu)) {
      return retryPolicy.call(new Callable<R>() {
        @Override
        public R call()
                throws InterruptedException, TimeoutException, E {
          try {
            return Template.doOnSupplied(handler, pool, ctx.getDeadlineNanos(), exClass);
          } catch (ObjectCreationException | ObjectBorrowException ex) {
            throw new UncheckedExecutionException(ex);
          }
        }
      }, exClass, ctx.getDeadlineNanos());
    }
  }

  //findbugs does not know about supress in spf4j
  @SuppressFBWarnings("LEST_LOST_EXCEPTION_STACK_TRACE")
  private static <T, R, E extends Exception> R doOnSupplied(final HandlerNano<T, R, E> handler,
          final RecyclingSupplier<T> pool, final long deadlineNanos, final Class<E> exClass)
          throws E, ObjectCreationException, ObjectBorrowException, InterruptedException, TimeoutException {
    T object = pool.get();
    R result;
    try {
      result = handler.handle(object, deadlineNanos);
    } catch (RuntimeException e) {
      try {
        pool.recycle(object, e);
      } catch (RuntimeException ex) {
        throw Throwables.suppress(ex, e);
      }
      throw e;
    } catch (Exception e) {
      try {
        pool.recycle(object, e);
      } catch (RuntimeException ex) {
        throw Throwables.suppress(ex, e);
      }
      if (exClass.isAssignableFrom(e.getClass())) {
        throw (E) e;
      } else {
        throw new UncheckedExecutionException(e);
      }
    }
    pool.recycle(object, null);
    return result;
  }

  @Override
  public String toString() {
    return "Template{" + "pool=" + pool + ", exClass=" + exClass + ", retryPolicy=" + retryPolicy + '}';
  }

}
