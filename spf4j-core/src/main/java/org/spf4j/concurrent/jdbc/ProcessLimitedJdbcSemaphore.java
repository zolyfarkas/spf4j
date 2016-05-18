package org.spf4j.concurrent.jdbc;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
  public void acquire(final long timeout, final TimeUnit unit) throws InterruptedException, TimeoutException {
    if (semaphore.tryAcquire(timeout, unit)) {
      try {
        jdbcSemaphore.acquire(timeout, unit);
      } catch (InterruptedException | TimeoutException | RuntimeException e) {
        semaphore.release();
        throw e;
      }
    } else {
      throw new TimeoutException("Timeout out after " + timeout + ' ' + unit);
    }
  }

  @Override
  public void acquire(final int nrPermits, final long timeout, final TimeUnit unit)
          throws InterruptedException, TimeoutException {
    if (semaphore.tryAcquire(nrPermits, timeout, unit)) {
      try {
        jdbcSemaphore.acquire(nrPermits, timeout, unit);
      } catch (InterruptedException | TimeoutException | RuntimeException e) {
        semaphore.release();
        throw e;
      }
    } else {
      throw new TimeoutException("Timeout out after " + timeout + ' ' + unit);
    }
  }

  @Override
  public void release() {
    jdbcSemaphore.release();
    semaphore.release();
  }

  @Override
  public void release(final int nrReservations) {
    jdbcSemaphore.release(nrReservations);
    semaphore.release();
  }

  @Override
  public boolean tryAcquire(final long timeout, final TimeUnit unit) throws InterruptedException {
    if (semaphore.tryAcquire(timeout, unit)) {
      try {
        return jdbcSemaphore.tryAcquire(timeout, unit);
      } catch (InterruptedException | RuntimeException e) {
        semaphore.release();
        throw e;
      }
    } else {
      return false;
    }
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
