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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import org.junit.Assert;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.junit.Test;
import org.spf4j.failsafe.AsyncRetryExecutor;
import org.spf4j.failsafe.RetryPolicy;
import org.spf4j.failsafe.concurrent.RetryExecutor;
import org.spf4j.recyclable.impl.ExpensiveTestObject;
import org.spf4j.recyclable.impl.ExpensiveTestObjectFactory;
import org.spf4j.recyclable.impl.RecyclingSupplierBuilder;
import org.spf4j.recyclable.impl.TestCallable;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
public final class ObjectPoolVsApache {

  private static final int TEST_TASKS = 1000000;

  @Test(timeout = 200000)
  @SuppressFBWarnings("HES_LOCAL_EXECUTOR_SERVICE")
  public void testPerformance() throws ObjectCreationException, InterruptedException, ExecutionException {
    final RecyclingSupplier<ExpensiveTestObject> pool
            = new RecyclingSupplierBuilder(10, new ExpensiveTestObjectFactory(1000, 100, 0, 1)).build();
    final GenericObjectPool apool
            = new GenericObjectPool(new ExpensiveTestObjectFactoryApache(1000, 10, 0, 1), 10);
    ExecutorService execService = Executors.newFixedThreadPool(10);
    BlockingQueue<Future<?>> completionQueue = new LinkedBlockingDeque<>();
    RetryExecutor exec = new RetryExecutor(execService, completionQueue);
    AsyncRetryExecutor<Object, Callable<? extends Object>> policy = RetryPolicy.newBuilder()
            .withDefaultThrowableRetryPredicate().buildAsync(exec);
    long zpooltime = testPool(policy, pool, completionQueue);
    long apooltime = testPoolApache(policy, apool, completionQueue);
    Assert.assertTrue("apache pool must be slower", apooltime > zpooltime);
    exec.close();
  }

  private long testPool(final AsyncRetryExecutor exec, final RecyclingSupplier<ExpensiveTestObject> pool,
          final BlockingQueue<Future<?>> completionQueue) throws InterruptedException, ExecutionException {
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < TEST_TASKS; i++) {
      exec.execute(new TestCallable(pool, i));
    }
    for (int i = 0; i < TEST_TASKS; i++) {
      completionQueue.take().get();
    }
    return System.currentTimeMillis() - startTime;
  }

  private long testPoolApache(final AsyncRetryExecutor exec,
          final GenericObjectPool pool,
          final BlockingQueue<Future<?>> completionQueue) throws InterruptedException, ExecutionException {
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < TEST_TASKS; i++) {
      exec.execute(new TestCallableApache(pool, i));
    }
    for (int i = 0; i < TEST_TASKS; i++) {
      completionQueue.take().get();
    }
    return System.currentTimeMillis() - startTime;
  }

}
