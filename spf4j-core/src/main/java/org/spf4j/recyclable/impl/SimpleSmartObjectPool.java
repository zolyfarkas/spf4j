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

import com.google.common.collect.LinkedHashMultimap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.spf4j.base.Either;
import org.spf4j.base.Pair;
import org.spf4j.base.Throwables;
import org.spf4j.recyclable.ObjectBorower;
import org.spf4j.recyclable.ObjectBorower.Action;
import org.spf4j.recyclable.ObjectCreationException;
import org.spf4j.recyclable.ObjectDisposeException;
import org.spf4j.recyclable.RecyclingSupplier;
import org.spf4j.recyclable.SmartRecyclingSupplier;

/**
 *
 * @author zoly
 */
final class SimpleSmartObjectPool<T> implements SmartRecyclingSupplier<T> {

    private int maxSize;
    private final LinkedHashMultimap<ObjectBorower<T>, T> borrowedObjects;
    private final List<T> availableObjects;
    private final ReentrantLock lock;
    private final Condition available;
    private final RecyclingSupplier.Factory<T> factory;

    public SimpleSmartObjectPool(final int initialSize, final int maxSize,
        final  RecyclingSupplier.Factory<T> factory, final boolean fair)
            throws ObjectCreationException {
        this.maxSize = maxSize;
        this.factory = factory;
        this.lock = new ReentrantLock(fair);
        this.available = this.lock.newCondition();
        this.borrowedObjects = LinkedHashMultimap.create();
        this.availableObjects = new ArrayList<>(initialSize);
        for (int i = 0; i < initialSize; i++) {
            availableObjects.add(factory.create());
        }
    }

    @Override
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public T get(final ObjectBorower borower) throws InterruptedException,
            TimeoutException, ObjectCreationException {
        long deadline = org.spf4j.base.Runtime.DEADLINE.get();
        lock.lock();
        try {
            if (availableObjects.size() > 0) {
                Iterator<T> it = availableObjects.iterator();
                T object = it.next();
                it.remove();
                borrowedObjects.put(borower, object);
                return object;
            } else if (borrowedObjects.size() < maxSize) {
                T object = factory.create();
                borrowedObjects.put(borower, object);
                return object;
            } else {
                if (borrowedObjects.isEmpty()) {
                    throw new RuntimeException(
                            "Pool is probably closing down or is missconfigured withe size 0 " + this);
                }
                for (ObjectBorower<T> b : borrowedObjects.keySet()) {
                    if (borower != b) {
                        T object = b.tryReturnObjectIfNotInUse();
                        if (object != null) {
                            if (!borrowedObjects.remove(b, object)) {
                                throw new IllegalStateException("Returned Object hasn't been borrowed " + object);
                            }
                            borrowedObjects.put(borower, object);
                            return object;
                        }
                    }
                }
                Either<ObjectBorower.Action, T> object;
                boolean requestNotMade = true;
                do {
                    Iterator<ObjectBorower<T>> itt = borrowedObjects.keySet().iterator();
                    ObjectBorower<T> b = itt.next();
                    while (b == borower && itt.hasNext()) {
                        b = itt.next();
                    }
                    if (b == borower) {
                        throw new IllegalStateException("Borrower " + b + " already has "
                                + "max number of pool objects");
                    }
                    do {
                        object = b.tryRequestReturnObject();
                        if (object.isRight()) {
                            if (!borrowedObjects.remove(b, object)) {
                                throw new IllegalStateException("Returned Object " + object
                                        + "hasn't been borrowed: " + borrowedObjects);
                            }
                            borrowedObjects.put(borower, (T) object);
                            return  object.getRight();
                        }
                        //CHECKSTYLE:OFF -- inner assignement
                    } while (object.isLeft() && object.getLeft() != Action.REQUEST_MADE
                            && (itt.hasNext() && ((b = itt.next()) != null)));
                    //CHECKSTYLE:ON
                    // if request has not been made, do a wait and trya again.
                    requestNotMade = object.isLeft() && object.getLeft() != Action.REQUEST_MADE;
                    if (requestNotMade) {
                        do {
                            available.await(1, TimeUnit.MILLISECONDS);
                        } while (borrowedObjects.isEmpty());
                    }
                } while (requestNotMade);

                while (availableObjects.isEmpty()) {
                    long waitTime = deadline - System.currentTimeMillis();
                    if (waitTime <= 0) {
                        throw new TimeoutException("Object wait timeout expired, deadline = " + deadline);
                    }
                    if (!available.await(waitTime, TimeUnit.MILLISECONDS)) {
                        throw new TimeoutException("Object wait timeout expired, deadline = " + deadline);
                    }
                }
                Iterator<T> it = availableObjects.iterator();
                T objectT = it.next();
                it.remove();
                borrowedObjects.put(borower,  objectT);
                return objectT;

            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void recycle(final T object, final ObjectBorower borower) {
        lock.lock();
        try {
            if (!borrowedObjects.remove(borower, object)) {
                // returned somebody else's object.
                Entry<ObjectBorower<T>, T> foundEntry = null;
                for (Entry<ObjectBorower<T>, T> entry : borrowedObjects.entries()) {
                    final ObjectBorower<T> lb = entry.getKey();
                    if (lb == borower) {
                        continue;
                    }
                    if (lb.nevermind(entry.getValue())) {
                        foundEntry = entry;
                        break;
                    }
                }
                if (foundEntry == null) {
                    throw new IllegalStateException("Object " + object + " has not been borrowed from this pool");
                } else {
                    borrowedObjects.remove(foundEntry.getKey(), foundEntry.getValue());
                }
            }
            availableObjects.add(object);
            available.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void dispose() throws ObjectDisposeException, InterruptedException {
        long deadline = org.spf4j.base.Runtime.DEADLINE.get();
        lock.lock();
        try {
            maxSize = 0;
            List<Pair<ObjectBorower<T>, T>> returnedObjects = new ArrayList<>();
            for (ObjectBorower<T> b : borrowedObjects.keySet()) {
                Either<Action, T> object =  b.tryRequestReturnObject();
                if (object.isRight()) {
                    returnedObjects.add(Pair.of(b, object.getRight()));
                }
            }
            for (Pair<ObjectBorower<T>, T> objectAndBorrower : returnedObjects) {
                T object = objectAndBorrower.getSecond();
                if (!borrowedObjects.remove(objectAndBorrower.getFirst(), object)) {
                        throw new IllegalStateException("Returned Object hasn't been borrowed " + object);
                }
                availableObjects.add(object);
            }
            ObjectDisposeException exception = disposeReturnedObjects(null);
            while (!borrowedObjects.isEmpty()) {
                long waitTime = deadline - System.currentTimeMillis();
                if (waitTime <= 0) {
                        throw new TimeoutException("Object wait timeout expired, deadline = " + deadline);
                }
                if (!available.await(waitTime, TimeUnit.MILLISECONDS)) {
                    throw new TimeoutException("Object wait timeout expired, deadline = " + deadline);
                }
                disposeReturnedObjects(exception);
            }
            if (exception != null) {
                throw exception;
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (TimeoutException e) {
            throw new ObjectDisposeException(e);
        } catch (RuntimeException e) {
            throw e;
        } finally {
            lock.unlock();
        }
    }

    private ObjectDisposeException disposeReturnedObjects(final ObjectDisposeException exception) {
        ObjectDisposeException result = exception;
        for (T obj : availableObjects) {
            try {
                factory.dispose(obj);
            } catch (ObjectDisposeException ex) {
                if (result == null) {
                    result = ex;
                } else {
                    result = Throwables.suppress(ex, result);
                }
            } catch (Exception ex) {
                if (result == null) {
                    result = new ObjectDisposeException(ex);
                } else {
                    result = Throwables.suppress(new ObjectDisposeException(ex), result);
                }
            }
        }
        availableObjects.clear();
        return result;
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("EXS_EXCEPTION_SOFTENING_HAS_CHECKED")
    @Override
    public boolean scan(final ScanHandler<T> handler) throws Exception {
        lock.lock();
        Exception resEx = null;
        try {
            for (ObjectBorower<T> objectBorower : borrowedObjects.keySet()) {
                try {
                    if (!objectBorower.scan(handler)) {
                        return false;
                    }
                } catch (Exception e) {
                    if (resEx == null) {
                        resEx = e;
                    } else {
                        resEx = Throwables.suppress(e, resEx);
                    }
                }

                Collection<T> returned = objectBorower.tryReturnObjectsIfNotNeededAnymore();
                if (returned != null) {
                    for (T ro : returned) {
                        if (!borrowedObjects.remove(objectBorower, ro)) {
                            throw new IllegalStateException("Object returned hasn't been borrowed" + ro);
                        }
                        availableObjects.add(ro);
                    }
                }

            }
            for (T object : availableObjects) {
                try {
                    if (!handler.handle(object)) {
                        return false;
                    }
                } catch (Exception e) {
                    if (resEx == null) {
                        resEx = e;
                    } else {
                        resEx = Throwables.suppress(e, resEx);
                    }
                }
            }
            if (resEx != null) {
                throw resEx;
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void requestReturnFromBorrowersIfNotInUse() throws InterruptedException {
        lock.lock();
        try {
            List<Pair<ObjectBorower<T>, T>> returnedObjects = new ArrayList<Pair<ObjectBorower<T>, T>>();
            for (ObjectBorower<T> b : borrowedObjects.keySet()) {
                Collection<T> objects = b.tryReturnObjectsIfNotInUse();
                if (objects != null) {
                    for (T object : objects) {
                        returnedObjects.add(Pair.of(b, object));
                    }
                }
            }
            for (Pair<ObjectBorower<T>, T> ro : returnedObjects) {
                T object = ro.getSecond();
                borrowedObjects.remove(ro.getFirst(), object);
                availableObjects.add(object);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        lock.lock();
        try {
            return "SimpleSmartObjectPool{" + "maxSize=" + maxSize + ", borrowedObjects="
                    + borrowedObjects + ", returnedObjects=" + availableObjects
                    + ", factory=" + factory + '}';
        } finally {
            lock.unlock();
        }
    }
}
