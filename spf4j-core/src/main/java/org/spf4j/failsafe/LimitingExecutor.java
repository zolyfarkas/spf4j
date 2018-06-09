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

import com.google.common.annotations.Beta;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.spf4j.base.Callables;
import org.spf4j.base.UncheckedExecutionException;
import org.spf4j.concurrent.PermitSupplier;
import org.spf4j.concurrent.Semaphore;

/**
 * @author Zoltan Farkas
 */
@Beta
public final class LimitingExecutor<T, C extends Callable<? extends T>> implements Executor {

  private final RejectedExecutionHandler rejectHandler;

  private final Semaphore semaphore;

  @FunctionalInterface
  public interface RejectedExecutionHandler<T, C extends Callable<? extends T>> {

    T reject(LimitingExecutor<T, C> limiter, C callable) throws Exception;
  }

  public LimitingExecutor(final PermitSupplier permitSupplier) {
    this(permitSupplier.toSemaphore());
  }

  public LimitingExecutor(final Semaphore semaphore) {
    this(new RejectedExecutionHandler<T, C>() {
      @Override
      public T reject(final LimitingExecutor<T, C> limiter, final C callable) {
        throw new RejectedExecutionException("No buckets available for " + callable + " in limiter " + limiter);
      }
    }, semaphore);
  }

  public LimitingExecutor(final RejectedExecutionHandler<T, C> rejectHandler, final Semaphore semaphore) {
    this.rejectHandler = rejectHandler;
    this.semaphore = semaphore;
  }

  @Override
  public void execute(final Runnable command) {
    try {
      execute((C) Callables.from(command));
    } catch (Exception ex) {
      throw new UncheckedExecutionException(ex);
    }
  }

  public <T> T execute(final C callable) throws Exception {
    if (semaphore.tryAcquire(0, TimeUnit.NANOSECONDS)) {
      try {
        return (T) callable.call();
      } finally {
        semaphore.release();
      }
    } else {
      return (T) rejectHandler.reject(this, callable);
    }
  }

  public Callable<T> toLimitedCallable(final C callable) {
    return () -> this.execute(callable);
  }

  public RejectedExecutionHandler getRejectHandler() {
    return rejectHandler;
  }

  public Semaphore getSemaphore() {
    return semaphore;
  }

  @Override
  public String toString() {
    return "LimitedExecutor{" + "rejectHandler=" + rejectHandler + ", semaphore=" + semaphore + '}';
  }

}
