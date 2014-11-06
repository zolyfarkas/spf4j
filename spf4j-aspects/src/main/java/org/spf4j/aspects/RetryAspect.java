
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
package org.spf4j.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.spf4j.annotations.Retry;
import org.spf4j.annotations.VoidPredicate;
import org.spf4j.base.Callables;
import org.spf4j.base.Callables.TimeoutCallable;

/**
 * Aspect that measures execution time and does performance logging for all methods annotated with: PerformanceMonitor
 * annotation.
 *
 * @author zoly
 */
@Aspect
public final class RetryAspect {

    
    private static final ThreadLocal<Long> DEADLINE = new ThreadLocal<Long>() {

        @Override
        protected Long initialValue() {
            return Long.MAX_VALUE;
        }

    };
    
    
    public static long getDeadline() {
        return DEADLINE.get();
    }
    
    @Around(value = "execution(@org.spf4j.annotations.Retry * *(..)) && @annotation(annot)",
            argNames = "pjp,annot")
    public Object performanceMonitoredMethod(final ProceedingJoinPoint pjp, final Retry annot)
            throws Throwable {

        return Callables.executeWithRetry(new TimeoutCallable<Object, Exception>(annot.timeoutMillis()) {

            @Override
            public Object call(final long dealine) throws Exception {
                DEADLINE.set(dealine);
                try {
                    return pjp.proceed();
                } catch (Exception e) {
                  throw e;
                } catch (Throwable ex) {
                    throw new Error(ex);
                } finally {
                    DEADLINE.set(Long.MAX_VALUE);
                }
            }
        }, annot.immediateRetries(), annot.retryDelayMillis(),
                annot.exRetry() == VoidPredicate.class ? Callables.DEFAULT_EXCEPTION_RETRY
                        : annot.exRetry().newInstance());

    }
}
