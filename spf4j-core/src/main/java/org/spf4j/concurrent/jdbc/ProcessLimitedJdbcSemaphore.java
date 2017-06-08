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

package org.spf4j.concurrent.jdbc;

import org.spf4j.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author zoly
 */
public final class ProcessLimitedJdbcSemaphore implements Semaphore {

  private final JdbcSemaphore jdbcSemaphore;

  private final java.util.concurrent.Semaphore semaphore;

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
  public boolean tryAcquire(final int nrPermits, final long timeout, final TimeUnit unit) throws InterruptedException {
    if (semaphore.tryAcquire(nrPermits, timeout, unit)) {
      try {
        return jdbcSemaphore.tryAcquire(nrPermits, timeout, unit);
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
