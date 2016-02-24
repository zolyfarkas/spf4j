
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
package org.spf4j.recyclable.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import org.spf4j.base.Either;
import org.spf4j.recyclable.ObjectBorower;
import org.spf4j.recyclable.ObjectCreationException;
import org.spf4j.recyclable.ObjectDisposeException;
import org.spf4j.recyclable.RecyclingSupplier;
import org.spf4j.recyclable.SmartRecyclingSupplier;

/**
 *
 * @author zoly
 */
final class LocalObjectPool<T> implements RecyclingSupplier<T>, ObjectBorower<ObjectHolder<T>> {

    private final Queue<ObjectHolder<T>> localObjects;
    private final Map<T, ObjectHolder<T>> borrowedObjects;
    private final SmartRecyclingSupplier<ObjectHolder<T>> globalPool;
    private int reqReturnObjects;
    private final Thread thread;
    private final ReentrantLock lock;

    LocalObjectPool(final SmartRecyclingSupplier<ObjectHolder<T>> globalPool) {
        localObjects = new LinkedList<>();
        borrowedObjects = new HashMap<>();
        this.globalPool = globalPool;
        reqReturnObjects = 0;
        thread = Thread.currentThread();
        lock = new ReentrantLock();
    }

    @Override
    public T get() throws ObjectCreationException,
            InterruptedException, TimeoutException {
        lock.lock();
        try {
            T object;
            ObjectHolder<T> objectHolder;
            do {
                if (localObjects.isEmpty()) {
                    objectHolder = globalPool.get(this);
                } else {
                    objectHolder = localObjects.remove();
                }
                object = objectHolder.borrowOrCreateObjectIfPossible();
            } while (object == null);
            borrowedObjects.put(object, objectHolder);
            return object;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void recycle(final T object, final Exception e) {
        lock.lock();
        try {
            ObjectHolder holder = borrowedObjects.remove(object);
            if (holder == null) {
                // not borrowed from here.
                globalPool.recycle(new ObjectHolder<>(object), this);
            } else {
                try {
                    holder.returnObject(object, e);
                } finally {
                    if (reqReturnObjects > 0) {
                        reqReturnObjects--;
                        globalPool.recycle(holder, this);
                    } else {
                        localObjects.add(holder);
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void dispose() throws ObjectDisposeException {
        throw new UnsupportedOperationException("LocalPool dispose is not supported");
    }

    @Override
    public boolean scan(final ScanHandler<ObjectHolder<T>> handler) throws Exception {
        lock.lock();
        try {
            for (ObjectHolder<T> object : localObjects) {
                if (!handler.handle(object)) {
                    return false;
                }
            }
            return true;
        } finally {
            lock.unlock();
        }
    }


    private static final  Either<Action, ObjectHolder<?>> REQ_MADE = Either.left(Action.REQUEST_MADE);
    private static final  Either<Action, ObjectHolder<?>> NONE = Either.left(Action.NONE);

    @Override
    public Either<Action, ObjectHolder<T>> tryRequestReturnObject() throws InterruptedException {
        boolean acquired = lock.tryLock(0, TimeUnit.SECONDS);
        if (acquired) {
            try {

                ObjectHolder<T> objectHolder = tryReturnObjectIfNotInUse();
                if (objectHolder != null) {
                    return Either.right(objectHolder);
                } else if (this.thread == Thread.currentThread()) {
                    // this will happen is the same thread is closing the pool.
                    // in this case whatever objects the thread is borrowing, we will return
                    // even if not officially returnes.
                    if (this.borrowedObjects.isEmpty()) {
                        return (Either) NONE;
                    } else {
                        Iterator<Map.Entry<T, ObjectHolder<T>>> it = this.borrowedObjects.entrySet().iterator();
                        Map.Entry<T, ObjectHolder<T>> next = it.next();
                        ObjectHolder<T> result = next.getValue();
                        result.returnObject(next.getKey(), null);
                        it.remove();
                        return Either.right(result);
                    }
                } else {
                    if (borrowedObjects.size() > reqReturnObjects) {
                        reqReturnObjects++;
                        return (Either) REQ_MADE;
                    } else {
                        return (Either) NONE;
                    }
                }
            } finally {
                lock.unlock();
            }
        } else {
            return (Either) NONE;
        }
    }

    @Override
    public ObjectHolder<T> tryReturnObjectIfNotInUse() throws InterruptedException {
        boolean acquired = lock.tryLock(0, TimeUnit.SECONDS);
        if (acquired) {
            try {
                if (!localObjects.isEmpty()) {
                    return localObjects.remove();
                } else {
                    return null;
                }
            } finally {
                lock.unlock();
            }
        } else {
            return null;
        }
    }


    @Override
    public Collection<ObjectHolder<T>> tryReturnObjectsIfNotInUse() throws InterruptedException {
        boolean acquired = lock.tryLock(0, TimeUnit.SECONDS);
        if (acquired) {
            try {
                if (!localObjects.isEmpty()) {
                    Collection<ObjectHolder<T>> result = new ArrayList<>(localObjects);
                    localObjects.clear();
                    return result;
                } else {
                    return null;
                }
            } finally {
                lock.unlock();
            }
        } else {
            return null;
        }
    }


    @Override
    public Collection<ObjectHolder<T>> tryReturnObjectsIfNotNeededAnymore() throws InterruptedException {
        boolean acquired = lock.tryLock(0, TimeUnit.SECONDS);
        if (acquired) {
            try {
                if (!thread.isAlive()) {
                    if (!borrowedObjects.isEmpty()) {
                        throw new IllegalStateException("Objects not returned by dead thread: " + borrowedObjects);
                    }
                    return localObjects;
                }
                return null;
            } finally {
                lock.unlock();
            }
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "LocalObjectPool{" + "localObjects=" + localObjects + ", borrowedObjects="
                + borrowedObjects + ", reqReturnObjects=" + reqReturnObjects + ", thread=" + thread + '}';
    }

    @Override
    public void recycle(final T object) {
        recycle(object, null);
    }

    @Override
    public boolean nevermind(final ObjectHolder<T> object) {
        lock.lock();
        try {
            return borrowedObjects.remove(object.getObj()) != null;
        } finally {
            lock.unlock();
        }
    }
}
