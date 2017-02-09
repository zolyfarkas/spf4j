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

import com.google.common.annotations.Beta;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.base.Callables.Action;
import org.spf4j.base.Callables.AdvancedAction;
import org.spf4j.base.Callables.AdvancedRetryPredicate;
import static org.spf4j.base.Callables.DEFAULT_EXCEPTION_RETRY;

/**
 * Utility class for executing stuff with retry logic.
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
@Beta
//CHECKSTYLE IGNORE RedundantThrows FOR NEXT 2000 LINES
public final class CallablesNano {

    private CallablesNano() {
    }

    public static final RetryPredicate<?, RuntimeException> RETRY_FOR_NULL_RESULT =
            new RetryPredicate<Object, RuntimeException>() {
        @Override
        public Action apply(final Object input) {
            return (input != null) ? Action.ABORT : Action.RETRY;
        }
    };

    public static <T, EX extends Exception> T executeWithRetry(final TimeoutCallable<T, EX> what,
            final int nrImmediateRetries,
            final long maxRetryWaitNanos)
            throws InterruptedException, EX, TimeoutException {
        return executeWithRetry(what, nrImmediateRetries, maxRetryWaitNanos,
                TimeoutRetryPredicate.NORETRY_FOR_RESULT, DEFAULT_EXCEPTION_RETRY);
    }

    public static <T, EX extends Exception> T executeWithRetry(final TimeoutCallable<T, EX> what,
            final int nrImmediateRetries,
            final long maxRetryWaitNanos,
            final AdvancedRetryPredicate<Exception> retryOnException)
            throws InterruptedException, EX, TimeoutException {
        return executeWithRetry(what, nrImmediateRetries, maxRetryWaitNanos,
                TimeoutRetryPredicate.NORETRY_FOR_RESULT, retryOnException);
    }

    /**
     * After the immediate retries are done,
     * delayed retry with randomized Fibonacci values up to the specified max is executed.
     * @param <T> - the type returned by the Callable that is retried.
     * @param <EX> - the Exception thrown by the retried callable.
     * @param what - the callable to retry.
     * @param nrImmediateRetries - the number of immediate retries.
     * @param maxWaitNanos - maximum wait time in between retries.
     * @param retryOnReturnVal - predicate to control retry on return value;
     * @param retryOnException - predicate to retry on thrown exception.
     * @return the result of the callable.
     * @throws java.lang.InterruptedException - thrown if interrupted.
     * @throws EX - the exception declared to be thrown by the callable.
     */
    public static <T, EX extends Exception> T executeWithRetry(final TimeoutCallable<T, EX> what,
            final int nrImmediateRetries, final long maxWaitNanos,
            final TimeoutRetryPredicate<? super T> retryOnReturnVal,
            final AdvancedRetryPredicate<Exception> retryOnException)
            throws InterruptedException, EX, TimeoutException {
        return executeWithRetry(what, retryOnReturnVal,
                new FibonacciBackoffRetryPredicate<>(retryOnException, nrImmediateRetries,
                        maxWaitNanos / 100, maxWaitNanos, Callables.EX_TYPE_CLASS_MAPPER));
    }


    private static final class RetryData {

        private long immediateLeft;

        private long p1;

        private long p2;

        private final long maxDelayNanos;

        RetryData(final long immediateLeft, final long p1, final long maxDelayNanos) {
            this.immediateLeft = immediateLeft;
            if (p1 < 1) {
                this.p1 = 0;
                this.p2 = 1;
            } else {
                this.p1 = p1;
                this.p2 = p1;
            }
            this.maxDelayNanos = maxDelayNanos;
        }

        long nextDelayNanos() {
            if (immediateLeft > 0) {
                immediateLeft--;
                return 0;
            } else if (p2 > maxDelayNanos) {
                return maxDelayNanos;
            } else {
                long result = p2;
                p2 = p1 + p2;
                p1 = result;
                return result;
            }
        }

    }


    public static final class FibonacciBackoffRetryPredicate<T> implements TimeoutRetryPredicate<T> {

        private final IntMath.XorShift32 random;

        private final AdvancedRetryPredicate<T> arp;

        private final int nrImmediateRetries;

        private final long maxWaitNanos;

        private final long minWaitNanos;

        private Map<Object, RetryData> retryRegistry;

        private final Function<T, ?> mapper;

        public FibonacciBackoffRetryPredicate(final AdvancedRetryPredicate<T> arp,
                final int nrImmediateRetries, final long minWaitNanos, final long maxWaitNanos,
                final Function<T, ?> mapper) {
            this.arp = arp;
            this.nrImmediateRetries = nrImmediateRetries;
            this.maxWaitNanos = maxWaitNanos;
            this.minWaitNanos = minWaitNanos;
            retryRegistry = null;
            this.mapper = mapper;
            this.random = new IntMath.XorShift32();
        }


        @Override
        @SuppressFBWarnings("MDM_THREAD_YIELD")
        public Action apply(final T value, final long deadlineNanos) throws InterruptedException, TimeoutException {
            long currentTimeNanos = System.nanoTime();
            if (currentTimeNanos > deadlineNanos) {
                return Action.ABORT;
            }
            if (retryRegistry == null) {
                retryRegistry = new HashMap<>();
            }
            AdvancedAction action = arp.apply(value, deadlineNanos);
            switch (action) {
                case ABORT:
                    return Action.ABORT;
                case RETRY_IMMEDIATE:
                    return Action.RETRY;
                case RETRY_DELAYED:
                case RETRY:
                    RetryData retryData = getRetryData(value, action);
                    final long nextDelay = retryData.nextDelayNanos();
                    long delayNanos = Math.min(nextDelay, deadlineNanos - currentTimeNanos);
                    if (delayNanos > 0) {
                        delayNanos = Math.abs(random.nextInt()) % delayNanos;
                        Thread.sleep(TimeUnit.NANOSECONDS.toMillis(delayNanos));
                    }
                    return Action.RETRY;
                default:
                    throw new RuntimeException("Unsupperted Retry Action " + action);

            }
        }

        RetryData getRetryData(final T value, final AdvancedAction action) {
            Object rootCauseClass = mapper.apply(value);
            RetryData data = retryRegistry.get(rootCauseClass);
            if (data == null) {
                data  = createRetryData(action);
                retryRegistry.put(rootCauseClass, data);
            }
            return data;
        }

        private RetryData createRetryData(final AdvancedAction action) {
            if (action == AdvancedAction.RETRY_DELAYED) {
                return new RetryData(0, minWaitNanos, maxWaitNanos);
            } else {
                return new RetryData(nrImmediateRetries, minWaitNanos, maxWaitNanos);
            }
        }


    }



    @SuppressFBWarnings("BED_BOGUS_EXCEPTION_DECLARATION")// findbugs s wrong.
    public static <T, EX extends Exception> T executeWithRetry(final TimeoutCallable<T, EX> what,
            final TimeoutRetryPredicate<? super T> retryOnReturnVal,
            final TimeoutRetryPredicate<Exception> retryOnException)
            throws InterruptedException, EX, TimeoutException {
        final long deadlineNanos = what.getDeadlineNanos();
        return executeWithRetry(what,
                new TimeoutRetryPredicate2RetryPredicate<>(deadlineNanos, retryOnReturnVal),
                new TimeoutRetryPredicate2RetryPredicate<>(deadlineNanos, retryOnException));
    }


    public abstract static class TimeoutCallable<T, EX extends Exception> extends CheckedCallable<T, EX> {

        private final long mdeadlineNanos;

        public TimeoutCallable(final long timeoutNanos) {
            mdeadlineNanos = System.nanoTime() + timeoutNanos;
        }

        @Override
        public final T call() throws EX, InterruptedException, TimeoutException {
            return call(mdeadlineNanos);
        }

        public abstract T call(long deadlinenanos) throws EX, InterruptedException, TimeoutException;

        public final long getDeadlineNanos() {
            return mdeadlineNanos;
        }

    }




    public interface DelayPredicate<T> {
        /**
         * the number or millis of delay until the next retry, or -1 for abort.
         * @param value
         * @return
         */
        int apply(T value);

        DelayPredicate<Object> NORETRY_DELAY_PREDICATE = new DelayPredicate<Object>() {

            @Override
            public int apply(final Object value) {
                return -1;
            }

        };
    }


    public interface TimeoutDelayPredicate<T>  {

        /**
         *
         * @param value - the value to apply the predicate for.
         * @param deadlineNanos - Deadline in nanos, relative to System.nanotime().
         * @return the number or nanos of delay until the next retry, or -1 for abort.
         */
         long apply(T value, long deadlineNanos);


         TimeoutDelayPredicate<Object> NORETRY_FOR_RESULT = new TimeoutDelayPredicate<Object>() {

            @Override
            public long apply(final Object value, final long deadlineNanos) {
                return -1;
            }

        };


    }

    public static final class SmartRetryPredicate2TimeoutRetryPredicate<T>
    implements TimeoutRetryPredicate<T> {

        private final TimeoutDelayPredicate predicate;

        public SmartRetryPredicate2TimeoutRetryPredicate(final TimeoutDelayPredicate<T> predicate) {
            this.predicate = predicate;
        }



        @Override
        @SuppressFBWarnings("MDM_THREAD_YIELD")
        public Action apply(final T value, final long deadlineNanos) throws InterruptedException {
            long apply = predicate.apply(value, deadlineNanos);
            if (apply < 0) {
                return Action.ABORT;
            } else if (apply == 0) {
                return Action.RETRY;
            } else {
                Thread.sleep(TimeUnit.NANOSECONDS.toMillis(apply));
                return Action.RETRY;
            }
        }

    }


    public static <T, EX extends Exception> T executeWithRetry(final TimeoutCallable<T, EX> what,
            final TimeoutDelayPredicate<T> retryOnReturnVal,
            final TimeoutDelayPredicate<Exception> retryOnException)
            throws InterruptedException, EX, TimeoutException {
        return executeWithRetry(what, new SmartRetryPredicate2TimeoutRetryPredicate<>(retryOnReturnVal),
                new SmartRetryPredicate2TimeoutRetryPredicate<>(retryOnException));
    }

   public static <T, EX extends Exception> T executeWithRetry(final TimeoutCallable<T, EX> what,
            final TimeoutDelayPredicate<Exception> retryOnException)
            throws InterruptedException, EX, TimeoutException {
        return (T) executeWithRetry(what, (TimeoutDelayPredicate<T>) TimeoutDelayPredicate.NORETRY_FOR_RESULT,
                retryOnException);
    }

    public interface TimeoutRetryPredicate<T> {

        Action apply(T value, long deadlineNanos)
                throws InterruptedException, TimeoutException;

        TimeoutRetryPredicate<Object> NORETRY_FOR_RESULT = new TimeoutRetryPredicate<Object>() {

            @Override
            public Action apply(final Object value, final long deadlineNanos) {
                return Action.ABORT;
            }

        };

    }

    public static final class TimeoutRetryPredicate2RetryPredicate<T> implements RetryPredicate<T, TimeoutException> {

        private final long deadlineNanos;

        private final TimeoutRetryPredicate<T> predicate;

        public TimeoutRetryPredicate2RetryPredicate(final long deadlineNanos,
                final TimeoutRetryPredicate<T> predicate) {
            this.deadlineNanos = deadlineNanos;
            this.predicate = predicate;
        }



        @Override
        public Action apply(final T value) throws TimeoutException, InterruptedException {
            return predicate.apply(value, deadlineNanos);
        }


    }


    /**
     * A callable that will be retried.
     * @param <T> - The type returned by  Callable.
     * @param <EX> - The type of exception thrown by call.
     */
    public abstract static class CheckedCallable<T, EX extends Exception> implements RetryCallable<T, EX> {

        /**
         * method to process result (after all retries exhausted).
         * @param lastRet - the last return.
         * @return - the value being returned.
         */
        //design for extension here is not quite right. This case is a case of a "default implementation"
        //CHECKSTYLE:OFF
        @Override
        public  T lastReturn(final T lastRet) {
            return lastRet;
        }

        @Override
        public <EXX extends Exception> EXX lastException(EXX ex) throws EXX {
            throw ex;
        }
        //CHECKSTYLE:ON


        @Override
        public abstract T call() throws EX, InterruptedException, TimeoutException;

    }



    /**
     * A callable that will be retried.
     * @param <T> - the type of the object returned by this callable.
     * @param <EX> - the exception type returned by this callable.
     */
    public interface RetryCallable<T, EX extends Exception> extends Callable<T> {

        /**
         * the method that is retried.
         * @return
         * @throws EX
         * @throws InterruptedException
         * @throws java.util.concurrent.TimeoutException
         */
        @Override
        T call() throws EX, InterruptedException, TimeoutException;

        /**
         * method to process result (after all retries exhausted).
         * @param lastRet
         * @return - the result to be returned.
         */
        T lastReturn(T lastRet);

        /**
         * method to press the exception after all retries exhausted.
         * @param <EXX>
         * @param ex
         * @return - the exception to be thrown.
         * @throws EXX
         */
       <EXX extends Exception> EXX lastException(EXX ex) throws EXX;

    }




    public interface RetryPredicate<T, EX extends Exception> {

        Action apply(T value) throws EX, InterruptedException;
    }

    /**
     * Naive implementation of execution with retry logic. a callable will be executed and retry attempted in current
     * thread if the result and exception predicates. before retry, a callable can be executed that can abort the retry
     * and finish the function with the previous result.
     *
     * @param <T> - The type of callable to retry result;
     * @param <EX> - the exception thrown by the callable to retry.
     * @param what - the callable to retry.
     * @param retryOnReturnVal - the predicate to control retry on return value.
     * @param retryOnException - the predicate to return on retry value.
     * @return the result of the retried callable if successful.
     * @throws java.lang.InterruptedException - thrown if retry interrupted.
     * @throws EX - the exception thrown by callable.
     */
    @SuppressFBWarnings("BC_UNCONFIRMED_CAST_OF_RETURN_VALUE")
    public static <T, EX extends Exception, EX2 extends Exception> T executeWithRetry(
            final RetryCallable<T, EX> what,
            final RetryPredicate<? super T, EX2> retryOnReturnVal,
            final RetryPredicate<Exception, EX2> retryOnException)
            throws InterruptedException, EX, EX2 {
        T result = null;
        Exception lastEx = null; // last exception
        try {
            result = what.call();
        } catch (InterruptedException ex1) {
            throw what.lastException(ex1);
        } catch (Exception e) { // only EX and RuntimeException
            lastEx = e;
        }
        Exception lastExChain = lastEx; // last exception chained with all previous exceptions
        while ((lastEx != null && retryOnException.apply(lastEx) == Action.RETRY)
                || retryOnReturnVal.apply(result) == Action.RETRY) {
            if (Thread.interrupted()) {
                Thread.currentThread().interrupt();
                throw what.lastException(new InterruptedException());
            }
            result = null;
            lastEx = null;
            try {
                result = what.call();
            } catch (InterruptedException ex1) {
                throw what.lastException(ex1);
            } catch (Exception e) { // only EX and RuntimeException
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
                throw what.lastException((RuntimeException) lastExChain);
            } else {
                throw what.lastException((EX) lastExChain);
            }
        }
        return what.lastReturn(result);
    }

}
