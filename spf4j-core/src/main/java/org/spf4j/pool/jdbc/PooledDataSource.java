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
package org.spf4j.pool.jdbc;

import com.google.common.annotations.Beta;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLTimeoutException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.spf4j.recyclable.ObjectBorrowException;
import org.spf4j.recyclable.ObjectCreationException;
import org.spf4j.recyclable.RecyclingSupplier;
import org.spf4j.recyclable.impl.RecyclingSupplierBuilder;

/**
 *
 * @author zoly
 */
@Beta
public final class PooledDataSource implements DataSource, AutoCloseable {

    private final RecyclingSupplier<Connection> pool;


    public PooledDataSource(final int initialSize, final int maxSize,
            final String driverName, final String url, final String user, final String password)
            throws ObjectCreationException {
        final JdbcConnectionFactory jdbcConnectionFactory =
                new JdbcConnectionFactory(driverName, url, user, password);
        RecyclingSupplierBuilder<Connection> builder =
                new RecyclingSupplierBuilder<>(maxSize, jdbcConnectionFactory);
        builder.withInitialSize(initialSize);
        pool = builder.build();
        jdbcConnectionFactory.setPool(pool);
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            return pool.get();
        } catch (InterruptedException | ObjectBorrowException | ObjectCreationException ex) {
            throw new SQLException(ex);
        } catch (TimeoutException ex) {
          throw new SQLTimeoutException(ex);
        }
    }

    @Override
    public Connection getConnection(final String username, final String password)
            throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setLogWriter(final PrintWriter out) throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setLoginTimeout(final int seconds) throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int getLoginTimeout() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        if (iface.equals(DataSource.class) || iface.equals(PooledDataSource.class)) {
            return (T) this;
        } else {
            throw new SQLException("Not a wrapper for " + iface);
        }
    }

    @Override
    public boolean isWrapperFor(final Class<?> iface) {
        return iface.equals(DataSource.class) || iface.equals(PooledDataSource.class);
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public String toString() {
        return "PooledDataSource{" + "pool=" + pool + '}';
    }

  @Override
  public void close() throws Exception {
    pool.dispose();
  }


}
