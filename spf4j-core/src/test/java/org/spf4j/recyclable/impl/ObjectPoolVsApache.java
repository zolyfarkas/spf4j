 /*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
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
package org.spf4j.recyclable.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.spf4j.base.Callables;
import org.spf4j.concurrent.RetryExecutor;
import org.spf4j.recyclable.ObjectCreationException;
import org.spf4j.recyclable.RecyclingSupplier;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import org.junit.Assert;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.junit.Test;
import org.spf4j.base.ParameterizedSupplier;

/**
 *
 * @author zoly
 */
public final class ObjectPoolVsApache {

    private static final int TEST_TASKS = 1000000;

    @Test(timeout = 200000)
    @SuppressFBWarnings("HES_LOCAL_EXECUTOR_SERVICE")
    public void testPerformance() throws ObjectCreationException, InterruptedException, ExecutionException {
        System.out.println("poolUse");
        final RecyclingSupplier<ExpensiveTestObject> pool
                = new RecyclingSupplierBuilder(10, new ExpensiveTestObjectFactory(1000, 100, 0, 1)).build();
        final org.apache.commons.pool.impl.GenericObjectPool apool
                = new GenericObjectPool(new ExpensiveTestObjectFactoryApache(1000, 10, 0, 1), 10);
        ExecutorService execService = Executors.newFixedThreadPool(10);
        BlockingQueue<Future<?>> completionQueue = new LinkedBlockingDeque<>();
        RetryExecutor exec
                = new RetryExecutor(execService,
                        new ParameterizedSupplier<Callables.DelayPredicate<Exception>, Callable<Object>>() {

                    @Override
                    public Callables.DelayPredicate<Exception> get(final Callable<Object> parameter) {
                        return new Callables.DelayPredicate<Exception>() {

                            @Override
                            public int apply(final Exception value) {
                                return 0;
                            }
                        };
                    }
                }, completionQueue);
        long zpooltime = testPool(exec, pool, completionQueue);
        long apooltime = testPoolApache(exec, apool, completionQueue);
        Assert.assertTrue("apache pool must be slower", apooltime > zpooltime);

    }

    private long testPool(final RetryExecutor exec, final RecyclingSupplier<ExpensiveTestObject> pool,
            final BlockingQueue<Future<?>> completionQueue) throws InterruptedException, ExecutionException {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < TEST_TASKS; i++) {
            exec.submit(new TestCallable(pool, i));
        }
        for (int i = 0; i < TEST_TASKS; i++) {
            completionQueue.take().get();
        }
        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("Completed all " + TEST_TASKS + " tasks in " + elapsedTime + "ms ");
        return elapsedTime;
    }


    private long testPoolApache(final RetryExecutor exec,
            final org.apache.commons.pool.impl.GenericObjectPool pool,
            final BlockingQueue<Future<?>> completionQueue) throws InterruptedException, ExecutionException {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < TEST_TASKS; i++) {
            exec.submit(new TestCallableApache(pool, i));
        }
        for (int i = 0; i < TEST_TASKS; i++) {
            completionQueue.take().get();
        }
        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("Completed all " + TEST_TASKS + " tasks in " + elapsedTime + "ms ");
        return elapsedTime;
    }



}
