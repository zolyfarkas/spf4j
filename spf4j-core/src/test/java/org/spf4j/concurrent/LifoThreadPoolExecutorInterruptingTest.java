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
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.perf.cpu.CpuUsageSampler;
import org.spf4j.stackmonitor.Sampler;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings({"MDM_THREAD_YIELD", "PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS"})
public class LifoThreadPoolExecutorInterruptingTest {

  private static final Logger LOG = LoggerFactory.getLogger(LifoThreadPoolExecutorInterruptingTest.class);

  @Test(timeout = 60000)
  public void testLifoExecSQ() throws InterruptedException, IOException, ExecutionException {
    LifoThreadPoolExecutorSQP executor
            = new LifoThreadPoolExecutorSQP("test", 2, 8, 20, 0);
    File destFolder = new File(org.spf4j.base.Runtime.TMP_FOLDER);
    Sampler s = Sampler.getSampler(20, 10000, destFolder,
            "lifeTest1");
    s.start();
    org.spf4j.base.Runtime.gc(5000);

    ArrayBlockingQueue<Thread> q = new ArrayBlockingQueue<>(1);
    Future<Object> f = executor.submit(() -> {
      q.put(Thread.currentThread());
      return null;
    });
    q.take().interrupt();
    Assert.assertNull(f.get());
    Thread.sleep(100);
    long time = CpuUsageSampler.getProcessCpuTimeNanos();
    Thread.sleep(3000);
    long cpuTime = CpuUsageSampler.getProcessCpuTimeNanos() - time;
    File dumpToFile = s.dumpToFile();
    Assert.assertEquals(destFolder.getCanonicalFile(), dumpToFile.getParentFile().getCanonicalFile());
    LOG.info("Cpu profile saved to {}", dumpToFile);
    LOG.debug("CPU time = {} ns", cpuTime);
    s.stop();
    Assert.assertTrue("CPU Time = " + cpuTime, cpuTime < 1500000000);
    // 3260523000 with bug  148672000 without bug with profiler
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.SECONDS);
  }

 @Test(timeout = 60000)
  public void testLifoExecSQ2() throws InterruptedException, IOException, ExecutionException {
    LifoThreadPoolExecutorSQP executor
            = new LifoThreadPoolExecutorSQP("test", 2, 8, 20, 0);
    File destFolder = new File(org.spf4j.base.Runtime.TMP_FOLDER);
    Sampler s = Sampler.getSampler(20, 10000, destFolder,
            "lifeTest1");
    s.start();
    org.spf4j.base.Runtime.gc(5000);

    List<Runnable> running = executor.shutdownNow();
    Assert.assertTrue(running.isEmpty());
    Thread.sleep(100);
    long time = CpuUsageSampler.getProcessCpuTimeNanos();
    Thread.sleep(3000);
    long cpuTime = CpuUsageSampler.getProcessCpuTimeNanos() - time;
    File dumpToFile = s.dumpToFile();
    Assert.assertEquals(destFolder.getCanonicalFile(), dumpToFile.getParentFile().getCanonicalFile());
    LOG.info("Cpu profile saved to {}", dumpToFile);
    LOG.debug("CPU time = {} ns", cpuTime);
    s.stop();
    Assert.assertTrue("CPU Time = " + cpuTime, cpuTime < 1500000000);
    // 3260523000 with bug  148672000 without bug with profiler
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.SECONDS);
  }



}
