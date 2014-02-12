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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import org.spf4j.base.Pair;
import org.spf4j.concurrent.FutureBean;

/**
 *
 * @author zoly
 */
public final class VMExecutor {

    public interface Suspendable<T> extends Callable<T> {

        @Override
        T call() throws SuspendedException, ZExecutionException, InterruptedException;

    }

    private final Executor exec;

    public VMExecutor(final Executor exec) {
        this.exec = exec;
    }

    public <T> Future<T> submit(final Suspendable<T> callable) {
        final FutureBean<T> resultFuture = new FutureBean<T>();
        submit(callable, resultFuture);
        return resultFuture;
    }

    private final ConcurrentMap<FutureBean<Object>, List<Pair<Suspendable<Object>, FutureBean<Object>>>> futToSuspMap
            = new ConcurrentHashMap<FutureBean<Object>, List<Pair<Suspendable<Object>, FutureBean<Object>>>>();

    private void resumeSuspendables(final FutureBean<Object> future) {
        List<Pair<Suspendable<Object>, FutureBean<Object>>> suspended = futToSuspMap.remove(future);
        if (suspended != null) {
            for (Pair<Suspendable<Object>, FutureBean<Object>> susp : suspended) {
                submit(susp.getFirst(), susp.getSecond());
            }
        }
    }

    private void addSuspendable(final FutureBean<Object> futureSuspendedFor,
            final Suspendable<Object> suspendedCallable, final FutureBean<Object> suspendedCallableFuture) {

        List<Pair<Suspendable<Object>, FutureBean<Object>>> suspended
                = futToSuspMap.get(futureSuspendedFor);
        if (suspended == null) {
            suspended = new LinkedList<Pair<Suspendable<Object>, FutureBean<Object>>>();
            List<Pair<Suspendable<Object>, FutureBean<Object>>> old
                    = futToSuspMap.putIfAbsent(futureSuspendedFor, suspended);
            if (old != null) {
                suspended = old;
            }
        }
        do {
            List<Pair<Suspendable<Object>, FutureBean<Object>>> newList
                    = new LinkedList<Pair<Suspendable<Object>, FutureBean<Object>>>(suspended);
            newList.add(Pair.of(suspendedCallable, suspendedCallableFuture));
            if (futToSuspMap.replace(futureSuspendedFor, suspended, newList)) {
                break;
            } else {
                suspended = futToSuspMap.get(futureSuspendedFor);
                if (suspended == null) {
                    suspended = new LinkedList<Pair<Suspendable<Object>, FutureBean<Object>>>();
                    List<Pair<Suspendable<Object>, FutureBean<Object>>> old
                            = futToSuspMap.putIfAbsent(futureSuspendedFor, suspended);
                    if (old != null) {
                        suspended = old;
                    }
                }
            }
        } while (true);
        if (futureSuspendedFor.isDone()) {
            resumeSuspendables(futureSuspendedFor);
        }
    }

    private <T> void submit(final Suspendable<T> callable, final FutureBean<T> future) {
        exec.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    T result = callable.call();
                    future.setResult(result);
                    resumeSuspendables((FutureBean<Object>) future);
                } catch (SuspendedException ex) {
                    addSuspendable((FutureBean<Object>) ex.getSuspendedAt(),
                            (Suspendable<Object>) callable, (FutureBean<Object>) future);
                } catch (Exception e) {
                    future.setExceptionResult(new ExecutionException(e));
                    resumeSuspendables((FutureBean<Object>) future);
                }
            }
        });
    }
    
    
    
    

}
