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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.ExecutionContext.Tag;
import org.spf4j.concurrent.ContextPropagatingCompletableFuture;
import org.spf4j.concurrent.DefaultExecutor;
import org.spf4j.log.Level;
import org.spf4j.log.Slf4jLogRecord;
import org.spf4j.log.Slf4jLogRecordImpl;

/**
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
public class ExecutionContextTest {

  private static final Logger LOG = LoggerFactory.getLogger(ExecutionContextTest.class);

  private static final ExecutionContext.Tag<String, Void> KEY_TAG = new ExecutionContext.Tag<String, Void>() {
    @Override
    public String toString() {
      return "KEY";
    }
  };

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
          Assert.assertEquals(ec, subCtx.getSource());
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
      start.put(KEY_TAG, "BAGAGE");
      Assert.assertEquals("BAGAGE", start.get(KEY_TAG));
    }


  }


  @Test
  public void testExecutionContext3() throws TimeoutException {

    try (ExecutionContext start = ExecutionContexts.start(10, TimeUnit.SECONDS)) {
      long secs = start.getSecondsToDeadline();
      Assert.assertTrue("secs = " + secs,  secs >= 9);
      Assert.assertTrue("secs = " + secs, secs <= 10);
      start.put(KEY_TAG, "BAGAGE");
      Assert.assertEquals("BAGAGE", start.get(KEY_TAG));
    }


  }


  @Test
  public void testExecutionPropagetionOfLogs() throws TimeoutException, InterruptedException, ExecutionException {

    try (ExecutionContext start = ExecutionContexts.start(10, TimeUnit.SECONDS)) {
      Slf4jLogRecordImpl log = new Slf4jLogRecordImpl("bla", Level.DEBUG, "{}", "bla");
      try (ExecutionContext second = start.startChild("ss", 10, java.util.concurrent.TimeUnit.SECONDS)) {
        second.addLog(log);
      }
      List<Slf4jLogRecord> logs = new ArrayList<>(2);
      start.streamLogs(logs::add);
      Assert.assertEquals(log, logs.get(0));
    }

     try (ExecutionContext start = ExecutionContexts.start(10, TimeUnit.SECONDS)) {

      Tag<String, Void> tag = new ExecutionContext.Tag<String, Void>() {
        @Override
        public String toString() {
          return  "mine";
        }
      };
      start.combine(tag, "bla");
      Assert.assertEquals("bla", start.get(tag));
      start.combine(tag, "bla2");
      Assert.assertEquals("bla2", start.get(tag));

      Slf4jLogRecordImpl log = new Slf4jLogRecordImpl("bla", Level.DEBUG, "{}", "bla");
      Slf4jLogRecordImpl log2 = new Slf4jLogRecordImpl("bla2", Level.DEBUG, "{}", "bla2");
      CompletableFuture<String> fut = ContextPropagatingCompletableFuture.supplyAsync(() -> {
        ExecutionContexts.current().addLog(log);
        return "";
      }, DefaultExecutor.INSTANCE).whenComplete((a, t) -> {
        ExecutionContexts.current().addLog(log2);
      });
      fut.get();
      List<Slf4jLogRecord> logs = new ArrayList<>(2);
      start.streamLogs(logs::add);
      LOG.debug("Context logs", logs);
//      Assert.assertThat(logs, Matchers.hasItems(log, log2));
      Assert.assertEquals(log, logs.get(0));
      Assert.assertEquals(log2, logs.get(1));
    }


  }



}
