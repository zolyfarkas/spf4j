package org.spf4j.jdbc;

import com.google.common.annotations.Beta;
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

  @Override
  public String toString() {
    return "JdbcTemplate{" + "dataSource=" + dataSource + '}';
  }

}
