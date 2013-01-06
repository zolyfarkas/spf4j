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

import com.google.common.base.Throwables;
import com.zoltran.pool.ObjectBorrowException;
import com.zoltran.pool.ObjectCreationException;
import com.zoltran.pool.ObjectDisposeException;
import com.zoltran.pool.ObjectPool;
import com.zoltran.pool.ObjectReturnException;
import com.zoltran.pool.Template;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.junit.Assert.*;
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
     
        ExecutorService exec = Executors.newFixedThreadPool(10);
        final CompletionService<Integer> cs = new ExecutorCompletionService(exec);
        final ConcurrentMap<Future<Integer>, Integer> futureToTask = new ConcurrentHashMap<Future<Integer>, Integer>();
        
        for (int i=0; i<1000; i++) {
            Future<Integer> future = cs.submit(new TestCallable(pool, i));
            futureToTask.put(future, i);
        }        
        
        Thread completionManager = new Thread(new Runnable (){

            Map<Integer, Integer> failureCounts = new HashMap<Integer, Integer>();
            
            @Override
            public void run() {
               try {
               while (true) {
                   Future<Integer> taskFuture = cs.take();
                       try {
                           Integer taskNr = taskFuture.get();
                       } catch (ExecutionException ex) {
                           
                       }
               }
               } catch (InterruptedException e) {
                   System.err.println("Interrupted:" + Throwables.getStackTraceAsString(e));
               } 
            }
        });
        
        
        
        
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
        InterruptedException, TimeoutException, ObjectReturnException, ObjectDisposeException{
            Template.doOnPooledObject(new ObjectPool.Hook<ExpensiveTestObject> () {

                @Override
                public void handle(ExpensiveTestObject object) throws Exception {
                    object.doStuff();
                }
            }, pool );
        return testNr;    
        }
        
    }
    
    
  
}
