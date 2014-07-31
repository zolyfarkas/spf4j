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
package org.spf4j.base;

import com.google.common.base.Predicate;
import java.io.IOException;
import java.sql.SQLTransientException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for executing stuff with retry logic.
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class Callables {

    private Callables() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(Callables.class);

    public static final Predicate<Object> NORETRY_FOR_RESULT = new Predicate<Object>() {
        @Override
        public boolean apply(final Object input) {
            return false;
        }
    };

    public static final Predicate<?> RETRY_FOR_NULL_RESULT = new Predicate<Object>() {
        @Override
        public boolean apply(final Object input) {
            return (input != null);
        }
    };

    public static final Predicate<Exception> DEFAULT_EXCEPTION_RETRY = new Predicate<Exception>() {
        @Override
        public boolean apply(@Nonnull final Exception input) {
            Throwable rootCause = com.google.common.base.Throwables.getRootCause(input);
            if (rootCause instanceof RuntimeException) {
                return false;
            }
            if (rootCause instanceof SQLTransientException
                    || rootCause instanceof IOException
                    || rootCause instanceof TimeoutException) {
                LOG.debug("Exception encountered, retrying...", input);
                return true;
            }
            return false;
        }
    };

    public static final class RetryPauseWithTimeout<T> implements PreRetryCallback<T> {

        private final int nrImmediateRetries;
        private final int maxWaitMillis;
        private final TimeoutCallable callable;
        private int count;
        private final PreRetryCallback<T> doBeforeRetry;
        private IntMath.XorShift32 random;
        private int prevWaitMillis;
        private int waitMillis;

        public RetryPauseWithTimeout(final int nrImmediateRetries,
                final int maxRetryWaitMillis, final TimeoutCallable callable,
                @Nullable final PreRetryCallback<T> doBeforeRetry) {
            this.nrImmediateRetries = nrImmediateRetries;
            this.maxWaitMillis = maxRetryWaitMillis;
            this.callable = callable;
            this.doBeforeRetry = doBeforeRetry;
            this.prevWaitMillis = 0;
            this.waitMillis = 1;
        }

        @Override
        public boolean call(final Exception lastException, final T lastResult) throws InterruptedException {
            long now = System.currentTimeMillis();
            if (now >= callable.getDeadline()) {
                return false;
            }
            if (count >= nrImmediateRetries) {
                if (random == null) {
                    // This might belong in a thread local.
                    random = new IntMath.XorShift32();
                }
                Thread.sleep(waitMillis - Math.abs(random.nextInt() % waitMillis));
                if (waitMillis < maxWaitMillis) {
                    int tmp = waitMillis;
                    waitMillis = waitMillis + prevWaitMillis;
                    prevWaitMillis = tmp;
                }
                if (waitMillis > maxWaitMillis) {
                    waitMillis = maxWaitMillis;
                }
            } else {
                count++;
            }
            if (doBeforeRetry != null) {
                return doBeforeRetry.call(lastException, lastResult);
            }
            return true;
        }

    }


    public static <T> T executeWithRetry(final TimeoutCallable<T> what, final int nrImmediateRetries,
            final int maxRetryWaitMillis)
            throws InterruptedException {
        return executeWithRetry(what, new RetryPauseWithTimeout<T>(
                nrImmediateRetries, maxRetryWaitMillis, what, null),
                NORETRY_FOR_RESULT, DEFAULT_EXCEPTION_RETRY);
    }

    public static <T> T executeWithRetry(final TimeoutCallable<T> what, final int nrImmediateRetries,
            final int maxRetryWaitMillis,
            final Predicate<Exception> retryOnException)
            throws InterruptedException {
        return executeWithRetry(what, new RetryPauseWithTimeout<T>(
                nrImmediateRetries, maxRetryWaitMillis, what, null),
                NORETRY_FOR_RESULT, retryOnException);
    }

    public static <T> T executeWithRetry(final TimeoutCallable<T> what, final int nrImmediateRetries,
            final int maxRetryWaitMillis,
            final Predicate<? super T> retryOnReturnVal, final Predicate<Exception> retryOnException)
            throws InterruptedException {
        return executeWithRetry(what, new RetryPauseWithTimeout<T>(
                nrImmediateRetries, maxRetryWaitMillis, what, null),
                retryOnReturnVal, retryOnException);
    }

    /**
     * After the immediate retries are done,
     * delayed retry with randomized Fibonacci values up to the specified max is executed.
     * @param <T>
     * @param what
     * @param doBeforeRetry
     * @param nrImmediateRetries
     * @param maxWaitMillis
     * @param retryOnReturnVal
     * @param retryOnException
     * @return
     * @throws InterruptedException
     */
    public static <T> T executeWithRetry(final TimeoutCallable<T> what, final PreRetryCallback<T> doBeforeRetry,
            final int nrImmediateRetries, final int maxWaitMillis,
            final Predicate<? super T> retryOnReturnVal, final Predicate<Exception> retryOnException)
            throws InterruptedException {
        return executeWithRetry(what, new RetryPauseWithTimeout<T>(
                nrImmediateRetries, maxWaitMillis, what, doBeforeRetry),
                retryOnReturnVal, retryOnException);
    }

    public abstract static class TimeoutCallable<T> implements Callable<T> {

        private final long mdeadline;

        public TimeoutCallable(final int timeoutMillis) {
            mdeadline = System.currentTimeMillis() + timeoutMillis;
        }

        @Override
        public final T call() throws Exception {
            return call(mdeadline);
        }

        public abstract T call(final long deadline) throws Exception;

        public final long getDeadline() {
            return mdeadline;
        }

    }

    public static <T> T executeWithRetry(final Callable<T> what, final Callable<Boolean> doBeforeRetry,
            final Predicate<? super T> retryOnReturnVal, final Predicate<Exception> retryOnException)
            throws InterruptedException {
        return executeWithRetry(what, (PreRetryCallback<T>) callableToPreRetryCallback(doBeforeRetry),
                retryOnReturnVal, retryOnException);
    }

    public static <T> PreRetryCallback<T> callableToPreRetryCallback(final Callable<Boolean> doBeforeRetry) {
        return new PreRetryCallback<T>() {

            @Override
            public boolean call(final Exception lastException, final T lastResult) throws InterruptedException {
                try {
                    return doBeforeRetry.call();
                } catch (InterruptedException ex) {
                    throw ex;
                }  catch (RuntimeException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }

    public interface PreRetryCallback<T> {

        /**
         * This is executed before doing a retry. any delay, or other retry logic is done here.
         *
         * @param lastException - last exception encountered, if null, retry does not happen because of exception.
         * @param lastResult - if lastException is null this value represents the last returned result.
         * @return
         * @throws InterruptedException
         */
        boolean call(Exception lastException, T lastResult)
                throws InterruptedException;

    }

    /**
     * Naive implementation of execution with retry logic. a callable will be executed and retry attempted in current
     * thread if the result and exception predicates. before retry, a callable can be executed that can abort the retry
     * and finish the function with the previous result.
     *
     * @param what
     * @param doBeforeRetry
     * @param retryOnReturnVal
     * @param retryOnException
     * @return
     * @throws InterruptedException
     */
    public static <T> T executeWithRetry(final Callable<T> what, final PreRetryCallback<T> doBeforeRetry,
            final Predicate<? super T> retryOnReturnVal, final Predicate<Exception> retryOnException)
            throws InterruptedException {
        T result = null;
        Exception lastEx = null; // last exception
        try {
            result = what.call();
        } catch (InterruptedException ex1) {
            throw ex1;
        } catch (Exception e) {
            lastEx = e;
        }
        Exception lastExChain = lastEx; // last exception chained with all previous exceptions
        while ((lastEx != null && retryOnException.apply(lastEx)) || retryOnReturnVal.apply(result)) {
            if (Thread.interrupted()) {
                Thread.currentThread().interrupt();
                throw new InterruptedException();
            }
            boolean retry = doBeforeRetry.call(lastEx, result);
            if (!retry) {
                break;
            }

            result = null;
            lastEx = null;
            try {
                result = what.call();
            } catch (InterruptedException ex1) {
                throw ex1;
            } catch (Exception e) {
                lastEx = e;
                if (lastExChain != null) {
                    lastExChain = Throwables.suppress(e, lastExChain);
                } else {
                    lastExChain = e;
                }
            }
        }
        if (lastEx != null) {
            if (lastExChain instanceof RuntimeException) {
                throw (RuntimeException) lastExChain;
            } else {
                throw new RuntimeException(lastExChain);
            }
        }
        return result;
    }

    public static <T> Callable<T> synchronize(final Callable<T> callable) {
        return new Callable<T>() {

            @Override
            public synchronized T call() throws Exception {
                return callable.call();
            }
        };
    }

}
