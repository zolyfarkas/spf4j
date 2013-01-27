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
import com.zoltran.pool.Template;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author zoly
 */
public class TestCallable implements Callable<Integer> {
    private final ObjectPool<ExpensiveTestObject> pool;
    private final int testNr;

    public TestCallable(ObjectPool<ExpensiveTestObject> pool, int testNr) {
        this.pool = pool;
        this.testNr = testNr;
    }

    @Override
    public Integer call() throws ObjectCreationException, ObjectBorrowException, InterruptedException, TimeoutException, ObjectReturnException, ObjectDisposeException, IOException {
        Template.doOnPooledObject(new ObjectPool.Hook<ExpensiveTestObject, IOException>() {
            @Override
            public void handle(ExpensiveTestObject object) throws IOException {
                object.doStuff();
            }
        }, pool, IOException.class);
        return testNr;
    }
    
}
