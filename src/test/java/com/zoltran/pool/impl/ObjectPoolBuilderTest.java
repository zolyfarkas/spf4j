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
package com.zoltran.pool.impl;

import com.zoltran.base.Callables;
import com.zoltran.base.RetryExecutor;
import com.zoltran.pool.ObjectBorrowException;
import com.zoltran.pool.ObjectCreationException;
import com.zoltran.pool.ObjectDisposeException;
import com.zoltran.pool.ObjectPool;
import com.zoltran.pool.ObjectReturnException;
import com.zoltran.pool.Template;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class ObjectPoolBuilderTest {
  
    /**
     * Test of build method, of class ObjectPoolBuilder.
     */
    @Test
    public void testBuild() throws ObjectCreationException, ObjectBorrowException, InterruptedException, TimeoutException, ObjectReturnException, ObjectDisposeException {
        System.out.println("build");
        ObjectPool<ExpensiveTestObject> pool = new ObjectPoolBuilder(10, new ExpensiveTestObjectFactory()).build();
        System.out.println(pool);
        ExpensiveTestObject object = pool.borrowObject();
        System.out.println(pool);
        pool.returnObject(object, null);
        System.out.println(pool);       
    }
    
    
    @Test
    public void testPoolUse() throws ObjectCreationException, ObjectBorrowException, InterruptedException, TimeoutException, ObjectReturnException, ObjectDisposeException {
        System.out.println("poolUse");
        final ObjectPool<ExpensiveTestObject> pool = new ObjectPoolBuilder(10, new ExpensiveTestObjectFactory()).build();
     
        Thread monitor = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    Assert.fail("Test was interrupted and needs to finish in 10 seconds");
                }
                
//                Map<Thread, StackTraceElement []> stackTraces = Thread.getAllStackTraces();
//                for (Map.Entry<Thread, StackTraceElement []> entry: stackTraces.entrySet()) {
//                    System.err.println("Thread: " +entry.getKey());
//                    System.err.println("Stack Trace:");
//                    StackTraceElement[] trace = entry.getValue();
//                    for (int i=0; i < trace.length; i++)
//                        System.err.println("\tat " + trace[i]);
//                    }
                ThreadMXBean threadMX = ManagementFactory.getThreadMXBean();
                System.err.println(Arrays.toString(threadMX.dumpAllThreads(true, true)));
                
                Assert.fail("Test needs to finish in 10 seconds, Stack Traces");
            }
        });
        monitor.start();
        ExecutorService execService = Executors.newFixedThreadPool(10);
        RetryExecutor exec = new RetryExecutor(execService, 8, 16, 5000, Callables.RETRY_FOR_ANY_EXCEPTION);
        List<Future<Integer>> futures = new ArrayList<Future<Integer>>();
        for (int i=0; i<1000; i++) {
            Future<Integer> future = exec.submit(new TestCallable(pool, i));
            futures.add(future);
        }        
        for (Future<Integer> future: futures) {
            try {
                System.out.println("Task " + future.get() + " finished ");
            } catch (ExecutionException ex) {
                System.out.println("Task " + future + " faild with " + ex);
                ex.printStackTrace();
            }
        }
        
    }
    
    
    public static class TestCallable implements Callable<Integer> {

        private final ObjectPool<ExpensiveTestObject> pool;
        private final int testNr;
        
        public TestCallable (ObjectPool<ExpensiveTestObject> pool, int testNr) {
            this.pool = pool;
            this.testNr = testNr;
        }
        
        @Override
        public Integer call() throws ObjectCreationException, ObjectBorrowException,
        InterruptedException, TimeoutException, ObjectReturnException, ObjectDisposeException, IOException{
            Template.doOnPooledObject(new ObjectPool.Hook<ExpensiveTestObject, IOException> () {

                @Override
                public void handle(ExpensiveTestObject object) throws IOException {
                    object.doStuff();
                }
                
            }, pool, IOException.class );
        return testNr;    
        }
        
    }
    
    
  
}
