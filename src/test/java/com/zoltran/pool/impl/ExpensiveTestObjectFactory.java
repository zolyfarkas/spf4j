/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.pool.impl;

import com.zoltran.pool.ObjectCreationException;
import com.zoltran.pool.ObjectDisposeException;
import com.zoltran.pool.ObjectPool;
import java.io.IOException;

/**
 *
 * @author zoly
 */
public class ExpensiveTestObjectFactory implements ObjectPool.Factory<ExpensiveTestObject> {

    private final long maxIdleMillis;
    private final int nrUsesToFailAfter;
    private final long  minOperationMillis;
    private final long maxOperationMillis;

    public ExpensiveTestObjectFactory(long maxIdleMillis, int nrUsesToFailAfter, long minOperationMillis, long maxOperationMillis) {
        this.maxIdleMillis = maxIdleMillis;
        this.nrUsesToFailAfter = nrUsesToFailAfter;
        this.minOperationMillis = minOperationMillis;
        this.maxOperationMillis = maxOperationMillis;
    }

    public ExpensiveTestObjectFactory() {
        this(100, 10, 1, 20);
    }

    
    
    @Override
    public ExpensiveTestObject create() throws ObjectCreationException {
        return new ExpensiveTestObject(maxIdleMillis, nrUsesToFailAfter, minOperationMillis, maxOperationMillis);
    }

    @Override
    public void dispose(ExpensiveTestObject object) throws ObjectDisposeException {
        try {
            object.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Exception validate(ExpensiveTestObject object, Exception e) {
       if (e instanceof IOException) {
           return e;
       } else {
            try {
                object.doStuff();
                return null;
            } catch (IOException ex) {
                return ex;
            }
       }
    }
    
 
}
