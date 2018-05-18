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
package org.spf4j.aspects;

import com.google.common.util.concurrent.UncheckedExecutionException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.spf4j.annotations.Retry;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.failsafe.RetryPolicy;

/**
 * Aspect that measures execution time and does performance logging for all methods annotated with: PerformanceMonitor
 * annotation.
 *
 * @author zoly
 */
@Aspect
public final class RetryAspect {

  private static final ConcurrentMap<String, RetryPolicy> POLICIES = new ConcurrentHashMap<>();

  @Around(value = "execution(@org.spf4j.annotations.Retry * *(..)) && @annotation(org.spf4j.annotations.Retry annot)",
          argNames = "pjp,annot")
  @SuppressFBWarnings("FII_USE_METHOD_REFERENCE")
  public Object retriedMethod(final ProceedingJoinPoint pjp, final Retry annot)
          throws Throwable {
    try (ExecutionContext ctx = ExecutionContexts.start(pjp.toShortString(), annot.timeout(), annot.units())) {
      Callable c = () -> {
        try {
          return pjp.proceed();
        } catch (Exception | Error e) {
          throw e;
        } catch (Throwable ex) {
          throw new UncheckedExecutionException(ex);
        }
      };
      String retryPolicyName = annot.retryPolicyName();
      if ("".equals(retryPolicyName)) {
        return RetryPolicy.defaultPolicy().call(c, Exception.class, ctx.getDeadlineNanos());
      } else {
        return POLICIES.get(retryPolicyName).call(c, Exception.class, ctx.getDeadlineNanos());
      }
    }

  }

  public static RetryPolicy registerRetryPolicy(final String name, final RetryPolicy policy) {
    return POLICIES.put(name, policy);
  }

}
