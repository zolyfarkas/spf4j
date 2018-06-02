/*
 * Copyright 2018 SPF4J.
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
package org.spf4j.base.intv;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Zoltan Farkas
 */
public class RateLimiterTest {

  private static final Logger LOG = LoggerFactory.getLogger(RateLimiterTest.class);


  @Test(expected = RejectedExecutionException.class)
  public void testRateLimiter() throws Exception {
    RateLimiter resourceLimiter = new RateLimiter.Source(10, 1, TimeUnit.SECONDS).getResourceLimiter("testRes");
    for (int i = 0; i < 11; i++) {
      int v = i;
      resourceLimiter.execute(() -> {
        LOG.debug("Executing {}, state = {}", v, resourceLimiter);
        return null;
      });
    }
  }

  @Test
  public void testRateLimiter2() throws Exception {
    RateLimiter resourceLimiter = new RateLimiter.Source(10, 1, TimeUnit.SECONDS).getResourceLimiter("testRes");
    long time = System.nanoTime();
    for (int i = 0; i < 10; i++) {
      int v = i;
      resourceLimiter.execute(() -> {
        LOG.debug("Executing {}, state = {}", v, resourceLimiter);
        return null;
      });
    }
    long wait = time + TimeUnit.SECONDS.toNanos(1) - System.nanoTime();
    TimeUnit.NANOSECONDS.sleep(wait);
    resourceLimiter.execute(() -> {
        LOG.debug("Executing {}, state = {}", 11, resourceLimiter);
        return null;
      });

  }

}
