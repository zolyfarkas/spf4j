/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.pool.impl;

import com.google.common.collect.LinkedHashMultimap;
import com.zoltran.pool.ObjectBorower;
import com.zoltran.pool.ObjectCreationException;
import com.zoltran.pool.ObjectPool;
import com.zoltran.pool.SmartObjectPool;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

    
/**
 *
 * @author zoly
 */
public class SimpleSmartObjectPool<T> implements SmartObjectPool<T> {

    private int maxSize;
    
    private final LinkedHashMultimap<ObjectBorower<T>, T> borrowedObjects = LinkedHashMultimap.create();
    
    private final List<T> returnedObjects = new ArrayList<T>();
    
    private final ReentrantLock lock;
    
    private final Condition available;
    
    private final ObjectPool.Factory<T> factory;
    
    private final long timeoutMillis;
    

    public SimpleSmartObjectPool(int maxSize, ObjectPool.Factory<T> factory, long timeoutMillis, boolean fair) {
        this.maxSize = maxSize;
        this.factory = factory;
        this.timeoutMillis = timeoutMillis;
        this.lock = new ReentrantLock(fair);
        this.available = this.lock.newCondition();
    }
    
    
    
    
    
    @Override
    public T borrowObject(ObjectBorower borower) throws InterruptedException, 
        TimeoutException, ObjectCreationException {
        lock.lock();
        try {
            if (returnedObjects.size() >0 ) {
                Iterator<T> it = returnedObjects.iterator();
                T object = it.next();
                it.remove();
                borrowedObjects.put(borower, object);
                return object;                
            } else if (borrowedObjects.size()< maxSize) {
                T object = factory.create();
                borrowedObjects.put(borower, object);
                return object;
            } else {
                if (borrowedObjects.isEmpty()) {
                    throw new RuntimeException("Pool size is probably closing down or is missconfigured withe size 0");
                }
                for(ObjectBorower<T> b: borrowedObjects.keySet()) {
                    T object = b.returnObjectIfAvailable();
                    if (object != null) {
                        borrowedObjects.remove(b, object);
                        borrowedObjects.put(borower, object);
                        return object;
                    }
                }
                ObjectBorower<T> b = borrowedObjects.keySet().iterator().next();
                T object = b.requestReturnObject();
                if (object != null) {
                   borrowedObjects.remove(b, object);
                   borrowedObjects.put(borower, object);
                   return object;
                }
                while (returnedObjects.isEmpty()) {               
                    if (!available.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
                        throw new TimeoutException("Object wait timeout expired " + timeoutMillis);
                    }
                }
                Iterator<T> it = returnedObjects.iterator();
                object = it.next();
                it.remove();
                borrowedObjects.put(borower, object);
                return object;                 
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void returnObject(T object, ObjectBorower borower) {
        lock.lock();
        try {
            borrowedObjects.remove(borower, object);
            returnedObjects.add(object);
            available.signalAll();           
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void dispose() throws InterruptedException, TimeoutException {
        lock.lock();
        try {
            maxSize = 0;
            for(ObjectBorower<T> b: borrowedObjects.keySet()) {
               b.dispose();
            }   
            disposeReturnedObjects();
            while (!borrowedObjects.isEmpty()) {               
                    if (!available.await(timeoutMillis, TimeUnit.MILLISECONDS))
                        throw new TimeoutException("Object wait timeout expired " + timeoutMillis);
                    disposeReturnedObjects();
            }           
             
        } finally {
            lock.unlock();
        }
    }

    private void disposeReturnedObjects() {
        for (T obj : returnedObjects) {
            factory.dispose(obj);
        }
        returnedObjects.clear();
    }
    
}
