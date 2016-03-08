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
package org.spf4j.zel.vm;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.ThreadSafe;
import org.spf4j.base.Either;

/**
 * bean like implementation of a future
 * @author zoly
 */
@ThreadSafe
public class VMASyncFuture<T> implements VMFuture<T> {
    private volatile Either<T, ? extends ExecutionException> resultStore;

    @Override
    public final boolean cancel(final boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean isDone() {
        return resultStore != null;
    }
    
    @Override
    public final Either<T, ? extends ExecutionException> getResultStore() {
        return resultStore;
    }

    @Override
    // Findbugs complain here is rubbish, InterruptedException is thrown by wait
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("BED_BOGUS_EXCEPTION_DECLARATION")
    public final T get() throws InterruptedException, ExecutionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public final T get(final long timeout, final TimeUnit unit) {
        throw new UnsupportedOperationException();
    }


    @Override
    public final void setResult(final T result) {
        resultStore = Either.left(result);
    }
    
    @Override
    public final void setExceptionResult(final ExecutionException result) {
        if (result.getCause() == ExecAbortException.INSTANCE) {
            return;
        }
        resultStore = Either.right(result);
    }

    @Override
    public final String toString() {
        return "VMASyncFuture{" + "resultStore=" + resultStore + '}';
    }
    
}
