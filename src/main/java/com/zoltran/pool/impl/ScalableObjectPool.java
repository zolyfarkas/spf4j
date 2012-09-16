/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.pool.impl;

import com.zoltran.pool.ObjectBorower;
import com.zoltran.pool.ObjectPool;
import com.zoltran.pool.SmartObjectPool;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author zoly
 */
public class ScalableObjectPool<T> implements ObjectPool<T>, ObjectBorower<T> {

    private final SmartObjectPool globalPool;
    
    private final ThreadLocal<List<T>> localObjects;

    public ScalableObjectPool(int maxSize, ObjectPool.Factory<T> factory, long timeoutMillis, boolean fair) {        
        globalPool = new SimpleSmartObjectPool(maxSize, factory, timeoutMillis, fair);
        localObjects = new ThreadLocal<List<T>>() {

            @Override
            protected List<T> initialValue() {
                return new ArrayList<T>();
            }
            
        };
    }
    
    
    
    @Override
    public T borrowObject() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void returnObject(T object) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void returnObject(T object, Exception e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void dispose() throws TimeoutException, InterruptedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void scan(ScanHandler<T> handler) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public T requestReturnObject() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public T returnObjectIfAvailable() throws InterruptedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
