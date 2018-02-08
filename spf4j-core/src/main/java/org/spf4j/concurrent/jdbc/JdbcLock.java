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

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import javax.sql.DataSource;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.concurrent.LockRuntimeException;

/**
 * A Jdbc Lock implementation.
 * @author zoly
 */
public final class JdbcLock implements Lock, AutoCloseable {

  private final JdbcSemaphore semaphore;

  private final int jdbcTimeoutSeconds;

  public JdbcLock(final DataSource dataSource, final SemaphoreTablesDesc semTableDesc,
          final String lockName, final int jdbcTimeoutSeconds) throws InterruptedException, SQLException {
    this.semaphore = new JdbcSemaphore(dataSource, semTableDesc, lockName, 1, jdbcTimeoutSeconds, true);
    this.jdbcTimeoutSeconds = jdbcTimeoutSeconds;
    this.semaphore.registerJmx();
  }

  @Override
  public void lock() {
    try {
      semaphore.acquire(ExecutionContexts.getMillisToDeadline(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException | TimeoutException ex) {
      throw new LockRuntimeException(ex);
    }
  }

  @Override
  public void lockInterruptibly() throws InterruptedException {
    try {
      semaphore.acquire(ExecutionContexts.getMillisToDeadline(), TimeUnit.MILLISECONDS);
    } catch (TimeoutException ex) {
      throw new LockRuntimeException(ex);
    }
  }

  @Override
  public boolean tryLock() {
    try {
      return semaphore.tryAcquire(((long) jdbcTimeoutSeconds) * 2, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      throw new LockRuntimeException(ex);
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
