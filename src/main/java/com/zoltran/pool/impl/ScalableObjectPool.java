/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.pool.impl;

import com.zoltran.pool.ObjectCreationException;
import com.zoltran.pool.ObjectPool;
import com.zoltran.pool.Scanable;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author zoly
 */
public class ScalableObjectPool<T> implements ObjectPool<T>,  Scanable<T> {

    private final SimpleSmartObjectPool<ObjectHolder<T>> globalPool;
    
    private final ThreadLocal<LocalObjectPool<T>> localPool;
    
    
    public ScalableObjectPool(int maxSize, ObjectPool.Factory<T> factory,
            long timeoutMillis, boolean fair) {        
        globalPool = new SimpleSmartObjectPool<ObjectHolder<T>>
                (maxSize, new ObjectHolderFactory<T>(factory), timeoutMillis, fair);
        localPool = new ThreadLocal<LocalObjectPool<T>>() {
                    @Override
                    protected LocalObjectPool<T> initialValue()
                    {
                        return new LocalObjectPool<T>(globalPool);
                    }
        };
    }
    
    
    
    @Override
    public T borrowObject() throws ObjectCreationException, InterruptedException, TimeoutException {
        return localPool.get().borrowObject();
    }

    @Override
    public void returnObject(T object, Exception e) throws TimeoutException, InterruptedException {
        localPool.get().returnObject(object, e);
    }

    @Override
    public void dispose() throws TimeoutException, InterruptedException {
        globalPool.dispose();
    }

    @Override
    public boolean scan(final ScanHandler<T> handler) {
        return globalPool.scan(new ScanHandler<ObjectHolder<T>>() {

            @Override
            public boolean handle(ObjectHolder<T> object)
            {
                return object.scan(handler);
            }
        });
    }
    
}
