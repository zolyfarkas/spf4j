/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.pool.impl;

import org.spf4j.pool.ObjectBorrowException;
import org.spf4j.pool.ObjectCreationException;
import org.spf4j.pool.ObjectDisposeException;
import org.spf4j.pool.ObjectPool;
import org.spf4j.pool.ObjectReturnException;
import org.spf4j.pool.Template;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import org.spf4j.base.Handler;

/**
 *
 * @author zoly
 */
public final class TestCallable implements Callable<Integer> {
    private final ObjectPool<ExpensiveTestObject> pool;
    private final int testNr;

    public TestCallable(final ObjectPool<ExpensiveTestObject> pool, final int testNr) {
        this.pool = pool;
        this.testNr = testNr;
    }

    @Override
    public Integer call() throws ObjectCreationException, ObjectBorrowException, InterruptedException,
            TimeoutException, ObjectReturnException, ObjectDisposeException, IOException {
        Template.doOnPooledObject(new Handler<ExpensiveTestObject, IOException>() {
            @Override
            public void handle(final ExpensiveTestObject object, final long deadline) throws IOException {
                object.doStuff();
            }
        }, pool, 60000);
        return testNr;
    }
    
}
