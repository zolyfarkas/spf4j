
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

import java.util.concurrent.Callable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.spf4j.annotations.Retry;
import org.spf4j.annotations.VoidPredicate;
import org.spf4j.base.Callables;

/**
 * Aspect that measures execution time and does performance logging for all methods annotated with: PerformanceMonitor
 * annotation.
 *
 * @author zoly
 */
@Aspect
public final class RetryAspect {

    @Around(value = "execution(@org.spf4j.annotations.Retry * *(..)) && @annotation(annot)",
            argNames = "pjp,annot")
    public Object performanceMonitoredMethod(final ProceedingJoinPoint pjp, final Retry annot)
            throws Throwable {

        return Callables.executeWithRetry(new Callable<Object>() {

            @Override
            public Object call() throws Exception {
                try {
                    return pjp.proceed();
                } catch (Throwable ex) {
                    throw new Exception(ex);
                }
            }
        }, annot.immediateRetries(), annot.totalRetries(), annot.retryDelayMillis(),
                annot.timeoutMillis(),
                annot.exRetry() == VoidPredicate.class ? Callables.RETRY_FOR_ANY_EXCEPTION
                        : annot.exRetry().newInstance());

    }
}
