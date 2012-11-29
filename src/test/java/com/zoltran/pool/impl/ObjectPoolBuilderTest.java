/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.pool.impl;

import com.zoltran.pool.ObjectPool;
import java.util.concurrent.ScheduledExecutorService;
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
    public void testBuild() {
        System.out.println("build");
        ObjectPool<ExpensiveTestObject> pool = new ObjectPoolBuilder(10, new ExpensiveTestObjectFactory()).build();
        fail("The test case is a prototype.");
    }
    
    
    @Test
    public void testUnfair() {
        System.out.println("unfair");
        ObjectPoolBuilder instance = null;
        ObjectPoolBuilder expResult = null;
        ObjectPoolBuilder result = instance.unfair();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withOperationTimeout method, of class ObjectPoolBuilder.
     */
    @Test
    public void testWithOperationTimeout() {
        System.out.println("withOperationTimeout");
        long timeoutMillis = 0L;
        ObjectPoolBuilder instance = null;
        ObjectPoolBuilder expResult = null;
        ObjectPoolBuilder result = instance.withOperationTimeout(timeoutMillis);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withMaintenance method, of class ObjectPoolBuilder.
     */
    @Test
    public void testWithMaintenance() {
        System.out.println("withMaintenance");
        ScheduledExecutorService exec = null;
        long maintenanceInterval = 0L;
        ObjectPoolBuilder instance = null;
        ObjectPoolBuilder expResult = null;
        ObjectPoolBuilder result = instance.withMaintenance(exec, maintenanceInterval);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withBorrowHook method, of class ObjectPoolBuilder.
     */
    @Test
    public void testWithBorrowHook() {
        System.out.println("withBorrowHook");
        ObjectPoolBuilder instance = null;
        ObjectPoolBuilder expResult = null;
        ObjectPoolBuilder result = instance.withBorrowHook(null);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of withReturnHook method, of class ObjectPoolBuilder.
     */
    @Test
    public void testWithReturnHook() {
        System.out.println("withReturnHook");
        ObjectPoolBuilder instance = null;
        ObjectPoolBuilder expResult = null;
        ObjectPoolBuilder result = instance.withReturnHook(null);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}
