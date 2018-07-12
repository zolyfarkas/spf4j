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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A randomizing Backoff strategy.
 * @author Zoltan Farkas
 */
final class JitteredDelaySupplier implements RetryDelaySupplier {

  private final RetryDelaySupplier wrapped;

  private final double jitterFactor;

  JitteredDelaySupplier(final RetryDelaySupplier wrapped, final double jitterFactor) {
    this.wrapped = wrapped;
    this.jitterFactor = jitterFactor;
  }

  @Override
  @SuppressFBWarnings("PREDICTABLE_RANDOM") // we can use predicatable randoms here
  public long nextDelay() {
    long nextDelay = wrapped.nextDelay();
    if (nextDelay > 1) {
      long jitter = (long) (nextDelay * jitterFactor);
      if (jitter > 0) {
        return ThreadLocalRandom.current().nextLong(Math.max(0, nextDelay - jitter), nextDelay);
      } else {
        return nextDelay;
      }
    } else {
      return nextDelay;
    }
  }

  @Override
  public String toString() {
    return "JitteredDelaySupplier{" + "wrapped=" + wrapped + '}';
  }

}
