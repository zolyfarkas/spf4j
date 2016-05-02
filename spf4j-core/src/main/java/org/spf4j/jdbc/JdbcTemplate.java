package org.spf4j.jdbc;

import com.google.common.annotations.Beta;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.spf4j.base.CallablesNano;
import org.spf4j.base.HandlerNano;

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

  @SuppressFBWarnings("BED_BOGUS_EXCEPTION_DECLARATION")
  public <R> R transactOnConnection(final HandlerNano<Connection, R, SQLException> handler,
          final long timeout, final TimeUnit tu)
          throws SQLException, InterruptedException {
    return CallablesNano.executeWithRetry(new CallablesNano.TimeoutCallable<R, SQLException>(tu.toNanos(timeout)) {

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
    }, 2, 1000);

  }

  @Override
  public String toString() {
    return "JdbcTemplate{" + "dataSource=" + dataSource + '}';
  }
  
  
}
