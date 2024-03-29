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
package org.spf4j.concurrent.jdbc;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.TimeUnit;
import org.spf4j.base.TimeSource;
import org.spf4j.concurrent.Semaphore;

public final class ProcessLimitedJdbcSemaphore implements Semaphore {

  private final JdbcSemaphore jdbcSemaphore;

  private final java.util.concurrent.Semaphore semaphore;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public ProcessLimitedJdbcSemaphore(final JdbcSemaphore jdbcSemaphore, final int maxProcessPermits) {
    this.jdbcSemaphore = jdbcSemaphore;
    this.semaphore = new java.util.concurrent.Semaphore(maxProcessPermits);
  }

  @Override
  public void release(final int nrReservations) {
    jdbcSemaphore.release(nrReservations);
    semaphore.release(nrReservations);
  }


  @Override
  public boolean tryAcquire(final int nrPermits, final long deadlineNanos) throws InterruptedException {
    long nanosToDeadline = deadlineNanos - TimeSource.nanoTime();
    if (nanosToDeadline <= 0) {
      return false;
    }
    if (semaphore.tryAcquire(nrPermits, nanosToDeadline, TimeUnit.NANOSECONDS)) {
      try {
        return jdbcSemaphore.tryAcquire(nrPermits, deadlineNanos);
      } catch (InterruptedException | RuntimeException e) {
        semaphore.release();
        throw e;
      }
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return "ProcessLimitedJdbcSemaphore{" + "jdbcSemaphore=" + jdbcSemaphore + ", semaphore=" + semaphore + '}';
  }


}
