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
import java.util.concurrent.Callable;
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

   public static final Predicate<Exception> RETRY_FOR_ANY_EXCEPTION = new Predicate<Exception>() {
        @Override
        public boolean apply(final Exception input) {
            LOG.debug("Exception encountered, retrying...", input);
            return true;
        }
    };

    public static final class RetryPause implements Callable<Boolean> {

        private final int nrImmediateRetries;
        private final int nrTotalRetries;
        private final int waitMillis;
        private int count;

        public RetryPause(final int nrImmediateRetries, final int nrTotalRetries, final int retryWaitMillis)
        {
            assert (nrImmediateRetries < nrTotalRetries);
            this.nrImmediateRetries = nrImmediateRetries;
            this.nrTotalRetries = nrTotalRetries;
            this.waitMillis = retryWaitMillis;
        }
           
        @Override
        public Boolean call() throws Exception {
           if (count > nrTotalRetries) {
               return false;
           }
           if (count > nrImmediateRetries) {
               Thread.sleep(waitMillis);
           }
           count++;
           return true;
        }
        
    }
    
    public static <T> T executeWithRetry(final Callable<T> what, final int nrImmediateRetries,
            final int nrTotalRetries, final int retryWaitMillis)
            throws InterruptedException {
       return executeWithRetry(what, new RetryPause(nrImmediateRetries, nrTotalRetries, retryWaitMillis),
               NORETRY_FOR_RESULT, RETRY_FOR_ANY_EXCEPTION);
    }
    
    public static <T> T executeWithRetry(final Callable<T> what, final int nrImmediateRetries,
            final int nrTotalRetries, final int retryWaitMillis , final Predicate<Exception> retryOnException)
            throws InterruptedException {
       return executeWithRetry(what, new RetryPause(nrImmediateRetries, nrTotalRetries, retryWaitMillis),
               NORETRY_FOR_RESULT, retryOnException);
    }
     
    /**
     * Naive implementation of execution with retry logic.
     * a callable will be executed and retry attempted in current thread,
     * a number of immediate retries, and a number of delayed retries.
     * the immediate retries should take advantage of redundancy of a particular service,
     * while the delayed retries should deal with a temporary service outage.
     * The reason why I call this implementation naive is because this can cause
     * the current thread to sleep instead of doing other work.
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
        try {
            result = what.call();
        } catch (InterruptedException ex1) {
               throw ex1;
        } catch (Exception e) {
            ex = e;
        }
        Exception prevEx = ex;
        while ((ex != null && retryOnException.apply(ex)) || retryOnReturnVal.apply(result)) {
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
            try {
                result = what.call();
            } catch (InterruptedException ex1) {
               throw ex1;
            } catch (Exception e) {
                if (prevEx != null) {
                    e = Exceptions.chain(e, prevEx);
                    prevEx = e;
                }
                ex = e;
            }
        }
        if (ex != null) {
            throw new RuntimeException(ex);
        }
        return result;
    }
    
}
