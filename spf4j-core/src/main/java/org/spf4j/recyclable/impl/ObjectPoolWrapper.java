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
final class ObjectPoolWrapper<T> implements RecyclingSupplier<T> , Scanable<ObjectHolder<T>> {

    private final RecyclingSupplier<T> pool;
    
    private final Handler<T, ? extends Exception> borrowHook;
    
    private final Handler<T, ? extends Exception> returnHook;
    
    private static final Logger LOG = LoggerFactory.getLogger(ObjectPoolWrapper.class);

    public ObjectPoolWrapper(final RecyclingSupplier<T> pool,
            @Nullable final Handler<T, ? extends Exception> borrowHook,
            @Nullable final Handler<T, ? extends Exception> returnHook) {
        this.pool = pool;
        this.borrowHook = borrowHook;
        this.returnHook = returnHook;
        if (borrowHook == null && returnHook == null) {
            throw new IllegalArgumentException("Both hooks can't be null for " + pool);
        }
    }
    
    
    
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("LEST_LOST_EXCEPTION_STACK_TRACE")
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
                throw Throwables.suppress(new RuntimeException(ex), e);
            }
            throw new ObjectBorrowException("Exception while executing borrow hook", e);
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
    public void dispose() throws ObjectDisposeException, InterruptedException {
        pool.dispose();
    }

    @Override
    public boolean scan(final ScanHandler<ObjectHolder<T>> handler) throws Exception {
        if (pool instanceof Scanable) {
            return ((Scanable<ObjectHolder<T>>) pool).scan(handler);
        } else {
            throw new RuntimeException("Wrapped pool " + pool + " is not scanable");
        }
    }

    @Override
    public void recycle(final T object) {
        pool.recycle(object);
    }

  
    
}
