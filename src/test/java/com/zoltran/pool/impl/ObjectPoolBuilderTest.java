/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.pool.impl;

import com.zoltran.pool.ObjectBorrowException;
import com.zoltran.pool.ObjectCreationException;
import com.zoltran.pool.ObjectDisposeException;
import com.zoltran.pool.ObjectPool;
import com.zoltran.pool.ObjectReturnException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
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
  
}
