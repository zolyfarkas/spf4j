
/*
 * Copyright (c) 2015, Zoltan Farkas All Rights Reserved.
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
 */
package org.spf4j.concurrent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.perf.cpu.CpuUsageSampler;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("MDM_THREAD_YIELD")
public class LifoThreadPoolExecutorTestCoreIdling {


    @Test
    public void testLifoExecSQ() throws InterruptedException, IOException {
        LifoThreadPoolExecutorSQP executor =
                new LifoThreadPoolExecutorSQP("test", 2, 8, 20, 1024, 1024);
        Thread.sleep(20);
        long time = CpuUsageSampler.getProcessCpuTimeNanos();
        Thread.sleep(3000);
        long cpuTime =  CpuUsageSampler.getProcessCpuTimeNanos() - time;
        Assert.assertTrue(cpuTime < 200000000);
        System.out.println(cpuTime); // 6069497000 with bug  53945000 without bug
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }


    @Test
    public void testLifoExecSQMutable() throws InterruptedException, IOException {
        MutableLifoThreadPoolExecutorSQP executor =
                new MutableLifoThreadPoolExecutorSQP("test", 2, 8, 20, 1024, 1024);
        Thread.sleep(20);
        long time = CpuUsageSampler.getProcessCpuTimeNanos();
        Thread.sleep(3000);
        long cpuTime =  CpuUsageSampler.getProcessCpuTimeNanos() - time;
        Assert.assertTrue(cpuTime < 200000000);
        System.out.println(cpuTime); // 6069497000 with bug  53945000 without bug
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }


}
