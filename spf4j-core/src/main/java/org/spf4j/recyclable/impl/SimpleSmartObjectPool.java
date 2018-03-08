/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
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
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import javax.annotation.CheckReturnValue;
import org.spf4j.base.Either;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.Pair;
import org.spf4j.base.Throwables;
import org.spf4j.base.TimeSource;
import org.spf4j.recyclable.ObjectBorower;
import org.spf4j.recyclable.ObjectBorower.Action;
import org.spf4j.recyclable.ObjectCreationException;
import org.spf4j.recyclable.ObjectDisposeException;
import org.spf4j.recyclable.RecyclingSupplier;
import org.spf4j.recyclable.SmartRecyclingSupplier;

/**
 * @author zoly
 */
final class SimpleSmartObjectPool<T> implements SmartRecyclingSupplier<T> {

  private int maxSize;
  private int waitingForReturn;
  private final LinkedHashMultimap<ObjectBorower<T>, T> borrowedObjects;
  private final List<T> availableObjects;
  private final ReentrantLock lock;
  private final Condition available;
  private final RecyclingSupplier.Factory<T> factory;
  private T sample;

  SimpleSmartObjectPool(final int initialSize, final int maxSize,
          final RecyclingSupplier.Factory<T> factory, final boolean fair)
          throws ObjectCreationException {
    if (maxSize < 1) {
      throw new IllegalArgumentException("Invalid pool size: " + maxSize);
    }
    this.maxSize = maxSize;
    this.factory = factory;
    this.lock = new ReentrantLock(fair);
    this.available = this.lock.newCondition();
    this.borrowedObjects = LinkedHashMultimap.create();
    this.availableObjects = new ArrayList<>(initialSize);
    for (int i = 0; i < initialSize; i++) {
      availableObjects.add(factory.create());
    }
    waitingForReturn = 0;
    this.sample = factory.create();
  }

  @Override
  @SuppressFBWarnings({"RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", "PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS"})
  public T get(final ObjectBorower borower) throws InterruptedException,
          TimeoutException, ObjectCreationException {
    lock.lock();
    try {
      if (maxSize <= 0) {
        throw new IllegalStateException("Pool closed, noting available for " + borower);
      }
      int nrAvailable = availableObjects.size();
      // trying to be fair here, if others are already waiting, we will not get one.
      if (nrAvailable - waitingForReturn > 0) {
        Iterator<T> it = availableObjects.iterator();
        T object = it.next();
        it.remove();
        if (!borrowedObjects.put(borower, object)) {
          throw new IllegalStateException("Cannot borrow " + object + ", " + borrowedObjects);
        }
        return object;
      } else if (borrowedObjects.size()  + nrAvailable < maxSize) {
        T object = factory.create();
        if (!borrowedObjects.put(borower, object)) {
          throw new IllegalStateException("Cannot borrow " + object + ", " + borrowedObjects);
        }
        return object;
      } else {
        // try to reclaim object from borrowers
        for (ObjectBorower<T> b : borrowedObjects.keySet()) {
          if (borower != b) {
            T object = b.tryReturnObjectIfNotInUse();
            if (object != null) {
              if (!borrowedObjects.remove(b, object)) {
                throw new IllegalStateException("Returned Object hasn't been borrowed " + object);
              }
              if (!borrowedObjects.put(borower, object)) {
                throw new IllegalStateException("Cannot borrow " + object + ", " + borrowedObjects);
              }
              return object;
            }
          }
        }
        // evrything in use, try to place a return request
        boolean requestMade = false;
        while (!requestMade) {
          boolean hasValidBorowers = false;
          for (ObjectBorower<T> b : borrowedObjects.keySet()) {
            if (borower != b) {
              hasValidBorowers = true;
              Either<ObjectBorower.Action, T> objOrPromise = b.tryRequestReturnObject();
              if (objOrPromise.isRight()) {
                T obj = objOrPromise.getRight();
                if (!borrowedObjects.remove(b, obj)) {
                  throw new IllegalStateException("Returned Object " + obj
                          + " hasn't been borrowed: " + borrowedObjects);
                }

                if (!borrowedObjects.put(borower, obj)) {
                  throw new IllegalStateException("Cannot boroow " + obj + ", " + borrowedObjects);
                }
                return obj;
              } else {
                requestMade = objOrPromise.getLeft() == Action.REQUEST_MADE;
                if (requestMade) {
                  break;
                }
              }
            }
          }
          if (!hasValidBorowers) {
            throw new IllegalStateException("Borrower asks for more than possible " + borower);
          }
          if (!requestMade) {
            // probably was unable to acquire the locks
            do {
              available.await(1, TimeUnit.MILLISECONDS);
              long millisToDeadline = ExecutionContexts.getTimeRelativeToDeadline(TimeUnit.MILLISECONDS);
              if (millisToDeadline < 0) {
                throw new TimeoutException("Object wait timeout expired by " + (-millisToDeadline));
              }
            } while (borrowedObjects.isEmpty());
          }
        }
        waitingForReturn++;
        while (availableObjects.isEmpty()) {
          long waitTime = ExecutionContexts.getTimeToDeadline(TimeUnit.MILLISECONDS);
          if (!available.await(waitTime, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException("Object wait timeout expired, waitTime = " + waitTime);
          }
        }
        waitingForReturn--;
        Iterator<T> it = availableObjects.iterator();
        T objectT = it.next();
        it.remove();
        if (!borrowedObjects.put(borower, objectT)) {
          throw new IllegalStateException("Cannot borrow " + objectT + ", " + borrowedObjects);
        }
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
        // returned somebody else's objOrPromise.
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
          if (!borrowedObjects.remove(foundEntry.getKey(), foundEntry.getValue())) {
            throw new IllegalStateException("Should have removed " + foundEntry);
          }
        }
      }
      availableObjects.add(object);
      available.signalAll();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean tryDispose(final long timeoutMillis) throws ObjectDisposeException, InterruptedException {
    factory.dispose(sample);
    long deadlineNanos = TimeSource.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
    lock.lock();
    try {
      maxSize = 0;
      List<Pair<ObjectBorower<T>, T>> returnedObjects = new ArrayList<>();
      for (Entry<ObjectBorower<T>, Collection<T>> b : borrowedObjects.asMap().entrySet()) {
        ObjectBorower<T> borrower = b.getKey();
        final int nrObjects = b.getValue().size();
        for (int i = 0; i < nrObjects; i++) {
          Either<Action, T> object = borrower.tryRequestReturnObject();
          if (object.isRight()) {
            returnedObjects.add(Pair.of(borrower, object.getRight()));
          }
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
        long waitTimeNanos = deadlineNanos - TimeSource.nanoTime();
        if (waitTimeNanos <= 0) {
          return false;
        }
        if (!available.await(waitTimeNanos, TimeUnit.NANOSECONDS)) {
          return false;
        }
        exception = disposeReturnedObjects(exception);
      }
      if (exception != null) {
        throw exception;
      }
      return true;
    } catch (InterruptedException | RuntimeException e) {
      throw e;
    } finally {
      lock.unlock();
    }
  }

  @CheckReturnValue
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

  @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_HAS_CHECKED")
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
        for (T ro : returned) {
          if (!borrowedObjects.remove(objectBorower, ro)) {
            throw new IllegalStateException("Object returned hasn't been borrowed" + ro);
          }
          availableObjects.add(ro);
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

  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
  public void requestReturnFromBorrowersIfNotInUse() throws InterruptedException {
    lock.lock();
    try {
      List<Pair<ObjectBorower<T>, T>> returnedObjects = new ArrayList<>();
      for (ObjectBorower<T> b : borrowedObjects.keySet()) {
        Collection<T> objects = b.tryReturnObjectsIfNotInUse();
        for (T object : objects) {
          returnedObjects.add(Pair.of(b, object));
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
              + borrowedObjects + ", availableObjects=" + availableObjects
              + ", factory=" + factory + '}';
    } finally {
      lock.unlock();
    }
  }

  @Override
  public T getSample() {
    return sample;
  }
}
