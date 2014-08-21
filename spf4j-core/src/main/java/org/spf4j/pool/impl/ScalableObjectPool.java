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

import org.spf4j.pool.ObjectCreationException;
import org.spf4j.pool.ObjectDisposeException;
import org.spf4j.pool.ObjectPool;
import org.spf4j.pool.Scanable;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author zoly
 */
// a pool instance is tipically alive for the entire life of the process
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings("PMB_INSTANCE_BASED_THREAD_LOCAL")
final class ScalableObjectPool<T> implements ObjectPool<T>,  Scanable<ObjectHolder<T>> {

    private final SimpleSmartObjectPool<ObjectHolder<T>> globalPool;
    
    private final ThreadLocal<LocalObjectPool<T>> localPool;
    
    
    public ScalableObjectPool(final int initialSize, final int maxSize, final ObjectPool.Factory<T> factory,
            final long timeoutMillis, final boolean fair) throws ObjectCreationException {
        globalPool = new SimpleSmartObjectPool<ObjectHolder<T>>(initialSize, maxSize,
                new ObjectHolderFactory<T>(initialSize, factory), timeoutMillis, fair);
        localPool = new ThreadLocal<LocalObjectPool<T>>() {
                    @Override
                    protected LocalObjectPool<T> initialValue() {
                        return new LocalObjectPool<T>(globalPool);
                    }
        };
    }
    
    
    
    @Override
    public T borrowObject() throws ObjectCreationException, InterruptedException, TimeoutException {
        return localPool.get().borrowObject();
    }

    @Override
    public void returnObject(final T object, final Exception e) {
        localPool.get().returnObject(object, e);
    }

    @Override
    public void dispose() throws ObjectDisposeException, InterruptedException {
        globalPool.dispose();
    }

    @Override
    public boolean scan(final ScanHandler<ObjectHolder<T>> handler) throws Exception {
        return globalPool.scan(handler);
    }
    
    public void requestReturnFromBorrowersIfNotInUse() throws InterruptedException {
        globalPool.requestReturnFromBorrowersIfNotInUse();
    }

    @Override
    public String toString() {
        return "ScalableObjectPool{" + "globalPool=" + globalPool + '}';
    }
    
    
    
}
