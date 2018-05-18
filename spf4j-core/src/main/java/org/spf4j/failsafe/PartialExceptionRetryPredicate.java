/*
 * Copyright 2018 SPF4J.
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
package org.spf4j.failsafe;

import java.util.concurrent.Callable;
import org.spf4j.base.Callables;

/**
 *
 * @author Zoltan Farkas
 */
@FunctionalInterface
public interface PartialExceptionRetryPredicate<T, C extends Callable<? extends T>>
        extends PartialTypedExceptionRetryPredicate<T, C, Exception> {

  /**
   * @deprecated use this method to migrate from deprecated API to new APIs (failsafe)
   */
  @Deprecated
  static PartialExceptionRetryPredicate<?, ? extends Callable<?>> from(
          final Callables.AdvancedRetryPredicate<Exception> oldStyle) {
    return new PartialExceptionRetryPredicate<Object, Callable<? extends Object>>() {
      @Override
      public org.spf4j.failsafe.RetryDecision getExceptionDecision(final Exception value, final Callable what) {
        Callables.AdvancedAction aa = oldStyle.apply(value);
        switch (aa) {
          case ABORT:
            return org.spf4j.failsafe.RetryDecision.abort();
          case RETRY:
          case RETRY_DELAYED:
            return org.spf4j.failsafe.RetryDecision.retryDefault(what);
          case RETRY_IMMEDIATE:
            return org.spf4j.failsafe.RetryDecision.retry(0, what);
          default:
            throw new IllegalStateException("Invalid enum value " + aa);
        }
      }
    };
  }




}
