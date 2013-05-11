
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
package org.spf4j.pool;

import org.spf4j.base.Callables;
import org.spf4j.base.Throwables;
import java.lang.reflect.ParameterizedType;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

public final class Template<T, E extends Exception> {

    private final ObjectPool<T> pool;
    private final int nrImmediateRetries;
    private final int nrTotalRetries;
    private final int retryWaitMillis;
    private final Class<E> exceptionClasz;

    public Template(final ObjectPool<T> pool, final int nrImmediateRetries,
            final int nrTotalRetries, final int retryWaitMillis) {
        this.pool = pool;
        this.nrImmediateRetries = nrImmediateRetries;
        this.nrTotalRetries = nrTotalRetries;
        this.retryWaitMillis = retryWaitMillis;
        exceptionClasz = (Class<E>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0];
    }

    // CHECKSTYLE IGNORE RedundantThrows FOR NEXT 100 LINES
    public void doOnPooledObject(final ObjectPool.Handler<T, E> handler)
            throws ObjectCreationException, InterruptedException, TimeoutException {
        Callables.executeWithRetry(new Callable<Void>() {
            @Override
            public Void call() throws ObjectReturnException, ObjectDisposeException,
                    ObjectCreationException, ObjectBorrowException,
                    InterruptedException, TimeoutException, E  {
                doOnPooledObject(handler, pool, exceptionClasz);
                return null;
            }
        }, nrImmediateRetries, nrTotalRetries, retryWaitMillis);

    }

    public static <T, E extends Exception> void doOnPooledObject(final ObjectPool.Handler<T, E> handler,
            final ObjectPool<T> pool, final Class<E> clasz)
            throws ObjectReturnException, ObjectDisposeException, ObjectCreationException,
            ObjectBorrowException, InterruptedException, TimeoutException, E {
        T object = pool.borrowObject();
        try {
            handler.handle(object);
        } catch (Exception e) {
            try {
                pool.returnObject(object, e);
            } catch (ObjectReturnException ex) {
                throw Throwables.chain(ex, e);
            } catch (ObjectDisposeException ex) {
                throw Throwables.chain(ex, e);
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
        pool.returnObject(object, null);
    }
}
