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
package org.spf4j.pool.impl;

import org.spf4j.base.AbstractRunnable;
import org.spf4j.pool.ObjectCreationException;
import org.spf4j.pool.ObjectDisposeException;
import org.spf4j.pool.ObjectPool;
import org.spf4j.pool.Scanable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author zoly
 */
public class ObjectPoolBuilder<T,E extends Exception> {

    private int maxSize;
    private ObjectPool.Factory<T> factory;
    private long timeoutMillis;
    private boolean fair;
    private ScheduledExecutorService maintenanceExecutor;
    private long maintenanceIntervalMillis;
    private ObjectPool.Hook<T,E> borrowHook;
    private ObjectPool.Hook<T,E> returnHook;
    private int initialSize;

    public ObjectPoolBuilder(int maxSize, ObjectPool.Factory<T> factory) {
        this.fair = true;
        this.timeoutMillis = 60000;
        this.maxSize = maxSize;
        this.factory = factory;
        this.initialSize = 0;
    }

    public ObjectPoolBuilder<T,E> unfair() {
        this.fair = false;
        return this;
    }

    public ObjectPoolBuilder<T,E> withInitialSize(int initialSize) {
        this.initialSize = initialSize;
        return this;
    }
    
    public ObjectPoolBuilder<T,E> withOperationTimeout(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        return this;
    }

    public ObjectPoolBuilder<T,E> withMaintenance(ScheduledExecutorService exec,
            long maintenanceIntervalMillis) {
        this.maintenanceExecutor = exec;
        this.maintenanceIntervalMillis = maintenanceIntervalMillis;
        return this;
    }

    public ObjectPoolBuilder<T,E> withBorrowHook(ObjectPool.Hook<T,E> hook) {
        this.borrowHook = hook;
        return this;
    }

    public ObjectPoolBuilder<T, E> withReturnHook(ObjectPool.Hook<T, E> hook) {
        this.returnHook = hook;
        return this;
    }

    public ObjectPool<T> build() throws ObjectCreationException {
        ObjectPool<T> pool = new ScalableObjectPool<T>(initialSize, maxSize, factory, timeoutMillis, fair);
        if (borrowHook != null || returnHook != null) {
            pool = new ObjectPoolWrapper<T>(pool, borrowHook, returnHook);
        }
        final Scanable<ObjectHolder<T>> scanable = (Scanable<ObjectHolder<T>>) pool;
        if (maintenanceExecutor != null) {
            maintenanceExecutor.scheduleWithFixedDelay(new AbstractRunnable(true) {
                @Override
                public void doRun() throws Exception {
                        scanable.scan(new Scanable.ScanHandler<ObjectHolder<T>>() {
                            @Override
                            public boolean handle(ObjectHolder<T> object) throws ObjectDisposeException {
                                object.validateObjectIfNotBorrowed();
                                return true;
                            }
                        });
                   
                }
            }, maintenanceIntervalMillis, maintenanceIntervalMillis, TimeUnit.MILLISECONDS);
        }
        return pool;
    }
}
