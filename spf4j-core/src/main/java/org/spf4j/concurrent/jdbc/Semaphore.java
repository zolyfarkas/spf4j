/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.concurrent.jdbc;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.CheckReturnValue;

/**
 *
 * @author zoly
 */
public interface Semaphore {

  void acquire(final long timeout, final TimeUnit unit)
          throws InterruptedException, TimeoutException;

  void acquire(final int nrPermits, final long timeout, final TimeUnit unit)
          throws InterruptedException, TimeoutException;

  void release();

  void release(final int nrReservations);

  boolean tryAcquire(final long timeout, final TimeUnit unit)
          throws InterruptedException;

  @SuppressFBWarnings(value = "UW_UNCOND_WAIT")
  @CheckReturnValue
  boolean tryAcquire(final int nrPermits, final long timeout, final TimeUnit unit)
          throws InterruptedException;

}
