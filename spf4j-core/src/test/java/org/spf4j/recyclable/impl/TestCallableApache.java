/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.recyclable.impl;


import java.util.concurrent.Callable;
import org.apache.commons.pool.ObjectPool;

/**
 *
 * @author zoly
 */
public final class TestCallableApache implements Callable<Integer> {
    private final ObjectPool<ExpensiveTestObject> pool;
    private final int testNr;

    public TestCallableApache(final ObjectPool<ExpensiveTestObject> pool, final int testNr) {
        this.pool = pool;
        this.testNr = testNr;
    }

    @Override
    public Integer call() throws Exception {
        ExpensiveTestObject object = pool.borrowObject();
        try {
            object.doStuff();
            pool.returnObject(object);
        } catch (Exception e) {
            pool.invalidateObject(object);
            throw e;
        }
        return testNr;
    }
    
}
