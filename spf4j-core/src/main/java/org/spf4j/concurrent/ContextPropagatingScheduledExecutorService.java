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
package org.spf4j.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.spf4j.base.ExecutionContexts;

/**
 * A Execution context propagating Scheduled executor.
 * @author Zoltan Farkas
 */
public final class ContextPropagatingScheduledExecutorService
        extends ContextPropagatingExecutorService
        implements ScheduledExecutorService {

  private final ScheduledExecutorService ses;

  public ContextPropagatingScheduledExecutorService(final ScheduledExecutorService sex) {
    super(sex);
    this.ses = sex;
  }

  @Override
  public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit) {
    return ses.schedule(ExecutionContexts.propagatingRunnable(command), delay, unit);
  }

  @Override
  public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay, final TimeUnit unit) {
    return ses.schedule(ExecutionContexts.propagatingCallable(callable), delay, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay,
          final long period, final TimeUnit unit) {
    return ses.scheduleAtFixedRate(ExecutionContexts.propagatingRunnable(command), initialDelay, period, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay,
          final long delay, final TimeUnit unit) {
    return ses.scheduleWithFixedDelay(ExecutionContexts.propagatingRunnable(command), initialDelay, delay, unit);
  }

  @Override
  public ExecutorService wrap(final ExecutorService wrapped) {
    if (wrapped instanceof ScheduledExecutorService) {
      return new ContextPropagatingScheduledExecutorService((ScheduledExecutorService) wrapped);
    } else {
      return super.wrap(wrapped);
    }
  }

  @Override
  public ScheduledExecutorService getWrapped() {
    return ses;
  }


}
