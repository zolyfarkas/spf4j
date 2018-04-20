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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * test to validate the behavior between spf4j and FJP implementations...
 * @author zoly
 */
public class ThreadPoolExecutorTest {

  private static final Logger LOG = LoggerFactory.getLogger(ThreadPoolExecutorTest.class);

  @Test(timeout = 10000)
  public void testInteruptionBehavior() throws InterruptedException, ExecutionException {
    LifoThreadPoolExecutorSQP executor = new LifoThreadPoolExecutorSQP("test", 0, 16, 60000, 0);
    testPoolTaskCancellation(executor);
  }

  @Test(timeout = 10000)
  public void testInteruptionBehaviorFJP() throws InterruptedException, ExecutionException {
    boolean assertError = false;
    try {
      testPoolTaskCancellation(new ForkJoinPool(16));
    } catch (AssertionError err) {
      LOG.debug("Expected error", err);
      assertError = true;
    }
    Assert.assertTrue("expected that FJP tasks cannot be interrupted", assertError);
  }

  public static void testPoolTaskCancellation(final ExecutorService executor)
          throws InterruptedException, ExecutionException {
    RunnableImpl testRunnable = new RunnableImpl();
    Future<?> submit = executor.submit(testRunnable);
    Assert.assertTrue("task did not start", testRunnable.getStartedlatch().await(5, TimeUnit.SECONDS));
    submit.cancel(true);
    try {
      submit.get();
      Assert.fail("expected CancellationException");
    } catch (CancellationException ex) {
      // expected
    }
    Assert.assertTrue("task was not interrupted", testRunnable.getInterruptedLatch().await(5, TimeUnit.SECONDS));
    executor.shutdown();
    Assert.assertTrue("executor was not shut down", executor.awaitTermination(1000, TimeUnit.MILLISECONDS));
  }

  static class RunnableImpl implements Runnable {

    private CountDownLatch startedlatch = new CountDownLatch(1);

    private CountDownLatch interruptedLatch = new CountDownLatch(1);

    @Override
    @SuppressFBWarnings("MDM_THREAD_YIELD")
    public void run() {
      startedlatch.countDown();
      try {
        Thread.sleep(100000);
      } catch (InterruptedException ex) {
        interruptedLatch.countDown();
      }
    }

    public CountDownLatch getInterruptedLatch() {
      return interruptedLatch;
    }

    public CountDownLatch getStartedlatch() {
      return startedlatch;
    }

  }

}
