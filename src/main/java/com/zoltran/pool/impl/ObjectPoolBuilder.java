 /*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package com.zoltran.pool.impl;

import com.zoltran.pool.ObjectDisposeException;
import com.zoltran.pool.ObjectPool;
import com.zoltran.pool.Scanable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author zoly
 */

public class ObjectPoolBuilder<T>
{
    private int maxSize;
    private ObjectPool.Factory<T> factory;
    private long timeoutMillis;
    private boolean fair;
    private ScheduledExecutorService maintenanceExecutor;
    private long maintenanceInterval;
    private ObjectPool.Hook<T> borrowHook;
    private ObjectPool.Hook<T> returnHook;
    
    public ObjectPoolBuilder(int maxSize, ObjectPool.Factory<T> factory) {     
        this.fair=true;
        this.timeoutMillis = 60000;
        this.maxSize = maxSize;
        this.factory = factory;
    }
    
    public ObjectPoolBuilder<T> unfair() {
        this.fair = false;
        return this;
    }
    
    public ObjectPoolBuilder<T> withOperationTimeout(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        return this;
    }
    
    public ObjectPoolBuilder<T> withMaintenance(ScheduledExecutorService exec, 
            long maintenanceInterval) {
        this.maintenanceExecutor = exec;
        this.maintenanceInterval = maintenanceInterval;
        return this;
    }
    
    public ObjectPoolBuilder<T> withBorrowHook(ObjectPool.Hook<T> hook) {
        this.borrowHook = hook;
        return this;
    }
    
    public ObjectPoolBuilder<T> withReturnHook(ObjectPool.Hook<T> hook) {
        this.returnHook = hook;
        return this;
    }   
    
    public  ObjectPool<T> build() {
        ObjectPool<T> pool = new ScalableObjectPool<T>(maxSize, factory, timeoutMillis, fair);
        if (borrowHook != null || returnHook != null) {
            pool = new ObjectPoolWrapper<T>(pool, borrowHook, returnHook);
        }
        final Scanable<ObjectHolder<T>> scanable = (Scanable<ObjectHolder<T>>) pool;
        maintenanceExecutor.scheduleWithFixedDelay(new Runnable() {

            @Override
            public void run() {
                try {
                    scanable.scan(new Scanable.ScanHandler<ObjectHolder<T>> ()  {

                        @Override
                        public boolean handle(ObjectHolder<T> object) throws ObjectDisposeException {
                            T o = object.borrowObjectIfAvailable();
                            if (o != null) {
                                try {
                                    factory.validate(o, null);
                                } finally {
                                    object.returnObject(o, null);                              
                                }
                            }
                            return true;
                        }
                    });
                } catch (Exception ex) {
                    
                }
            }
        }, maintenanceInterval, maintenanceInterval, TimeUnit.MILLISECONDS);
        return pool;
    }
}
