package org.spf4j.concurrent.jdbc;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import javax.sql.DataSource;

/**
 * A Jdbc Lock implementation.
 * @author zoly
 */
public final class JdbcLock implements Lock, AutoCloseable {

  private final JdbcSemaphore semaphore;

  private final int jdbcTimeoutSeconds;

  public JdbcLock(final DataSource dataSource, final SemaphoreTablesDesc semTableDesc,
          final String lockName, final int jdbcTimeoutSeconds) throws InterruptedException {
    this.semaphore = new JdbcSemaphore(dataSource, semTableDesc, lockName, 1, jdbcTimeoutSeconds, true);
    this.jdbcTimeoutSeconds = jdbcTimeoutSeconds;
    this.semaphore.registerJmx();
  }

  @Override
  public void lock() {
    try {
      semaphore.acquire(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    } catch (InterruptedException | TimeoutException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void lockInterruptibly() throws InterruptedException {
    try {
      semaphore.acquire(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    } catch (TimeoutException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public boolean tryLock() {
    try {
      return semaphore.tryAcquire(((long) jdbcTimeoutSeconds) * 4, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public boolean tryLock(final long time, final TimeUnit unit) throws InterruptedException {
      return semaphore.tryAcquire(time, unit);
  }

  @Override
  public void unlock() {
    semaphore.release();
  }

  @Override
  public Condition newCondition() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() {
    semaphore.close();
  }

  @Override
  public String toString() {
    return "JdbcLock{" + "semaphore=" + semaphore + ", jdbcTimeoutSeconds=" + jdbcTimeoutSeconds + '}';
  }



}
