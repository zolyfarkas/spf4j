/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.pool.impl;

import com.zoltran.pool.ObjectBorrowException;
import com.zoltran.pool.ObjectCreationException;
import com.zoltran.pool.ObjectPool;
import com.zoltran.pool.ObjectReturnException;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author zoly
 */
public class ObjectPoolWrapper<T> implements ObjectPool<T> {

    private final ObjectPool<T> pool;
    
    private final Hook<T> borrowHook;
    
    private final Hook<T> returnHook;

    public ObjectPoolWrapper(ObjectPool<T> pool, Hook<T> borrowHook, Hook<T> returnHook) {
        this.pool = pool;
        this.borrowHook = borrowHook;
        this.returnHook = returnHook;
    }
    
    
    
    
    @Override
    public T borrowObject() throws ObjectCreationException, ObjectBorrowException, InterruptedException, TimeoutException {
        T result = pool.borrowObject();
        try {
            borrowHook.handle(result);
            return result;
        } catch (Exception e) {
            try {
                pool.returnObject(result, e);
            } catch (ObjectReturnException ex) {
                throw new RuntimeException("Exception while executing borrow hook", ex);
            }
            throw new ObjectBorrowException("Exception while executing borrow hook", e);
        }
    }

    @Override
    public void returnObject(T object, Exception e) throws ObjectReturnException, TimeoutException, InterruptedException {
        try {
            returnHook.handle(object);
        } catch (Exception ex) {
            throw new ObjectReturnException("Exception while executing borrow hook", e);
        } finally {
            pool.returnObject(object, e);
        }
    }

    @Override
    public void dispose() throws TimeoutException, InterruptedException {
        pool.dispose();
    }
    
}
