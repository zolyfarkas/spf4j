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
package org.spf4j.failsafe.concurrent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.TimeSource;
import org.spf4j.concurrent.DefaultExecutor;
import org.spf4j.failsafe.RetryPolicy;
import org.spf4j.failsafe.RetryPredicate;
import org.spf4j.log.Level;
import org.spf4j.test.log.annotations.ExpectLog;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("MDM_THREAD_YIELD")
public class RetryExecutorTest {
  private static final Logger LOG = LoggerFactory.getLogger(RetryExecutorTest.class);

  @Test
  @ExpectLog(category = "org.spf4j.failsafe.concurrent.RetryExecutorTest", level = Level.DEBUG,
          nrTimes = 3, messageRegexp = "Executing .*")
  public void testHedgedretryExecution() throws InterruptedException, ExecutionException {
    try (FailSafeExecutorImpl exec = new FailSafeExecutorImpl(DefaultExecutor.INSTANCE)) {
      Future fut = exec.submit(() -> {
        LOG.debug("Executing {}", this);
        return Thread.currentThread().getName();
      }, RetryPredicate.NORETRY, 2, 0, TimeUnit.MILLISECONDS);
      Assert.assertNotNull(fut.get());
    }
  }

  @Test
  @ExpectLog(category = "org.spf4j.failsafe.concurrent.RetryExecutorTest", level = Level.DEBUG,
          nrTimes = 1, messageRegexp = "Executing .*")
  public void testHedgedretryExecution2() throws InterruptedException, ExecutionException {
    try (FailSafeExecutorImpl exec = new FailSafeExecutorImpl(DefaultExecutor.INSTANCE)) {
      Future fut = exec.submit(() -> {
        LOG.debug("Executing {}", this);
        return Thread.currentThread().getName();
      }, RetryPredicate.NORETRY, 2, 1000, TimeUnit.MILLISECONDS);
      Assert.assertNotNull(fut.get());
    }
  }

  @Test(expected = ExecutionException.class)
  public void testHedgedretryExecution3() throws InterruptedException, ExecutionException {
    try (FailSafeExecutorImpl exec = new FailSafeExecutorImpl(DefaultExecutor.INSTANCE)) {
      Future fut = exec.submit(() -> {
        throw new RuntimeException();
      }, RetryPredicate.NORETRY, 2, 10, TimeUnit.MILLISECONDS);
      Assert.assertNotNull(fut.get());
    }
  }

  @Test(timeout = 1000)
  public void testHedgedretryExecution4() throws InterruptedException, ExecutionException {
    AtomicInteger ai = new AtomicInteger();
    try (FailSafeExecutorImpl exec = new FailSafeExecutorImpl(DefaultExecutor.INSTANCE)) {
      long nanoTime = TimeSource.nanoTime();
      Future fut = exec.submit(() -> {
        if (ai.getAndIncrement() == 0) {
          Thread.sleep(1000000);
          return "B";
        }
        LOG.debug("Executing {}", this);
        return "A";
      }, RetryPolicy.defaultPolicy().getRetryPredicate(nanoTime, nanoTime + 1000000000), 2, 10, TimeUnit.MILLISECONDS);
      Assert.assertEquals("A", fut.get());
    }
  }


}
