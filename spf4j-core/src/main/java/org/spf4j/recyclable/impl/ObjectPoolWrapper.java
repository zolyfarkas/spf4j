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

import org.spf4j.base.Throwables;
import org.spf4j.recyclable.ObjectBorrowException;
import org.spf4j.recyclable.ObjectCreationException;
import org.spf4j.recyclable.ObjectDisposeException;
import org.spf4j.recyclable.RecyclingSupplier;
import org.spf4j.recyclable.Scanable;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Handler;

/**
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
final class ObjectPoolWrapper<T> implements RecyclingSupplier<T>, Scanable<ObjectHolder<T>> {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectPoolWrapper.class);

    private final RecyclingSupplier<T> pool;

    private final Handler<T, ? extends Exception> borrowHook;

    private final Handler<T, ? extends Exception> returnHook;

    ObjectPoolWrapper(final RecyclingSupplier<T> pool,
            @Nullable final Handler<T, ? extends Exception> borrowHook,
            @Nullable final Handler<T, ? extends Exception> returnHook) {
        this.pool = pool;
        this.borrowHook = borrowHook;
        this.returnHook = returnHook;
        if (borrowHook == null && returnHook == null) {
            throw new IllegalArgumentException("Both hooks can't be null for " + pool);
        }
    }



    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("LEST_LOST_EXCEPTION_STACK_TRACE")
    @Override
    public T get()
            throws ObjectCreationException, ObjectBorrowException, InterruptedException, TimeoutException {
        T result = pool.get();
        try {
            if (borrowHook != null) {
                borrowHook.handle(result, Long.MAX_VALUE);
            }
            return result;
        } catch (Exception e) {
            try {
                pool.recycle(result, e);
            } catch (RuntimeException ex) {
                throw Throwables.suppress(ex, e);
            }
            throw new ObjectBorrowException("Exception while executing borrow hook " + borrowHook, e);
        }
    }

    @Override
    public void recycle(final T object, final Exception e) {
        try {
            if (returnHook != null) {
                returnHook.handle(object, Long.MAX_VALUE);
            }
        } catch (Exception ex) {
            LOG.error("Error while handling object {} ", object, ex);
        } finally {
            pool.recycle(object, e);
        }
    }

    @Override
    public boolean tryDispose(final long timeoutMillis) throws ObjectDisposeException, InterruptedException {
        return pool.tryDispose(timeoutMillis);
    }

    @Override
    public boolean scan(final ScanHandler<ObjectHolder<T>> handler) throws Exception {
        if (pool instanceof Scanable) {
            return ((Scanable<ObjectHolder<T>>) pool).scan(handler);
        } else {
            throw new UnsupportedOperationException("Wrapped pool " + pool + " is not scanable");
        }
    }

    @Override
    public void recycle(final T object) {
        pool.recycle(object);
    }

    @Override
    public String toString() {
        return "ObjectPoolWrapper{" + "pool=" + pool + ", borrowHook="
                + borrowHook + ", returnHook=" + returnHook + '}';
    }



}
