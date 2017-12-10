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

/**
 *
 * @author Zoltan Farkas
 */
public final class FibonacciBackoff implements BackoffDelay {

    private int immediateLeft;

    private long p1;

    private long p2;

    private final long maxDelay;

    public FibonacciBackoff(final int immediateLeft, final long startDelay, final long maxDelay) {
      this.immediateLeft = immediateLeft;
      if (startDelay < 1) {
        this.p1 = 0;
        this.p2 = 1;
      } else {
        this.p1 = startDelay;
        this.p2 = startDelay;
      }
      this.maxDelay = maxDelay;
    }

  @Override
    public long nextDelay() {
      if (immediateLeft > 0) {
        immediateLeft--;
        return 0;
      } else if (p2 > maxDelay) {
        return maxDelay;
      } else {
        long result = p2;
        p2 = p1 + p2;
        p1 = result;
        return result;
      }
    }

  }

