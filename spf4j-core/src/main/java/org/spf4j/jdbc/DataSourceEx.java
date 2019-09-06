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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.sql.DataSource;
import org.spf4j.concurrent.LocalSemaphore;
import org.spf4j.concurrent.Semaphore;

/**
 * @author Zoltan Farkas
 */
public interface DataSourceEx extends DataSource {

  /**
   * get a JDBC connection.
   *
   * @param timeout max time to wait.
   * @param unit
   * @return null if unable to obtain connection in the provided time.
   * @throws java.sql.SQLException
   */
  @Nonnull
  Connection getConnection(long timeout, TimeUnit unit) throws SQLException;

  /**
   * Create a DataSourceEx from a plain vanilla DataSource.
   * This DataDource will be guarded by a semaphore.
   * THis is basically will allow you to not wait more than you want when the pool
   * has no more connections to provide.
   * @param ds
   * @param maxNrConnections
   * @return
   */
  static DataSourceEx from(final DataSource ds, final int maxNrConnections) {
    return new DataSourceExImpl(new LocalSemaphore(maxNrConnections, false), ds);
  }

  /**
   * Create a DataSourceEx from a plain vanilla DataSource.
   * This DataDource will be guarded by the provided semaphore.
   *
   * Using s distributed semaphore can allow you to control nr of conns at a more global level.
   * @param maxNrConnections
   * @return
   */
  static DataSourceEx from(final DataSource ds, final Semaphore sem) {
    return new DataSourceExImpl(sem, ds);
  }

}
