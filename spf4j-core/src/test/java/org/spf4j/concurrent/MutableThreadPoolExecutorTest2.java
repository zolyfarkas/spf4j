
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class MutableThreadPoolExecutorTest2 {


    @Test
    public void testLifoExecSQ() throws InterruptedException, IOException, ExecutionException {
        MutableLifoThreadPoolExecutorSQP executor
                = new MutableLifoThreadPoolExecutorSQP("test", 2, 8, 60000, 1024, 1024);
        testPoolThreadDynamics(executor);
    }

    @SuppressFBWarnings("MDM_THREAD_YIELD")
    public static void testPoolThreadDynamics(final MutableLifoThreadPoolExecutorSQP le) throws InterruptedException {
        le.setMaxIdleTimeMillis(1000);
        LifoThreadPoolExecutorTest2.testMaxParallel(le, 10);

        Assert.assertEquals(8, le.getThreadCount());
        le.setMaxThreadCount(4);
        Thread.sleep(1000); // allow time for threads to retire.
        Assert.assertEquals(2, le.getThreadCount());
        le.setMaxIdleTimeMillis(1000);
        LifoThreadPoolExecutorTest2.testMaxParallel(le, 8);
        Assert.assertEquals(4, le.getThreadCount());

        le.shutdown();
        boolean awaitTermination = le.awaitTermination(10000, TimeUnit.MILLISECONDS);
        Assert.assertTrue(awaitTermination);
    }

}
