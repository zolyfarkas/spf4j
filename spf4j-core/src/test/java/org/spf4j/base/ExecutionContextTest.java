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
package org.spf4j.base;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.concurrent.DefaultExecutor;

/**
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
public class ExecutionContextTest {

  @Test
  public void testExecutionContext() throws InterruptedException, ExecutionException, TimeoutException {
    ExecutionContext orig = ExecutionContexts.current();
    try (ExecutionContext ec = ExecutionContexts.start(10, TimeUnit.SECONDS)) {
      long unitsToDeadline = ExecutionContexts.current().getTimeToDeadline(TimeUnit.SECONDS);
      Assert.assertThat(unitsToDeadline, Matchers.lessThanOrEqualTo(10L));
      Assert.assertThat(unitsToDeadline, Matchers.greaterThanOrEqualTo(9L));
      Future<?> submit = DefaultExecutor.INSTANCE.submit(() -> {
        try (ExecutionContext subCtx = ExecutionContexts.start(ec)) {
          long utd = ExecutionContexts.current().getUncheckedTimeToDeadline(TimeUnit.SECONDS);
          Assert.assertThat(utd, Matchers.lessThanOrEqualTo(10L));
          Assert.assertThat(utd, Matchers.greaterThanOrEqualTo(9L));
          Assert.assertEquals(ec, subCtx.getParent());
        }
        Assert.assertNull(ExecutionContexts.current());
      });
      submit.get();
    }
    Assert.assertSame(orig, ExecutionContexts.current());
  }

  @Test
  public void testExecutionContext2() throws TimeoutException {
    try (ExecutionContext start = ExecutionContexts.start(10, TimeUnit.SECONDS)) {
      long secs = start.getTimeToDeadline(TimeUnit.SECONDS);
      Assert.assertTrue(secs >= 9);
      Assert.assertTrue(secs <= 10);
      start.put("KEY", "BAGAGE");
      Assert.assertEquals("BAGAGE", start.get("KEY", String.class));
    }


  }


  @Test
  public void testExecutionContext3() {

    try (ExecutionContext start = ExecutionContexts.start(10, TimeUnit.SECONDS)) {
      long secs = (Runtime.getDeadline() - System.currentTimeMillis()) / 1000;
      Assert.assertTrue("secs = " + secs,  secs >= 9);
      Assert.assertTrue("secs = " + secs, secs <= 10);
      start.put("KEY", "BAGAGE");
      Assert.assertEquals("BAGAGE", start.get("KEY", String.class));
    }


  }



}
