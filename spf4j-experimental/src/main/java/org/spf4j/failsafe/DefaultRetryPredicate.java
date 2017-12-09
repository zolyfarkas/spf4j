/*
 * Copyright 2017 SPF4J.
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

import java.net.SocketException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Throwables;

/**
 *
 * @author Zoltan Farkas
 */
public class DefaultRetryPredicate implements RetryPredicate<Exception, Callable<?>> {

  private static final Logger LOG = LoggerFactory.getLogger(RetryPredicate.class);

  private static final long MAX_DELAY_NANOS = TimeUnit.SECONDS.toNanos(10);

  private final Predicate<Exception> predicate;

  private long i = 0;

  private long j = TimeUnit.MILLISECONDS.toNanos(1);

  public DefaultRetryPredicate() {
    this(DefaultRetryPredicate::isRecoverable);
  }

  public DefaultRetryPredicate(final Predicate<Exception> predicate) {
    this.predicate = predicate;
  }

  @Override
  public RetryPredicate<Exception, Callable<?>> newInstance() {
    return new DefaultRetryPredicate();
  }

  public static final boolean isRecoverable(final Exception value) {
    Throwable rootCause = com.google.common.base.Throwables.getRootCause(value);
    if (rootCause instanceof RuntimeException) {
      return false;
    }
    Throwable e = Throwables.firstCause(value,
            (ex) -> (ex instanceof SQLTransientException
            || ex instanceof SQLRecoverableException
            || ex instanceof SocketException
            || ex instanceof TimeoutException));
    if (e != null) {
      return true;
    } else {
      return false;
    }

  }


  @Override
  public RetryDecision getDecision(final Exception value, final Callable what) {
    if (predicate.test(value)) {
      LOG.debug("Exception encountered, retrying {}...", what, value);
      long delay = i;
      if (delay < MAX_DELAY_NANOS) {
        i = j;
        j = i + delay;
      } else {
        delay = MAX_DELAY_NANOS;
      }
      return RetryDecision.retry(delay, what);
    } else {
      return RetryDecision.abort(value);
    }
  }

}
