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
package org.spf4j.test.log;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.TimeUnit;
import org.spf4j.base.TimeSource;

/**
 * @author Zoltan Farkas
 */
@CleanupObligation
public interface ObservationAssert {

  /**
   * Assert that a sequence of messages has not been seen.
   * also unregisters this assertion handler.
   */
  @DischargesObligation
  void assertObservation();


  @SuppressFBWarnings("MDM_THREAD_YIELD")
  default void assertObservation(final long time, final TimeUnit tu) {
    long deadline = TimeSource.nanoTime() + tu.toNanos(time);
    AssertionError rae;
    do  {
      try {
        assertObservation();
        rae = null;
      } catch (AssertionError ae) {
        rae = ae;
      }
      if (rae == null) {
        return;
      }
      try {
        Thread.sleep(250);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new AssertionError(ex);
      }
    } while (TimeSource.nanoTime() < deadline);
    throw rae;
  }

}
