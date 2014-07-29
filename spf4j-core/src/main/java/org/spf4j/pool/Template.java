
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

import java.util.concurrent.TimeoutException;
import org.spf4j.base.Callables;
import org.spf4j.base.Callables.TimeoutCallable;
import org.spf4j.base.Handler;
import org.spf4j.base.Throwables;

public final class Template<T, E extends Exception> {

    private final ObjectPool<T> pool;
    private final int nrImmediateRetries;
    private final int retryWaitMillis;
    private final int timeout;

    public Template(final ObjectPool<T> pool, final int nrImmediateRetries,
             final int retryWaitMillis, final int timeout) {
        this.pool = pool;
        this.nrImmediateRetries = nrImmediateRetries;
        this.retryWaitMillis = retryWaitMillis;
        this.timeout = timeout;
    }

    // CHECKSTYLE IGNORE RedundantThrows FOR NEXT 100 LINES
    public void doOnPooledObject(final Handler<T, E> handler)
            throws ObjectCreationException, InterruptedException, TimeoutException {
        Callables.executeWithRetry(new TimeoutCallable<Void>(timeout) {

            @Override
            public Void call(final long deadline)
                    throws ObjectReturnException, ObjectCreationException,
                    ObjectBorrowException, InterruptedException, TimeoutException, E {
                doOnPooledObject(handler, pool, deadline);
                return null;
            }
        }, nrImmediateRetries, retryWaitMillis);

    }

    //findbugs does not know about supress in spf4j
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("LEST_LOST_EXCEPTION_STACK_TRACE")
    public static <T, E extends Exception> void doOnPooledObject(final Handler<T, E> handler,
            final ObjectPool<T> pool, final long deadline)
            throws ObjectReturnException, ObjectCreationException,
            ObjectBorrowException, InterruptedException, TimeoutException, E {
        T object = pool.borrowObject();
        try {
            handler.handle(object, deadline);
        } catch (Exception e) {
            try {
                pool.returnObject(object, e);
            } catch (RuntimeException ex) {
                throw Throwables.suppress(ex, e);
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw (E) e;
            }
        }
        pool.returnObject(object, null);
    }
}
