
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
public class VMExecutor {

    public static class SuspendedException extends Exception {

        private final FutureBean<Object> suspendedAt;

        public SuspendedException(FutureBean<Object> suspendedAt) {
            this.suspendedAt = suspendedAt;
        }

        public FutureBean<Object> getSuspendedAt() {
            return suspendedAt;
        }

        @Override
        public Throwable fillInStackTrace() {
            return this;
        }

    }

    public interface Suspendable<T> extends Callable<T> {

        T call() throws SuspendedException, ZExecutionException, InterruptedException;

    }

    private final Executor exec;

    public VMExecutor(Executor exec) {
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

        List<Pair<Suspendable<Object>, FutureBean<Object>>> suspended;
        if (!futToSuspMap.containsKey(futureSuspendedFor)) {
            suspended = new LinkedList<Pair<Suspendable<Object>, FutureBean<Object>>>();
            List<Pair<Suspendable<Object>, FutureBean<Object>>> old =
                    futToSuspMap.putIfAbsent(futureSuspendedFor, suspended);
            if (old != null) {
                suspended = old;
            }
        } else {
            suspended = futToSuspMap.get(futureSuspendedFor);
        }
        do {
            List<Pair<Suspendable<Object>, FutureBean<Object>>> newList
                    = new LinkedList<Pair<Suspendable<Object>, FutureBean<Object>>>(suspended);
            newList.add(Pair.of(suspendedCallable, suspendedCallableFuture));
            if (futToSuspMap.replace(futureSuspendedFor, suspended, newList)) {
                break;
            } else {
                suspended = futToSuspMap.get(futureSuspendedFor);
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
