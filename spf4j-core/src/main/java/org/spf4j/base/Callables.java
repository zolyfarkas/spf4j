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
import java.sql.SQLException;
import java.sql.SQLTransientException;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for executing stuff with retry logic.
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class Callables {

    private Callables() { }

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
            if (rootCause instanceof SQLException
                    && !(rootCause instanceof SQLTransientException)) {
                return false;
            }
            LOG.debug("Exception encountered, retrying...", input);
            return true;
        }
    };

    public static final class RetryPause implements Callable<Boolean> {

        private final int nrImmediateRetries;
        private final int nrTotalRetries;
        private final int waitMillis;
        private int count;

        public RetryPause(final int nrImmediateRetries, final int nrTotalRetries, final int retryWaitMillis) {
            assert (nrImmediateRetries <= nrTotalRetries);
            this.nrImmediateRetries = nrImmediateRetries;
            this.nrTotalRetries = nrTotalRetries;
            this.waitMillis = retryWaitMillis;
        }
           
        @Override
        public Boolean call() throws Exception {
           if (count >= nrTotalRetries) {
               return Boolean.FALSE;
           }
           if (count >= nrImmediateRetries) {
               Thread.sleep(waitMillis);
           }
           count++;
           return Boolean.TRUE;
        }
        
    }
    
    
    public static final class RetryPauseWithTimeout implements Callable<Boolean> {

        private final int nrImmediateRetries;
        private final int waitMillis;
        private final TimeoutCallable callable;
        private int count;
        private final Callable<Boolean> doBeforeRetry;
        private IntMath.XorShift32 random;

        public RetryPauseWithTimeout(final int nrImmediateRetries,
                final int retryWaitMillis, final TimeoutCallable callable,
                @Nullable final Callable<Boolean> doBeforeRetry) {
            this.nrImmediateRetries = nrImmediateRetries;
            this.waitMillis = retryWaitMillis;
            this.callable = callable;
            this.doBeforeRetry = doBeforeRetry;
        }
           
        @Override
        public Boolean call() throws Exception {
           long now = System.currentTimeMillis();
           if (now  >= callable.getDeadline()) {
               return Boolean.FALSE;
           }
           if (count >= nrImmediateRetries) {
               if (random == null) {
                   // This might belong in a thread local.
                   random = new IntMath.XorShift32();
               }
               Thread.sleep(waitMillis + random.nextInt() % waitMillis);
           }
           count++;
           if (doBeforeRetry != null) {
               return doBeforeRetry.call();
           }
           return Boolean.TRUE;
        }
        
    }
    
    
    public static <T> T executeWithRetry(final Callable<T> what, final int nrImmediateRetries,
            final int nrTotalRetries, final int retryWaitMillis)
            throws InterruptedException {
       return executeWithRetry(what, new RetryPause(nrImmediateRetries, nrTotalRetries, retryWaitMillis),
               NORETRY_FOR_RESULT, DEFAULT_EXCEPTION_RETRY);
    }
    
    public static <T> T executeWithRetry(final Callable<T> what, final int nrImmediateRetries,
            final int nrTotalRetries, final int retryWaitMillis , final Predicate<Exception> retryOnException)
            throws InterruptedException {
       return executeWithRetry(what, new RetryPause(nrImmediateRetries, nrTotalRetries, retryWaitMillis),
               NORETRY_FOR_RESULT, retryOnException);
    }
    
   public static <T> T executeWithRetry(final Callable<T> what, final int nrImmediateRetries,
            final int nrTotalRetries, final int retryWaitMillis, final Predicate<? super T> retryOnReturnVal,
            final Predicate<Exception> retryOnException)
            throws InterruptedException {
       return executeWithRetry(what, new RetryPause(nrImmediateRetries, nrTotalRetries, retryWaitMillis),
               retryOnReturnVal, retryOnException);
    }
    

    public static <T> T executeWithRetry(final TimeoutCallable<T> what, final int nrImmediateRetries,
            final int retryWaitMillis)
            throws InterruptedException {
       return executeWithRetry(what, new RetryPauseWithTimeout(
               nrImmediateRetries, retryWaitMillis, what, null),
               NORETRY_FOR_RESULT, DEFAULT_EXCEPTION_RETRY);
    }
    
    public static <T> T executeWithRetry(final TimeoutCallable<T> what, final int nrImmediateRetries,
            final int retryWaitMillis ,
            final Predicate<Exception> retryOnException)
            throws InterruptedException {
       return executeWithRetry(what, new RetryPauseWithTimeout(
               nrImmediateRetries, retryWaitMillis, what, null),
               NORETRY_FOR_RESULT, retryOnException);
    }
    
    public static <T> T executeWithRetry(final TimeoutCallable<T> what, final int nrImmediateRetries,
            final int retryWaitMillis,
            final Predicate<? super T> retryOnReturnVal, final Predicate<Exception> retryOnException)
            throws InterruptedException {
        return executeWithRetry(what, new RetryPauseWithTimeout(
                nrImmediateRetries, retryWaitMillis, what, null),
                retryOnReturnVal, retryOnException);
    }
    
    public static <T> T executeWithRetry(final TimeoutCallable<T> what, final Callable<Boolean> doBeforeRetry,
            final int nrImmediateRetries, final int retryWaitMillis,
            final Predicate<? super T> retryOnReturnVal, final Predicate<Exception> retryOnException)
            throws InterruptedException {
        return executeWithRetry(what, new RetryPauseWithTimeout(
                nrImmediateRetries, retryWaitMillis, what, doBeforeRetry),
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
   
   
   
    /**
     * Naive implementation of execution with retry logic.
     * a callable will be executed and retry attempted in current thread if the result and exception predicates.
     * before retry, a callable can be executed that can abort the retry
     * and finish the function with the previous result.
     * 
     * @param what
     * @param doBeforeRetry
     * @param retryOnReturnVal
     * @param retryOnException
     * @return
     * @throws InterruptedException
     */
    public static <T> T executeWithRetry(final Callable<T> what, final Callable<Boolean> doBeforeRetry,
            final Predicate<? super T> retryOnReturnVal, final Predicate<Exception> retryOnException)
            throws InterruptedException {
        T result = null;
        Exception ex = null;
        Exception lastEx = null;
        try {
            result = what.call();
        } catch (InterruptedException ex1) {
               throw ex1;
        } catch (Exception e) {
            ex = e;
            lastEx = e;
        }
        Exception prevEx = ex;
        while ((lastEx != null && retryOnException.apply(lastEx)) || retryOnReturnVal.apply(result)) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            try {
                boolean retry = doBeforeRetry.call();
                if (!retry) {
                    break;
                }
            } catch (InterruptedException ex1) {
               throw ex1;
            }  catch (Exception ex1) {
                throw new RuntimeException(ex1);
            }
            ex = null;
            result = null;
            lastEx = null;
            try {
                result = what.call();
            } catch (InterruptedException ex1) {
               throw ex1;
            } catch (Exception e) {
                lastEx = e;
                if (prevEx != null) {
                    e = Throwables.suppress(e, prevEx);
                    prevEx = e;
                }
                ex = e;
            }
        }
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        } else if (ex != null) {
            throw new RuntimeException(ex);
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
