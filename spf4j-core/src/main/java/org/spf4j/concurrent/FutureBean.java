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
package org.spf4j.concurrent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.concurrent.ThreadSafe;
import org.spf4j.base.Either;
import org.spf4j.base.TimeSource;

/**
 * bean like implementation of a future
 * @author zoly
 */
@ThreadSafe
@SuppressFBWarnings("NOS_NON_OWNED_SYNCHRONIZATION")
public class FutureBean<T> implements Future<T> {

    private volatile Either<T, ? extends Exception> resultStore;

    private volatile boolean isCanceled;


    @Override
    public final boolean cancel(final boolean mayInterruptIfRunning) {
      if (isDone()) {
        return false;
      } else {
        isCanceled = true;
        return true;
      }
    }

    @Override
    public final boolean isCancelled() {
       return isCanceled;
    }

    @Override
    public final boolean isDone() {
        return isCanceled || resultStore != null;
    }

    public final Either<T, ? extends Exception> getResultStore() {
        return resultStore;
    }

    @Override
    // Findbugs complain here is rubbish, InterruptedException is thrown by wait
    @SuppressFBWarnings({"BED_BOGUS_EXCEPTION_DECLARATION", "MDM_WAIT_WITHOUT_TIMEOUT" })
    public final synchronized T get() throws InterruptedException, ExecutionException {
        while (resultStore == null) {
            this.wait();
        }
        return processResult(resultStore);
    }

    @Override
    public final T get(final long timeout, final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        long timeoutMillis = unit.toMillis(timeout);
        long toWait = timeoutMillis;
        long startTime = TimeSource.nanoTime();
        synchronized (this) {
            while (toWait > 0 && resultStore == null) {
                this.wait(toWait);
                toWait = timeoutMillis - (TimeSource.nanoTime() - startTime);
            }
            if (resultStore == null) {
                throw new TimeoutException();
            }
            return processResult(resultStore);
        }
    }

    @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
    public static <T> T processResult(final Either<T, ? extends Exception> result)
            throws ExecutionException {
        if (result.isLeft()) {
            return result.getLeft();
        } else {
            Exception e = result.getRight();
            if (e instanceof ExecutionException) {
              throw (ExecutionException) e;
            } else if (e instanceof IllegalStateException) {
              throw (IllegalStateException) e;
            } else {
              throw new IllegalStateException(e);
            }
        }
    }

    public final synchronized void setResult(final T result) {
        if (resultStore != null) {
            throw new IllegalStateException("cannot set result multiple times " +  result);
        }
        resultStore = Either.left(result);
        done();
        this.notifyAll();
    }

    public final synchronized void setExceptionResult(final ExecutionException result) {
        if (resultStore != null) {
            throw new IllegalStateException("cannot set result multiple times " + result);
        }
        resultStore = Either.right(result);
        done();
        this.notifyAll();
    }

    public final synchronized void setCancelationResult(final CancellationException  result) {
        if (resultStore != null) {
            throw new IllegalStateException("cannot set result multiple times " + result);
        }
        resultStore = Either.right(result);
        done();
        this.notifyAll();
    }

    public void done() {
    }

    @Override
    public final String toString() {
        return "FutureBean{" + "resultStore=" + resultStore + '}';
    }

}
