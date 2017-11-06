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
package org.spf4j.recyclable;

import com.google.common.util.concurrent.UncheckedExecutionException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.TimeoutException;
import org.spf4j.base.Callables;
import org.spf4j.base.Callables.TimeoutCallable;
import org.spf4j.base.Handler;
import org.spf4j.base.Throwables;

//CHECKSTYLE IGNORE RedundantThrows FOR NEXT 2000 LINES
public final class Template<T, E extends Exception> {

    private final RecyclingSupplier<T> pool;
    private final int nrImmediateRetries;
    private final int retryWaitMillis;
    private final int timeout;
    private final Class<E> exClass;

    public Template(final RecyclingSupplier<T> pool, final int nrImmediateRetries,
             final int retryWaitMillis, final int timeoutMillis, final Class<E> exClass) {
        this.pool = pool;
        this.nrImmediateRetries = nrImmediateRetries;
        this.retryWaitMillis = retryWaitMillis;
        this.timeout = timeoutMillis;
        this.exClass = exClass;
    }

    public void doOnSupplied(final Handler<T, E> handler)
            throws InterruptedException, E, TimeoutException {
        doOnSupplied(handler, pool, nrImmediateRetries, retryWaitMillis, timeout, exClass);
    }

    public static  <T, E extends Exception> void doOnSupplied(final Handler<T, E> handler,
            final RecyclingSupplier<T> pool, final int nrImmediateRetries,
             final int retryWaitMillis, final int timeoutMillis, final Class<E> exClass)
            throws E, InterruptedException, TimeoutException {
        Callables.executeWithRetry(new TimeoutCallable<Void, E>(timeoutMillis) {

            @Override
            // CHECKSTYLE IGNORE RedundantThrows FOR NEXT 100 LINES
            public Void call(final long deadline)
                    throws InterruptedException, TimeoutException, E {
                try {
                  Template.doOnSupplied(handler, pool, deadline, exClass);
                } catch (ObjectCreationException | ObjectBorrowException ex) {
                  throw new UncheckedExecutionException(ex);
                }
                return null;
            }
        }, nrImmediateRetries, retryWaitMillis, exClass);
    }

    //findbugs does not know about supress in spf4j
    @SuppressFBWarnings("LEST_LOST_EXCEPTION_STACK_TRACE")
    private static <T, E extends Exception> void doOnSupplied(final Handler<T, E> handler,
            final RecyclingSupplier<T> pool, final long deadline, final Class<E> exClass)
            throws  E, ObjectCreationException, ObjectBorrowException, InterruptedException, TimeoutException {
        T object = pool.get();
        try {
            handler.handle(object, deadline);
        }  catch (RuntimeException e) {
            try {
                pool.recycle(object, e);
            } catch (RuntimeException ex) {
                throw Throwables.suppress(ex, e);
            }
            throw e;
        } catch (Exception e) {
            try {
                pool.recycle(object, e);
            } catch (RuntimeException ex) {
                throw Throwables.suppress(ex, e);
            }
            if (exClass.isAssignableFrom(e.getClass())) {
                throw (E) e;
            } else {
                throw new UncheckedExecutionException(e);
            }
        }
        pool.recycle(object, null);
    }

    @Override
    public String toString() {
        return "Template{" + "pool=" + pool + ", nrImmediateRetries=" + nrImmediateRetries
                + ", retryWaitMillis=" + retryWaitMillis + ", timeout=" + timeout + '}';
    }



}
