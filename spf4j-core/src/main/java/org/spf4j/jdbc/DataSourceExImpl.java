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
package org.spf4j.jdbc;

import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLTimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.concurrent.Semaphore;

/**
 *
 * @author Zoltan Farkas
 */
class DataSourceExImpl implements DataSourceEx {

  private final DataSource ds;
  private final Semaphore sem;

  DataSourceExImpl(final Semaphore sem, final DataSource ds) {
    this.ds = ds;
    this.sem = sem;
  }

  @Override
  public Connection getConnection(final long timeout, final TimeUnit unit) throws SQLException {
    boolean tryAcquire;
    try {
      tryAcquire = sem.tryAcquire(1, timeout, unit);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new SQLException(ex);
    }
    if (!tryAcquire) {
      throw new SQLTimeoutException();
    }
    return (Connection) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            new Class<?>[]{Connection.class}, new SemaphoredConnectionInvocationHandler(ds.getConnection(), sem));
  }

  @Override
  public Connection getConnection() throws SQLException {
    boolean tryAcquire;
    try {
      tryAcquire = sem.tryAcquire(1, ExecutionContexts.getContextDeadlineNanos());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new SQLException(ex);
    }
    if (!tryAcquire) {
      throw new SQLTimeoutException();
    }
    return (Connection) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            new Class<?>[]{Connection.class}, new SemaphoredConnectionInvocationHandler(ds.getConnection(), sem));
  }

  @Override
  public Connection getConnection(final String username, final String password) throws SQLException {
    return ds.getConnection(username, password);
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return ds.getLogWriter();
  }

  @Override
  public void setLogWriter(final PrintWriter out) throws SQLException {
    ds.setLogWriter(out);
  }

  @Override
  public void setLoginTimeout(final int seconds) throws SQLException {
    ds.setLoginTimeout(seconds);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return ds.getLoginTimeout();
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    return ds.getParentLogger();
  }

  @Override
  public <T> T unwrap(final Class<T> iface) throws SQLException {
    return ds.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(final Class<?> iface) throws SQLException {
    return ds.isWrapperFor(iface);
  }

  @Override
  public String toString() {
    return "DataSourceExImpl{" + "ds=" + ds + ", sem=" + sem + '}';
  }

}
