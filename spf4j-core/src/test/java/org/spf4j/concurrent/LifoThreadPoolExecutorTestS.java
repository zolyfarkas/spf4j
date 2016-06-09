
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.spf4j.base.Pair;
import org.spf4j.base.Throwables;

/**
 *
 * @author zoly
 */
public class LifoThreadPoolExecutorTestS {


    @Test
    public void testLifoExecSQ() throws Exception {
        LifoThreadPoolExecutorSQP executor =
                new LifoThreadPoolExecutorSQP("test", 2, 8, 10000, 1024, 1024);
        testPool(executor);
    }

    @Test
    @Ignore
    @SuppressFBWarnings("HES_LOCAL_EXECUTOR_SERVICE")
    public void testJdkExec() throws Exception {
        final LinkedBlockingQueue linkedBlockingQueue = new LinkedBlockingQueue(1024);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 8, 60000, TimeUnit.MILLISECONDS,
                linkedBlockingQueue);
        testPool(executor);
    }

    public static void testPool(final ExecutorService executor)
            throws InterruptedException, IOException, Exception {
        final LongAdder adder = new LongAdder();
        final int testCount = 20000;
        long rejected = 0;
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                adder.increment();
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    Throwables.writeTo(ex, System.err, Throwables.Detail.STANDARD);
                }
            }
        };
        long start = System.currentTimeMillis();
        List<Future> futures = new ArrayList<>(testCount);
        for (int i = 0; i < testCount; i++) {
            try {
               futures.add(executor.submit(runnable));
            } catch (RejectedExecutionException ex) {
                rejected++;
                runnable.run();
            }
        }
        Pair<Map<Future, Object>, Exception> results = Futures.getAll(60000, futures);
        Exception second = results.getSecond();
        if (second != null) {
          throw second;
        }
        executor.shutdown();
        boolean awaitTermination = executor.awaitTermination(10000, TimeUnit.MILLISECONDS);
        System.out.println("Stats for " + executor.getClass()
                + ", rejected = " + rejected + ", Exec time = " + (System.currentTimeMillis() - start));
        Assert.assertTrue(awaitTermination);
        Assert.assertEquals(testCount, adder.sum());
    }

}
