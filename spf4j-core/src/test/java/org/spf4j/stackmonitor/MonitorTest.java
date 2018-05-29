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
package org.spf4j.stackmonitor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.ExitException;
import org.spf4j.base.NoExitSecurityManager;

@NotThreadSafe
public final class MonitorTest {

  private static volatile boolean stopped;

  private SecurityManager original;

  @Before
  public void init() {
    original = System.getSecurityManager();
    System.setSecurityManager(new NoExitSecurityManager());
    Thread.setDefaultUncaughtExceptionHandler((final Thread t, final Throwable e) -> {
      StringWriter strw = new StringWriter();
      e.printStackTrace(new PrintWriter(strw));
      Assert.fail("Got Exception: " + strw);
    });
  }

  @After
  public void cleanup() {
    System.setSecurityManager(original);
  }

  @Test(expected = ExitException.class)
  public void testError() throws ClassNotFoundException, NoSuchMethodException,
          IllegalAccessException, InvocationTargetException, IOException {
    String report = File.createTempFile("stackSample", ".html").getPath();
    System.setSecurityManager(new NoExitSecurityManager());
    Monitor.main(new String[]{"-ASDF", "-f", report, "-ss", "-si", "10", "-w", "600", "-main",
      MonitorTest.class.getName()});
  }

  @Test
  public void testJmx() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
          InvocationTargetException {
    try {
      Monitor.main(new String[]{"-ss", "-si", "10", "-main",
        MonitorTest.class.getName()});
    } catch (ExitException ex) {
      Assert.assertEquals(0, ex.getExitCode());
    }
  }

  @SuppressFBWarnings("MDM_THREAD_YIELD")
  public static void main(final String[] args) throws InterruptedException {
    stopped = false;
    try (ExecutionContext ctx = ExecutionContexts.start("main", 10, TimeUnit.MINUTES)) {
      List<Thread> threads = new ArrayList<Thread>(20);
      for (int i = 0; i < 20; i++) {
        Thread t;
        t = new Thread(new AbstractRunnable() {
          @Override
          @SuppressFBWarnings("PREDICTABLE_RANDOM")
          public void doRun() throws InterruptedException {
            try (ExecutionContext tctx = ExecutionContexts.start("testThread", ctx, 10, TimeUnit.MINUTES)) {
              while (!stopped) {
                double rnd = ThreadLocalRandom.current().nextDouble();
                if (rnd < 0.33) {
                  doStuff1(rnd, 50);
                } else if (rnd < 0.66) {
                  doStuff2(rnd);
                } else {
                  doStuff3(rnd);
                }
              }
            }
          }

          @SuppressFBWarnings("MDM_THREAD_YIELD")
          private double doStuff3(final double rnd) throws InterruptedException {
            Thread.sleep(1);
            if (rnd > 0.8) {
              doStuff2(rnd);
            }
            return rnd * Math.pow(2, 10000);
          }

          @SuppressFBWarnings("MDM_THREAD_YIELD")
          private double doStuff2(final double prnd) throws InterruptedException {
            double rnd = prnd;
            Thread.sleep(1);
            for (int j = 0; j < 10000; j++) {
              rnd = rnd + rnd;
            }
            return rnd;
          }

          @SuppressFBWarnings("MDM_THREAD_YIELD")
          private void doStuff1(final double rnd, final int depth) throws InterruptedException {
            if (depth <= 0) {
              Thread.sleep(10);
            } else {
              doStuff1(rnd, depth - 1);
            }
          }
        }, "Thread" + i);
        t.start();
        threads.add(t);
      }
      Thread.sleep(5000);
      stopped = true;
      for (Thread t : threads) {
        t.join(3000);
      }
    }

  }
}
