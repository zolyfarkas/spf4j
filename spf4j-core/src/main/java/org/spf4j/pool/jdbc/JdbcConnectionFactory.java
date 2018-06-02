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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.spf4j.recyclable.ObjectCreationException;
import org.spf4j.recyclable.ObjectDisposeException;
import org.spf4j.recyclable.RecyclingSupplier;

/**
 *
 * @author zoly
 */
@Beta
public final class JdbcConnectionFactory implements RecyclingSupplier.Factory<Connection> {

  private final String url;
  private final String user;
  private final String password;

  public JdbcConnectionFactory(final String driverName, final String url,
          final String user, final String password) {
    try {
      Class.forName(driverName);
    } catch (ClassNotFoundException ex) {
      throw new IllegalArgumentException("Invalid driver " + driverName, ex);
    }
    this.url = url;
    this.password = password;
    this.user = user;
  }

  @Override
  public Connection create() throws ObjectCreationException {
    try {
      return DriverManager.getConnection(url, user, password);
    } catch (SQLException ex) {
      throw new ObjectCreationException(ex);
    }
  }

  @Override
  public void dispose(final Connection object) throws ObjectDisposeException {
    try {
      object.close();
    } catch (SQLException ex) {
      throw new ObjectDisposeException(ex);
    }
  }

  @Override
  public boolean validate(final Connection object, final Exception e) throws SQLException {
    return object.isValid(60);
  }

  @Override
  public String toString() {
    return "JdbcConnectionFactory{" + "url=" + url + ", user=" + user + '}';
  }

}
