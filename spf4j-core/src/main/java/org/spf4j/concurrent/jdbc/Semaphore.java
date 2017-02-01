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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A interface that abstracts a semaphore.
 * @author zoly
 */
@ParametersAreNonnullByDefault
public interface Semaphore {

  /**
   * Acquire one permit.
   * @param timeout  time to wait for permit to become available.
   * @param unit  units of time.
   * @throws InterruptedException - operation interrupted.
   * @throws TimeoutException - timed out.
   */
  void acquire(long timeout, TimeUnit unit)
          throws InterruptedException, TimeoutException;

  /**
   * Acquire a arbitrary number of permits.
   * @param nrPermits - numer of permits to acquire.
   * @param timeout - time to wait for permit to become available.
   * @param unit - units of time.
   * @throws InterruptedException - operation interrupted.
   * @throws TimeoutException - timed out.
   */
  void acquire(int nrPermits, long timeout, TimeUnit unit)
          throws InterruptedException, TimeoutException;


  /**
   * try to acquire a permit.
   * @param timeout  time to wait for permit to become available.
   * @param unit  units of time.
   * @return  true if permit acquired, false if timed out.
   * @throws InterruptedException - operation interrupted.
   */
  @CheckReturnValue
  boolean tryAcquire(long timeout, TimeUnit unit)
          throws InterruptedException;

  /**
   * try to acquire a number of permits.
   * @param nrPermits  number of permits to acquire.
   * @param timeout  time to wait for permits to become available.
   * @param unit  units of time.
   * @return  true if permits acquired, false if timed out.
   * @throws InterruptedException - operation interrupted.
   */
  @CheckReturnValue
  boolean tryAcquire(int nrPermits, long timeout, TimeUnit unit)
          throws InterruptedException;

  /**
   * release 1 permit.
   */
  void release();

  /**
   * release a number of permits.
   * @param nrPermits  the number of permits to release.
   */
  void release(int nrPermits);

}
