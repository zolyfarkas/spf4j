package org.spf4j.concurrent;

import com.google.common.annotations.Beta;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.spf4j.base.HandlerNano;
import org.spf4j.base.IntMath;
import org.spf4j.jdbc.JdbcTemplate;

/**
 * A jdbc table based semaphore implementation.
 * There is a caveat to this implementation, a crashed process will keep reservations acquired...
 * which sucks... a zookeeper based implementation is preferable...
 *
 * @author zoly
 */
@SuppressFBWarnings({ "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING", "NP_LOAD_OF_KNOWN_NULL_VALUE" })
@Beta
public final class JdbcSemaphore {

  private final JdbcTemplate jdbc;

  private final String acquireSql;

  private final String releaseSql;

  private final int maxNrReservations;

  private final int jdbcTimeoutSeconds;

  private final IntMath.XorShift32 rnd;

  private final String semName;
  
  /**
   * CREATE TABLE SEMAPHORES (
   *  NAME varchar(255) NOT NULL,
   *  AVAILABLE_RESERVATIONS bigint(20) NOT NULL,
   *  MAX_RESERVATIONS bigint(20) NOT NULL,
   *  LAST_UPDATED_BY varchar(255) NOT NULL,
   *  PRIMARY KEY (NAME),
   *  UNIQUE KEY NAME (SEMAPHORE_NAME_UQK)
   * );
   * @param dataSource - the jdbc data source with the Semaphores table. Please be sensible, no "test on borrow" pools.
   */
  
  public JdbcSemaphore(final DataSource dataSource) throws SQLException, InterruptedException {
    this(dataSource, "SEMAPHORES", "NAME", "AVAILABLE_RESERVATIONS", "MAX_RESERVATIONS", "LAST_UPDATED_BY",
            "test_sem", 2, 10);
  }
  
  public JdbcSemaphore(final DataSource dataSource, final String tableName, final String semNameColumn,
          final String availableReservationsColumn, final String maxReservationsColumn, final String lastModifiedColumn,
          final String semaphoreName, final int maxNrReservations, final int jdbcTimeoutSeconds)
          throws SQLException, InterruptedException {
    this.semName = semaphoreName.intern();
    this.jdbcTimeoutSeconds = jdbcTimeoutSeconds;
    this.jdbc = new JdbcTemplate(dataSource);
    this.maxNrReservations = maxNrReservations;
    this.rnd = new IntMath.XorShift32();
    this.acquireSql = "UPDATE " + tableName + " SET "
            + availableReservationsColumn + " = " + availableReservationsColumn + " - 1, "
            + lastModifiedColumn + " = ? WHERE " + availableReservationsColumn + " > 0";
    this.releaseSql = "UPDATE " + tableName + " SET "
            + availableReservationsColumn + " = " + availableReservationsColumn + " + 1, "
            + lastModifiedColumn + " = ? WHERE " + availableReservationsColumn + " < " + maxReservationsColumn;
    try {
      createLockRowIfNotPresent(lastModifiedColumn, tableName, semNameColumn,
              availableReservationsColumn, maxReservationsColumn);
    } catch (SQLIntegrityConstraintViolationException ex) {
      // RACE condition while creating the row.
      createLockRowIfNotPresent(lastModifiedColumn, tableName, semNameColumn,
              availableReservationsColumn, maxReservationsColumn);
    }
  }

  public void createLockRowIfNotPresent(
          final String lastModifiedColumn, final String tableName, final String semNameColumn,
          final String semCountColumn, final String maxReservationsColumn) throws SQLException, InterruptedException {

    jdbc.transactOnConnection((final Connection conn, final long deadlineNanos) -> {
      try (final PreparedStatement stmt = conn.prepareStatement("SELECT " + semCountColumn
              + ',' + maxReservationsColumn + " FROM " + tableName
              + " WHERE " + semNameColumn + " = ?")) {
        stmt.setNString(1, semName);
        stmt.setQueryTimeout((int) TimeUnit.NANOSECONDS.toSeconds(deadlineNanos - System.nanoTime()));
        try (final ResultSet rs = stmt.executeQuery()) {
          if (!rs.next()) {
            try (final PreparedStatement insert = conn.prepareStatement("insert into " + tableName
                    + " (" + semNameColumn + ',' + semCountColumn + ',' + maxReservationsColumn
                    + ',' + lastModifiedColumn + ") VALUES (?, ?, ?, ?)")) {
              insert.setNString(1, semName);
              insert.setInt(2, maxNrReservations);
              insert.setInt(3, maxNrReservations);
              insert.setNString(4, FileBasedLock.getContextInfo());
              insert.setQueryTimeout((int) TimeUnit.NANOSECONDS.toSeconds(deadlineNanos - System.nanoTime()));
              insert.execute();
            }
          } else { // there is a record already.
            int existingMaxReservations = rs.getInt(2);
            if (existingMaxReservations != maxNrReservations) {
              throw new RuntimeException("Semaphore " + semName + " max reservations count different "
                      + existingMaxReservations + " != " + maxNrReservations + " use different semaphore");
            }
          }
        }
      }
      return null;
    }, jdbcTimeoutSeconds, TimeUnit.SECONDS);

  }

  public static int nanosToSeconds(final long nanos) {
    long seconds = TimeUnit.NANOSECONDS.toSeconds(nanos);
    if (seconds > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    } else {
      return (int) seconds;
    }
  }

  

  @SuppressFBWarnings("UW_UNCOND_WAIT")
  public boolean acquire(final long timeout, final TimeUnit unit) throws InterruptedException, SQLException {
    long deadlineNanos = System.nanoTime() + unit.toNanos(timeout);
    boolean acquired = false;
    do {
      acquired = jdbc.transactOnConnection(new HandlerNano<Connection, Boolean, SQLException>() {
        @Override
        public Boolean handle(final Connection conn, final long deadlineNanos) throws SQLException {
          try (PreparedStatement stmt = conn.prepareStatement(acquireSql)) {
            stmt.setQueryTimeout(nanosToSeconds(deadlineNanos - System.nanoTime()));
            stmt.setNString(1, FileBasedLock.getContextInfo());
            if (stmt.execute()) {
              throw new RuntimeException("Statement must be a update statement and not " + acquireSql);
            }
            int rowsUpdated = stmt.getUpdateCount();
            if (rowsUpdated == 1) {
              return Boolean.TRUE;
            } else {
              return Boolean.FALSE;
            }
          }
        }
      }, timeout, unit);
      if (!acquired) {
        synchronized (semName) {
          semName.wait(Math.min(TimeUnit.NANOSECONDS.toMillis(deadlineNanos - System.nanoTime()),
                  Math.abs(rnd.nextInt()) % 1000));
        }
      }
    } while (!acquired && deadlineNanos > System.nanoTime());
    return acquired;
  }

  public void release() throws SQLException, InterruptedException {
    jdbc.transactOnConnection(new HandlerNano<Connection, Void, SQLException>() {
      @Override
      public Void handle(final Connection conn, final long deadlineNanos) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(releaseSql)) {
          stmt.setQueryTimeout(10);
          stmt.setNString(1, FileBasedLock.getContextInfo());
          if (stmt.execute()) {
            throw new RuntimeException("Statement must be a update statement and not " + acquireSql);
          }
          int rowsUpdated = stmt.getUpdateCount();
          if (!(rowsUpdated == 1)) {
            throw new IllegalStateException("There seems to be attempt to release more than acquired "
                    + maxNrReservations);
          }
          synchronized (semName) {
            semName.notifyAll();
          }
          return null;
        }
      }
    }, jdbcTimeoutSeconds, TimeUnit.SECONDS);

  }

  @Override
  public String toString() {
    return "JdbcSemaphore{" + "jdbc=" + jdbc + ", maxNrReservations=" + maxNrReservations
            + ", jdbcTimeoutSeconds=" + jdbcTimeoutSeconds + ", semName=" + semName + '}';
  }

  
  
}
