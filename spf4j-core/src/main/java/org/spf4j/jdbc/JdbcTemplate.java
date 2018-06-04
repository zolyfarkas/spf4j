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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnegative;
import javax.annotation.Signed;
import javax.sql.DataSource;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.HandlerNano;
import org.spf4j.base.JavaUtils;
import org.spf4j.base.TimeSource;
import org.spf4j.failsafe.RetryPolicy;

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

  private final RetryPolicy<Object, Callable<? extends Object>> retryPolicy;

  public JdbcTemplate(final DataSource dataSource) {
    this(dataSource, RetryPolicy.newBuilder()
            .withDefaultThrowableRetryPredicate()
            .build());
  }

  public JdbcTemplate(final DataSource dataSource, final RetryPolicy<Object, Callable<? extends Object>> retryPolicy) {
    this.dataSource = dataSource;
    this.retryPolicy = retryPolicy;
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
    try (ExecutionContext ctx = ExecutionContexts.start(handler.toString(), timeout, tu)) {
      return (R) retryPolicy.call(new Callable() {
        @Override
        public R call() throws SQLException {
          try (Connection conn = dataSource.getConnection()) {
            boolean autocomit = conn.getAutoCommit();
            if (autocomit) {
              conn.setAutoCommit(false);
            }
            try {
              R result = handler.handle(conn, ctx.getDeadlineNanos());
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
      }, SQLException.class, ctx.getDeadlineNanos());
    } catch (TimeoutException ex) {
      throw new SQLTimeoutException(ex);
    }
  }

  @SuppressFBWarnings("BED_BOGUS_EXCEPTION_DECLARATION")
  public <R> R transactOnConnectionNonInterrupt(final HandlerNano<Connection, R, SQLException> handler,
          final long timeout, final TimeUnit tu)
          throws SQLException {
    try {
      return transactOnConnection(handler, timeout, tu);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new SQLException(ex);
    }
  }


  /**
   * @param deadlineNanos the deadline relative to the same as System.nanoTime()
   * @return
   */
  @Nonnegative
  public static int getTimeoutToDeadlineSeconds(final long deadlineNanos) throws SQLTimeoutException {
    long toSeconds = TimeUnit.NANOSECONDS.toSeconds(deadlineNanos - TimeSource.nanoTime());
    if (toSeconds < 0L) {
      throw new SQLTimeoutException("deadline exceeded by " + (-toSeconds) + " seconds");
    }
    if (toSeconds == 0) {
      return 1;
    }
    if (toSeconds > MAX_JDBC_TIMEOUTSECONDS) {
      return MAX_JDBC_TIMEOUTSECONDS;
    } else {
     return (int) toSeconds;
    }
  }

  @Signed
  public static int getTimeoutToDeadlineSecondsNoEx(final long deadlineNanos) {
    long toSeconds = TimeUnit.NANOSECONDS.toSeconds(deadlineNanos - TimeSource.nanoTime());
    if (toSeconds == 0) {
      return 1;
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
