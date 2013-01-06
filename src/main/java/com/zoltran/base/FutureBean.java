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
package com.zoltran.base;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * bean like implementation of a future
 * @author zoly
 */
@ThreadSafe
public class FutureBean<T> implements Future<T> {
    private Pair<Object, ? extends ExecutionException> resultStore;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public synchronized boolean isDone() {
        return resultStore != null;
    }

    @Override
    public synchronized T get() throws InterruptedException, ExecutionException {
        while (resultStore == null) {
            this.wait();
        }     
        return processResult(resultStore);
    }

    @Override
    public synchronized T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        while (resultStore == null) {
            this.wait(unit.toMillis(timeout));
        } 
        return processResult(resultStore);
    }

    private T processResult(Pair<Object, ? extends ExecutionException> result) throws ExecutionException {
        ExecutionException e = result.getSecond();
        if (e != null) {
            throw e;
        } else {
            return (T) result.getFirst();
        }
    }

    public synchronized void setResult(Object result) {
        resultStore = new Pair<Object, ExecutionException>(result, (ExecutionException) null);
        this.notifyAll();
    }

    public synchronized void setExceptionResult(ExecutionException result) {
        resultStore = new Pair<Object, ExecutionException>(null, (ExecutionException) result);
        this.notifyAll();
    }
    
}
