/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.pool.impl;

import com.zoltran.base.ExceptionChain;
import com.zoltran.pool.ObjectBorrowException;
import com.zoltran.pool.ObjectCreationException;
import com.zoltran.pool.ObjectDisposeException;
import com.zoltran.pool.ObjectPool;
import com.zoltran.pool.ObjectReturnException;
import com.zoltran.pool.Scanable;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public class ObjectPoolWrapper<T> implements ObjectPool<T> , Scanable<ObjectHolder<T>>{

    private final ObjectPool<T> pool;
    
    private final Hook<T> borrowHook;
    
    private final Hook<T> returnHook;

    public ObjectPoolWrapper(ObjectPool<T> pool, @Nullable Hook<T> borrowHook, 
            @Nullable Hook<T> returnHook) {
        this.pool = pool;
        this.borrowHook = borrowHook;
        this.returnHook = returnHook;
        if (borrowHook == null && returnHook == null) {
            throw new IllegalArgumentException("Both hooks can't be null");
        }
    }
    
    
    
    
    @Override
    public T borrowObject() throws ObjectCreationException, ObjectBorrowException, InterruptedException, TimeoutException {
        T result = pool.borrowObject();
        try {
            if (borrowHook != null) {
                borrowHook.handle(result);
            } 
            return result;
        } catch (Exception e) {
            try {
                pool.returnObject(result, e);
            } catch (ObjectReturnException ex) {
                throw ExceptionChain.chain(new RuntimeException(ex), e);
            } catch (ObjectDisposeException ex) {
                 throw ExceptionChain.chain(new RuntimeException(ex), e);
            }
            throw new ObjectBorrowException("Exception while executing borrow hook", e);
        }
    }

    @Override
    public void returnObject(T object, Exception e) throws ObjectReturnException, ObjectDisposeException {
        try {
            if (returnHook != null) { 
                returnHook.handle(object);
            }
        } catch (Exception ex) {
            throw new ObjectReturnException("Exception while executing borrow hook", e);
        } finally {
            pool.returnObject(object, e);
        }
    }

    @Override
    public void dispose() throws ObjectDisposeException {
        pool.dispose();
    }

    @Override
    public boolean scan(ScanHandler<ObjectHolder<T>> handler) throws Exception {
        if (pool instanceof Scanable) {
            return ((Scanable<ObjectHolder<T>>)pool).scan(handler);
        } else {
            throw new RuntimeException("Wrapped pool is not scanable");
        }
    }

  
    
}
