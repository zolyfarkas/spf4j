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
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.TestTimeSource;
import org.spf4j.base.TimeSource;
import org.spf4j.log.Level;
import org.spf4j.test.log.annotations.PrintLogs;

/**
 * @author Zoltan Farkas
 */
public class RateLimiterTest {

  private static final Logger LOG = LoggerFactory.getLogger(RateLimiterTest.class);

  @Test(expected = IllegalArgumentException.class)
  public void testRateLimitInvalid() {
    new RateLimiter(1000, Duration.ofSeconds(1), 9);
  }

  @Test
  public void testRateLimitInvalidTryReservation() throws InterruptedException {
    long nanoTime = TestTimeSource.freezeTime();
    try {
    ScheduledExecutorService mockExec = Mockito.mock(ScheduledExecutorService.class);
    ScheduledFuture mockFut = Mockito.mock(ScheduledFuture.class);
    Mockito.when(mockExec.scheduleAtFixedRate(Mockito.any(), Mockito.anyLong(), Mockito.anyLong(),
            Mockito.eq(TimeUnit.NANOSECONDS))).thenReturn(mockFut);
    try (RateLimiter rateLimiter = new RateLimiter(10, Duration.ofNanos(10000000), 100, mockExec)) {
      long s1Nanos = TimeUnit.SECONDS.toNanos(1);
      long timeToWait = rateLimiter.tryAcquireGetDelayNanos(1000, nanoTime + s1Nanos);
      Assert.assertEquals(s1Nanos, timeToWait);
    }
    } finally {
      TestTimeSource.clear();
    }
  }


  @Test
  @PrintLogs(ideMinLevel = Level.TRACE)
  public void testRateLimitArgs() {
    try (RateLimiter rateLimiter = new RateLimiter(17, Duration.ofSeconds(1), 20, 10, TimeUnit.MILLISECONDS)) {
      LOG.debug("Rate Limiter = {}", rateLimiter);
      Assert.assertEquals(17, rateLimiter.getPermitsPerReplenishInterval());
      Assert.assertEquals(1000000000, rateLimiter.getPermitReplenishIntervalNanos());
    }
  }

  @Test
  public void testRateLimitTryAcquisition() throws InterruptedException {
    try (RateLimiter rateLimiter = new RateLimiter(10, Duration.ofSeconds(1), 10)) {
      LOG.debug("Rate Limiter = {}", rateLimiter);
      Assert.assertFalse(rateLimiter.tryAcquire(20, 0, TimeUnit.MILLISECONDS));
      long startTime = TimeSource.nanoTime();
      boolean tryAcquire = rateLimiter.tryAcquire(20, 2, TimeUnit.SECONDS);
      LOG.debug("waited {} ns for {}", (TimeSource.nanoTime() - startTime), rateLimiter);
      Assert.assertTrue(tryAcquire);
      Assert.assertFalse(rateLimiter.tryAcquire(20, 1, TimeUnit.SECONDS));
    }
  }


  @Test
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public void testRateLimitTryAcquisition2() throws InterruptedException {
    ScheduledExecutorService mockExec = Mockito.mock(ScheduledExecutorService.class);
    ScheduledFuture mockFut = Mockito.mock(ScheduledFuture.class);
    Mockito.when(mockExec.scheduleAtFixedRate(Mockito.any(), Mockito.eq(100000000L), Mockito.eq(100000000L),
            Mockito.eq(TimeUnit.NANOSECONDS))).thenReturn(mockFut);
    try (RateLimiter rateLimiter = new RateLimiter(1, Duration.ofNanos(100000000L), 10, mockExec, () -> 0L)) {
      long tryAcquireGetDelayNs = rateLimiter.tryAcquireGetDelayNanos(10, TimeUnit.SECONDS.toNanos(10));
      LOG.debug("Rate Limiter = {}, waitMs = {}", rateLimiter, tryAcquireGetDelayNs);
      Assert.assertEquals(1000000000, tryAcquireGetDelayNs);
      Assert.assertEquals(-10, rateLimiter.getNrPermits(), 0.0001);
      tryAcquireGetDelayNs = rateLimiter.tryAcquireGetDelayNanos(10, TimeUnit.MILLISECONDS.toNanos(10));
      Assert.assertTrue(tryAcquireGetDelayNs < 0);
      tryAcquireGetDelayNs = rateLimiter.tryAcquireGetDelayNanos(1, TimeUnit.MILLISECONDS.toNanos(2000));
      Assert.assertEquals(1100000000, tryAcquireGetDelayNs);
    }
    Mockito.verify(mockExec).scheduleAtFixedRate(Mockito.any(), Mockito.eq(100000000L), Mockito.eq(100000000L),
            Mockito.eq(TimeUnit.NANOSECONDS));
    Mockito.verify(mockFut).cancel(false);
  }


  @Test
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public void testRateLimitTryAcquisition3() throws InterruptedException {
    ScheduledExecutorService mockExec = Mockito.mock(ScheduledExecutorService.class);
    ScheduledFuture mockFut = Mockito.mock(ScheduledFuture.class);
    Mockito.when(mockExec.scheduleAtFixedRate(Mockito.any(), Mockito.eq(10000000000L), Mockito.eq(10000000000L),
            Mockito.eq(TimeUnit.NANOSECONDS))).thenReturn(mockFut);
    try (RateLimiter rateLimiter = new RateLimiter(100, Duration.ofSeconds(10), 100, mockExec, () -> 0L)) {
      long tryAcquireGetDelayNs = rateLimiter.tryAcquireGetDelayNanos(2, TimeUnit.SECONDS.toNanos(20));
      LOG.debug("Rate Limiter = {}, waitMs = {}", rateLimiter, tryAcquireGetDelayNs);
      Assert.assertEquals(10000000000L, tryAcquireGetDelayNs);
      Assert.assertEquals(-2, rateLimiter.getNrPermits());
      tryAcquireGetDelayNs = rateLimiter.tryAcquireGetDelayNanos(1, TimeUnit.MILLISECONDS.toNanos(10));
      Assert.assertTrue(tryAcquireGetDelayNs < 0);
    }
    Mockito.verify(mockExec).scheduleAtFixedRate(Mockito.any(), Mockito.eq(10000000000L), Mockito.eq(10000000000L),
            Mockito.eq(TimeUnit.NANOSECONDS));
    Mockito.verify(mockFut).cancel(false);
  }


}
