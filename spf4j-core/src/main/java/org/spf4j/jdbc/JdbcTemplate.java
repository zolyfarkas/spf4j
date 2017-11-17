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

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.sql.DataSource;
import org.spf4j.base.CallablesNano;
import org.spf4j.base.CallablesNanoNonInterrupt;
import org.spf4j.base.HandlerNano;
import org.spf4j.base.JavaUtils;

/**
 * A very simple JdbTemplate.
 *
 * @author zoly
 */
@Beta
public final class JdbcTemplate {

  /**
   * certain JDBC drivers multiply timeout to transform in milis or nanos without validating overflow.
   * This value needs to be low enough to account for this case.
   */
  private static final int MAX_JDBC_TIMEOUTSECONDS =
          Integer.getInteger("spf4j.jdbc.maxdbcTimeoutSeconds", 3600 * 24);


  private final DataSource dataSource;

  public JdbcTemplate(final DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public static void checkJdbcObjectName(final CharSequence name) {
    if (!JavaUtils.isJavaIdentifier(name) || name.length() > 30) {
      throw new IllegalArgumentException("Invalid database Object identifier " + name);
    }
  }


  @SuppressFBWarnings("BED_BOGUS_EXCEPTION_DECLARATION")
  public <R> R transactOnConnection(final HandlerNano<Connection, R, SQLException> handler,
          final long timeout, final TimeUnit tu)
          throws SQLException, InterruptedException {
    try {
      return CallablesNano.executeWithRetry(
              new CallablesNano.NanoTimeoutCallable<R, SQLException>(tu.toNanos(timeout)) {

                @Override
                // CHECKSTYLE IGNORE RedundantThrows FOR NEXT 100 LINES
                @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
                public R call(final long deadlineNanos)
                        throws SQLException {
                  try (Connection conn = dataSource.getConnection()) {
                    boolean autocomit = conn.getAutoCommit();
                    if (autocomit) {
                      conn.setAutoCommit(false);
                    }
                    try {
                      R result = handler.handle(conn, deadlineNanos);
                      conn.commit();
                      return result;
                    } catch (SQLException | RuntimeException ex) {
                      conn.rollback();
                      throw ex;
                    } finally {
                      if (autocomit) {
                        conn.setAutoCommit(true);
                      }
                    }
                  }
                }
              }, 2, 1000, SQLException.class);
    } catch (TimeoutException ex) {
      throw new SQLTimeoutException(ex);
    }

  }

  @SuppressFBWarnings("BED_BOGUS_EXCEPTION_DECLARATION")
  public <R> R transactOnConnectionNonInterrupt(final HandlerNano<Connection, R, SQLException> handler,
          final long timeout, final TimeUnit tu)
          throws SQLException {
      return CallablesNanoNonInterrupt.executeWithRetry(
              new CallablesNano.NanoTimeoutCallable<R, SQLException>(tu.toNanos(timeout)) {

        @Override
        // CHECKSTYLE IGNORE RedundantThrows FOR NEXT 100 LINES
        @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
        public R call(final long deadlineNanos)
                throws SQLException {
          try (Connection conn = dataSource.getConnection()) {
            boolean autocomit = conn.getAutoCommit();
            if (autocomit) {
              conn.setAutoCommit(false);
            }
            try {
              R result = handler.handle(conn, deadlineNanos);
              conn.commit();
              return result;
            } catch (SQLException | RuntimeException ex) {
              conn.rollback();
              throw ex;
            } finally {
              if (autocomit) {
                conn.setAutoCommit(true);
              }
            }
          }
        }
      }, 2, 1000, SQLException.class);
  }


  /**
   * @param deadlineNanos the deadline relative to the same as System.nanoTime()
   * @return
   */
  public static int getTimeoutToDeadlineSeconds(final long deadlineNanos) {
    long toSeconds = TimeUnit.NANOSECONDS.toSeconds(deadlineNanos - System.nanoTime());
    if (toSeconds < 0L) {
      throw new UncheckedTimeoutException("deadline exceeded by " + (-toSeconds) + " seconds");
    }
    if (toSeconds > MAX_JDBC_TIMEOUTSECONDS) {
      return MAX_JDBC_TIMEOUTSECONDS;
    } else {
     return (int) toSeconds;
    }
  }

  @Override
  public String toString() {
    return "JdbcTemplate{" + "dataSource=" + dataSource + '}';
  }

}
