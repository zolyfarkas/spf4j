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

import org.spf4j.concurrent.Semaphore;
import com.google.common.annotations.Beta;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.HandlerNano;
import org.spf4j.base.MutableHolder;
import org.spf4j.base.TimeSource;
import org.spf4j.concurrent.DefaultExecutor;
import org.spf4j.concurrent.LockRuntimeException;
import org.spf4j.jdbc.JdbcTemplate;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.Registry;

/**
 * A jdbc table based semaphore implementation. Similar with a semaphore implemented with zookeeper, we rely on
 * heartbeats to detect dead members. If you have a zookeeper instance accessible you should probably use a semaphore
 * implemented with it... If you are already connecting to a database. this should be a reliable and low overhead (no
 * calls from DBA) implementation. (at leat that is my goal) Using a crappy database will give you crappy results.
 *
 * There are 3 tables involved:
 *
 * SEMAPHORES - keep track of available and total permits by semaphore.
 * PERMITS_BY_OWNER - keeps track of all permits by
 * owner.
 * HEARTBEATS - keeps heartbeats by owner to detect - dead owners.
 *
 * All table names and columns are customizable to adapt this implementation to different naming conventions.
 *
 *
 * @author zoly
 */
@SuppressFBWarnings(value = {"NP_LOAD_OF_KNOWN_NULL_VALUE", "SQL_INJECTION_JDBC",
  "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING", "PREDICTABLE_RANDOM"},
        justification = "Sql injection is not really possible since the parameterized values are"
                + "  validated to be java ids")
@Beta
public final class JdbcSemaphore implements AutoCloseable, Semaphore {

  private static final int CLEANUP_TIMEOUT_SECONDS =
          Integer.getInteger("spf4j.jdbc.semaphore.cleanupTimeoutSeconds", 60);

  private static final Logger LOG = LoggerFactory.getLogger(JdbcSemaphore.class);

  private static final ConcurrentMap<String, Object> SYNC_OBJS = new ConcurrentHashMap<>();

  private final JdbcTemplate jdbc;

  private final String permitsSql;

  private final String ownedPermitsSql;

  private final String totalPermitsSql;

  private final String reducePermitsSql;

  private final String increasePermitsSql;

  private final String updatePermitsSql;

  private final String acquireSql;

  private final String acquireByOwnerSql;

  private final String releaseSql;

  private final String releaseByOwnerSql;

  private final String deleteDeadOwnerRecordsSql;

  private final String getDeadOwnerPermitsSql;

  private final String deleteDeadOwerRecordSql;

  private final String insertLockRowSql;

  private final String insertPermitsByOwnerSql;

  private final int jdbcTimeoutSeconds;

  private final String semName;

  private final Object syncObj;

  private final JdbcHeartBeat heartBeat;

  private volatile boolean isHealthy;

  private Error heartBeatFailure;

  private final int acquirePollMillis;

  private final JdbcHeartBeat.LifecycleHook failureHook;

  private int ownedReservations;

  /**
   * @param dataSource  the jdbc data source with the Semaphores table. Please be sensible, no "test on borrow" pools.
   * @param semaphoreName  number of initial permits, if semaphore already exists the existing nr of permits is kept.
   * @param nrPermits  the number of initial permits.
   */
  public JdbcSemaphore(final DataSource dataSource, final String semaphoreName, final int nrPermits)
          throws InterruptedException, SQLException {
    this(dataSource, semaphoreName, nrPermits, false);
  }

  /**
   * create a JDBC Semaphore. create one instance / process.
   *
   * @param dataSource  the data source to use for sync.
   * @param semaphoreName  the semaphore name.
   * @param nrPermits  number of initial permits.
   * @param strict  if true, if semaphore already exists and the total permits is different that param nrPermits an
   * IllegalArgumentException will be thrown.
   */
  public JdbcSemaphore(final DataSource dataSource, final String semaphoreName,
          final int nrPermits, final boolean strict) throws InterruptedException, SQLException {
    this(dataSource, SemaphoreTablesDesc.DEFAULT, semaphoreName, nrPermits,
            Integer.getInteger("spf4j.jdbc.semaphore.jdbcTimeoutSeconds", 10), strict);
  }

  public JdbcSemaphore(final DataSource dataSource, final SemaphoreTablesDesc semTableDesc,
          final String semaphoreName, final int nrPermits, final int jdbcTimeoutSeconds,
          final boolean strictReservations) throws InterruptedException, SQLException {
    this(dataSource, semTableDesc, semaphoreName, nrPermits, jdbcTimeoutSeconds, strictReservations,
            Integer.getInteger("spf4j.jdbc.semaphore.defaultMaxPollIntervalMillis", 1000));
  }


  @SuppressFBWarnings({"CBX_CUSTOM_BUILT_XML", "STT_TOSTRING_STORED_IN_FIELD"}) // no sql builder (yet)
  public JdbcSemaphore(final DataSource dataSource, final SemaphoreTablesDesc semTableDesc,
          final String semaphoreName, final int nrPermits, final int jdbcTimeoutSeconds,
          final boolean strictReservations, final int acquirePollMillis) throws InterruptedException, SQLException {
    if (nrPermits < 0) {
      throw new IllegalArgumentException("Permits must be positive and not " + nrPermits);
    }
    this.acquirePollMillis = acquirePollMillis;
    this.semName = semaphoreName;
    this.syncObj = SYNC_OBJS.computeIfAbsent(semaphoreName, (key) -> new Object());
    this.jdbcTimeoutSeconds = jdbcTimeoutSeconds;
    this.jdbc = new JdbcTemplate(dataSource);
    this.isHealthy = true;
    this.ownedReservations = 0;
    this.failureHook = new JdbcHeartBeat.LifecycleHook() {
      @Override
      public void onError(final Error error) {
        heartBeatFailure = error;
        isHealthy = false;
      }

      @Override
      public void onClose() {
          close();
      }
    };
    this.heartBeat = JdbcHeartBeat.getHeartBeatAndSubscribe(dataSource,
            semTableDesc.getHeartBeatTableDesc(), failureHook);
    final String semaphoreTableName = semTableDesc.getSemaphoreTableName();
    String availablePermitsColumn = semTableDesc.getAvailablePermitsColumn();
    String lastModifiedByColumn = semTableDesc.getLastModifiedByColumn();
    String lastModifiedAtColumn = semTableDesc.getLastModifiedAtColumn();
    String ownerColumn = semTableDesc.getOwnerColumn();
    String semaphoreNameColumn = semTableDesc.getSemNameColumn();
    String totalPermitsColumn = semTableDesc.getTotalPermitsColumn();
    String ownerPermitsColumn = semTableDesc.getOwnerPermitsColumn();
    String permitsByOwnerTableName = semTableDesc.getPermitsByOwnerTableName();
    HeartBeatTableDesc hbTableDesc = heartBeat.getHbTableDesc();
    String heartBeatTableName = hbTableDesc.getTableName();
    String heartBeatOwnerColumn = hbTableDesc.getOwnerColumn();
    String currentTimeMillisFunc = hbTableDesc.getDbType().getCurrTSSqlFn();

    this.reducePermitsSql = "UPDATE " + semaphoreTableName + " SET "
            + totalPermitsColumn + " = " + totalPermitsColumn + " - ?, "
            + availablePermitsColumn + " = " + availablePermitsColumn + " - ?, "
            + lastModifiedByColumn + " = ?, " + lastModifiedAtColumn + " = " + currentTimeMillisFunc + " WHERE "
            + semaphoreNameColumn + " = ? AND "
            + totalPermitsColumn + " >= ?";

    this.increasePermitsSql = "UPDATE " + semaphoreTableName + " SET "
            + totalPermitsColumn + " = " + totalPermitsColumn + " + ?, "
            + availablePermitsColumn + " = " + availablePermitsColumn + " + ?, "
            + lastModifiedByColumn + " = ?, " + lastModifiedAtColumn + " = " + currentTimeMillisFunc + " WHERE "
            + semaphoreNameColumn + " = ? ";

    this.updatePermitsSql = "UPDATE " + semaphoreTableName + " SET "
            + totalPermitsColumn + " =  ?, "
            + availablePermitsColumn + " =  " + availablePermitsColumn + " + ? - " + totalPermitsColumn + ','
            + lastModifiedByColumn + " = ?, " + lastModifiedAtColumn + " = " + currentTimeMillisFunc + " WHERE "
            + semaphoreNameColumn + " = ?";

    this.acquireSql = "UPDATE " + semaphoreTableName + " SET "
            + availablePermitsColumn + " = " + availablePermitsColumn + " - ?, "
            + lastModifiedByColumn + " = ?, " + lastModifiedAtColumn + " = " + currentTimeMillisFunc + " WHERE "
            + semaphoreNameColumn + " = ? AND "
            + availablePermitsColumn + " >= ?";
    this.acquireByOwnerSql = "UPDATE " + permitsByOwnerTableName
            + " SET " + ownerPermitsColumn + " = " + ownerPermitsColumn + " + ?, "
            + lastModifiedAtColumn + " = " + currentTimeMillisFunc + " WHERE "
            + ownerColumn + " = ? AND " + semaphoreNameColumn + " = ?";

    this.releaseSql = "UPDATE " + semaphoreTableName + " SET "
            + availablePermitsColumn + " = CASE WHEN "
            + availablePermitsColumn + " + ? > " + totalPermitsColumn
            + " THEN " + totalPermitsColumn + " ELSE " + availablePermitsColumn + " + ? END, "
            + lastModifiedByColumn + " = ?, " + lastModifiedAtColumn + " = " + currentTimeMillisFunc + " WHERE "
            + semaphoreNameColumn + " = ?";

    this.releaseByOwnerSql = "UPDATE " + permitsByOwnerTableName
            + " SET " + ownerPermitsColumn + " = " + ownerPermitsColumn
            + " - ?, " + lastModifiedAtColumn + " = " + currentTimeMillisFunc + " WHERE "
            + ownerColumn + " = ? AND " + semaphoreNameColumn + " = ? and " + ownerPermitsColumn + " >= ?";

    this.permitsSql = "SELECT " + availablePermitsColumn + ',' + totalPermitsColumn
            + " FROM " + semaphoreTableName
            + " WHERE " + semaphoreNameColumn + " = ?";

    this.totalPermitsSql = "SELECT " + totalPermitsColumn + " FROM " + semaphoreTableName
            + " WHERE " + semTableDesc.getSemNameColumn() + " = ?";

    this.ownedPermitsSql = "SELECT " + ownerPermitsColumn + " FROM "
            + permitsByOwnerTableName + " WHERE "
            + ownerColumn + " = ? AND " + semaphoreNameColumn + " = ?";

    this.deleteDeadOwnerRecordsSql = "DELETE FROM " + permitsByOwnerTableName + " RO "
            + "WHERE RO." + semaphoreNameColumn + " = ? AND " + ownerPermitsColumn + " = 0 AND "
            + "NOT EXISTS (select H." + heartBeatOwnerColumn + " from " + heartBeatTableName
            + " H where H." + heartBeatOwnerColumn + " = RO." + ownerColumn + ')';

    this.getDeadOwnerPermitsSql = "SELECT " + ownerColumn + ", " + ownerPermitsColumn
            + " FROM " + permitsByOwnerTableName + " RO "
            + "WHERE RO." + semaphoreNameColumn + " = ? AND  " + ownerPermitsColumn + " > 0 AND "
            + "NOT EXISTS (select H." + heartBeatOwnerColumn + " from " + heartBeatTableName
            + " H where H." + heartBeatOwnerColumn + " = RO." + ownerColumn
            + ") ORDER BY " + ownerColumn + ',' + ownerPermitsColumn;

    this.deleteDeadOwerRecordSql = "DELETE FROM " + permitsByOwnerTableName + " WHERE "
            + ownerColumn + " = ? AND " + semaphoreNameColumn + " = ? AND "
            + ownerPermitsColumn + " = ?";

    this.insertLockRowSql = "insert into " + semaphoreTableName
                    + " (" + semaphoreNameColumn + ',' + availablePermitsColumn + ',' + totalPermitsColumn
                    + ',' + lastModifiedByColumn + ',' + lastModifiedAtColumn + ") VALUES (?, ?, ?, ?, "
                    + currentTimeMillisFunc + ')';

    this.insertPermitsByOwnerSql = "insert into " + permitsByOwnerTableName
              + " (" + semaphoreNameColumn + ',' + ownerColumn + ',' + ownerPermitsColumn + ','
              + lastModifiedAtColumn + ") VALUES (?, ?, ?, " + currentTimeMillisFunc + ")";


    try {
      createLockRowIfNotPresent(strictReservations, nrPermits);
    } catch (SQLIntegrityConstraintViolationException ex) {
      try {
        // RACE condition while creating the row, will retry to validate if everything is OK.
        createLockRowIfNotPresent(strictReservations, nrPermits);
      } catch (SQLException ex1) {
        ex1.addSuppressed(ex);
        throw ex1;
      }
    }
    createOwnerRow();
  }

  public void registerJmx() {
    Registry.export(JdbcSemaphore.class.getName(), semName, this);
  }

  public void unregisterJmx() {
    Registry.unregister(JdbcSemaphore.class.getName(), semName);
  }

  private void validate() {
    if (!isHealthy) {
      throw new IllegalStateException("Heartbeats failed! semaphore broken " + this, heartBeatFailure);
    }
  }

  private void createLockRowIfNotPresent(final boolean strictReservations, final int nrPermits)
          throws SQLException, InterruptedException {
    jdbc.transactOnConnection((final Connection conn, final long deadlineNanos) -> {
    try (PreparedStatement stmt = conn.prepareStatement(permitsSql)) {
        stmt.setNString(1, semName);
        stmt.setQueryTimeout(JdbcTemplate.getTimeoutToDeadlineSeconds(deadlineNanos));
        try (ResultSet rs = stmt.executeQuery()) {
          if (!rs.next()) {
            try (PreparedStatement insert = conn.prepareStatement(insertLockRowSql)) {
              insert.setNString(1, semName);
              insert.setInt(2, nrPermits);
              insert.setInt(3, nrPermits);
              insert.setNString(4, org.spf4j.base.Runtime.PROCESS_ID);
              insert.setQueryTimeout(JdbcTemplate.getTimeoutToDeadlineSeconds(deadlineNanos));
              insert.executeUpdate();
            }
          } else if (strictReservations) { // there is a record already. for now blow up if different nr reservations.
            int existingMaxReservations = rs.getInt(2);
            if (existingMaxReservations != nrPermits) {
              throw new IllegalArgumentException("Semaphore " + semName + " max reservations count different "
                      + existingMaxReservations + " != " + nrPermits + " use different semaphore");
            }
            if (rs.next()) {
              throw new IllegalStateException("Cannot have mutiple semaphores with the same name " + semName);
            }
          } else if (rs.next()) {
            throw new IllegalStateException("Cannot have mutiple semaphores with the same name " + semName);
          }
        }
      }
      return null;
    }, jdbcTimeoutSeconds, TimeUnit.SECONDS);
  }

  private void createOwnerRow()
          throws SQLException, InterruptedException {

    jdbc.transactOnConnection((final Connection conn, final long deadlineNanos) -> {

      try (PreparedStatement insert = conn.prepareStatement(insertPermitsByOwnerSql)) {
        insert.setNString(1, this.semName);
        insert.setNString(2, org.spf4j.base.Runtime.PROCESS_ID);
        insert.setInt(3, 0);
        insert.setQueryTimeout(JdbcTemplate.getTimeoutToDeadlineSeconds(deadlineNanos));
        insert.executeUpdate();
      }
      return null;
    }, jdbcTimeoutSeconds, TimeUnit.SECONDS);
  }

  @SuppressFBWarnings("UW_UNCOND_WAIT")
  @CheckReturnValue
  @Override
  public boolean tryAcquire(final int nrPermits, final long timeout, final TimeUnit unit)
          throws InterruptedException {
    if (nrPermits < 1) {
      throw new IllegalArgumentException("You should try to acquire something! not " + nrPermits);
    }
    if (timeout <= 0) {
      throw new IllegalArgumentException("Illegal timeout, please reasonable values, and not: " + timeout);
    }
    synchronized (syncObj) {
      long toNanos = unit.toNanos(timeout);
      long deadlineNanos;
      if (toNanos < 0) {
        deadlineNanos = Long.MAX_VALUE;
      } else {
        deadlineNanos = TimeSource.nanoTime() + toNanos;
        if (deadlineNanos < 0) { //Overflow
          deadlineNanos = Long.MAX_VALUE;
        }
      }
      boolean acquired = false;
      final MutableHolder<Boolean> beat = MutableHolder.of(Boolean.FALSE);
      do {
        validate();
        try {
          acquired = jdbc.transactOnConnection(new HandlerNano<Connection, Boolean, SQLException>() {
            @Override
            public Boolean handle(final Connection conn, final long deadlineNanos) throws SQLException {
              try (PreparedStatement stmt = conn.prepareStatement(acquireSql)) {
                stmt.setQueryTimeout(Math.min(JdbcTemplate.getTimeoutToDeadlineSeconds(deadlineNanos),
                        jdbcTimeoutSeconds));
                stmt.setInt(1, nrPermits);
                stmt.setNString(2, org.spf4j.base.Runtime.PROCESS_ID);
                stmt.setNString(3, semName);
                stmt.setInt(4, nrPermits);
                int rowsUpdated = stmt.executeUpdate();
                Boolean acquired;
                if (rowsUpdated == 1) {
                  try (PreparedStatement ostmt = conn.prepareStatement(acquireByOwnerSql)) {
                    ostmt.setInt(1, nrPermits);
                    ostmt.setNString(2, org.spf4j.base.Runtime.PROCESS_ID);
                    ostmt.setNString(3, semName);
                    ostmt.setQueryTimeout(Math.min(JdbcTemplate.getTimeoutToDeadlineSeconds(deadlineNanos),
                            jdbcTimeoutSeconds));
                    int nrUpdated = ostmt.executeUpdate();
                    if (nrUpdated != 1) {
                      throw new IllegalStateException("Updated " + nrUpdated + " is incorrect for " + ostmt);
                    }
                  }
                  acquired = Boolean.TRUE;
                } else {
                  if (rowsUpdated > 1) {
                    throw new IllegalStateException("Too many rows updated! when trying to acquire " + nrPermits);
                  }
                  acquired = Boolean.FALSE;
                }
                long currNanoTime = TimeSource.nanoTime();
                if (deadlineNanos - currNanoTime > heartBeat.getBeatDurationNanos()) {
                  // do a heartbeat if have time, and if it makes sense.
                  beat.setValue(heartBeat.tryBeat(conn, currNanoTime, deadlineNanos));
                }
                return acquired;
              }
            }
          }, timeout, unit);
        } catch (SQLTimeoutException ex) {
          return false;
        } catch (SQLException ex) {
          throw new LockRuntimeException(ex);
        }
        if (beat.getValue()) { // we did a heartbeat as part of the acquisition.
          heartBeat.updateLastRunNanos(TimeSource.nanoTime());
        }
        if (!acquired) {
          long secondsLeft = JdbcTemplate.getTimeoutToDeadlineSecondsNoEx(deadlineNanos);
          if (secondsLeft < 0) {
            return false;
          }
          if (secondsLeft < CLEANUP_TIMEOUT_SECONDS) {
            Future<Integer> fut = DefaultExecutor.INSTANCE.submit(
                    () -> removeDeadHeartBeatAndNotOwnerRows(CLEANUP_TIMEOUT_SECONDS));
            try {
              fut.get(secondsLeft, TimeUnit.SECONDS);
            } catch (TimeoutException ex) {
              //removing dead entries did not finish in time, but continues in the background.
              break;
            } catch (ExecutionException ex) {
              throw new LockRuntimeException(ex);
            }
          } else {
            try {
              removeDeadHeartBeatAndNotOwnerRows(secondsLeft);
            } catch (SQLTimeoutException ex) {
              return false;
            } catch (SQLException ex) {
              throw new LockRuntimeException(ex);
            }
          }
          try {
            if (releaseDeadOwnerPermits(nrPermits) <= 0) { //wait of we did not find anything dead to release.
              long wtimeMilis = Math.min(TimeUnit.NANOSECONDS.toMillis(deadlineNanos - TimeSource.nanoTime()),
                      ThreadLocalRandom.current().nextLong(acquirePollMillis));
              if (wtimeMilis > 0) {
                syncObj.wait(wtimeMilis);
              } else {
                break;
              }
            }
          } catch (SQLException ex) {
            throw new LockRuntimeException(ex);
          }

        }
      } while (!acquired && deadlineNanos > TimeSource.nanoTime());
      if (acquired) {
        ownedReservations += nrPermits;
      }
      return acquired;
    }
  }


  @Override
  @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
  public void release(final int nrReservations) {
    synchronized (syncObj) {
      try {
        jdbc.transactOnConnectionNonInterrupt(new HandlerNano<Connection, Void, SQLException>() {
          @Override
          public Void handle(final Connection conn, final long deadlineNanos) throws SQLException {
            releaseReservations(conn, deadlineNanos, nrReservations);
            try (PreparedStatement ostmt = conn.prepareStatement(releaseByOwnerSql)) {
              ostmt.setInt(1, nrReservations);
              ostmt.setNString(2, org.spf4j.base.Runtime.PROCESS_ID);
              ostmt.setNString(3, semName);
              ostmt.setInt(4, nrReservations);
              ostmt.setQueryTimeout(Math.min(JdbcTemplate.getTimeoutToDeadlineSeconds(deadlineNanos),
                      jdbcTimeoutSeconds));
              int nrUpdated = ostmt.executeUpdate();
              if (nrUpdated != 1) {
                throw new IllegalStateException("Trying to release more than you own! " + ostmt);
              }
            }
            return null;
          }
        }, jdbcTimeoutSeconds, TimeUnit.SECONDS);
      } catch (SQLException ex) {
        throw new LockRuntimeException(ex);
      }
      ownedReservations -= nrReservations;
      if (ownedReservations < 0) {
        throw new IllegalStateException("Should not be trying to release more than you acquired!" + nrReservations);
      }
      syncObj.notifyAll();
    }
  }

  public void releaseAll() {
    synchronized (syncObj) {
      release(ownedReservations);
    }
  }

  private void releaseReservations(final Connection conn, final long deadlineNanos, final int nrReservations)
          throws SQLException {
    try (PreparedStatement stmt = conn.prepareStatement(releaseSql)) {
      stmt.setQueryTimeout(Math.min(JdbcTemplate.getTimeoutToDeadlineSeconds(deadlineNanos),
              jdbcTimeoutSeconds));
      stmt.setInt(1, nrReservations);
      stmt.setInt(2, nrReservations);
      stmt.setNString(3, org.spf4j.base.Runtime.PROCESS_ID);
      stmt.setNString(4, semName);
      stmt.executeUpdate(); // Since a release might or might not update a row.
    }
  }

  @JmxExport(description = "Get the available semaphore permits")
  public int availablePermits() throws SQLException, InterruptedException {
    return jdbc.transactOnConnection((final Connection conn, final long deadlineNanos) -> {
      try (PreparedStatement stmt = conn.prepareStatement(permitsSql)) {
        stmt.setNString(1, semName);
        stmt.setQueryTimeout(JdbcTemplate.getTimeoutToDeadlineSeconds(deadlineNanos));
        try (ResultSet rs = stmt.executeQuery()) {
          if (!rs.next()) {
            throw new IllegalStateException();
          } else {
            int result = rs.getInt(1);
            if (rs.next()) {
              throw new IllegalStateException();
            }
            return result;
          }
        }
      }
    }, jdbcTimeoutSeconds, TimeUnit.SECONDS);
  }

  @JmxExport(description = "get the number of permits owned by this process")
  public int permitsOwned() throws SQLException, InterruptedException {
    return jdbc.transactOnConnection((final Connection conn, final long deadlineNanos) -> {
      try (PreparedStatement stmt = conn.prepareStatement(ownedPermitsSql)) {
        stmt.setNString(1, org.spf4j.base.Runtime.PROCESS_ID);
        stmt.setNString(2, semName);
        stmt.setQueryTimeout(JdbcTemplate.getTimeoutToDeadlineSeconds(deadlineNanos));
        try (ResultSet rs = stmt.executeQuery()) {
          if (!rs.next()) {
            throw new IllegalStateException();
          } else {
            int result = rs.getInt(1);
            if (rs.next()) {
              throw new IllegalStateException();
            }
            return result;
          }
        }
      }
    }, jdbcTimeoutSeconds, TimeUnit.SECONDS);
  }

  @JmxExport(description = "Get the total permits this semaphore can hand out")
  public int totalPermits() throws SQLException, InterruptedException {
    return jdbc.transactOnConnection((final Connection conn, final long deadlineNanos) -> {
      try (PreparedStatement stmt = conn.prepareStatement(totalPermitsSql)) {
        stmt.setNString(1, semName);
        stmt.setQueryTimeout(JdbcTemplate.getTimeoutToDeadlineSeconds(deadlineNanos));
        try (ResultSet rs = stmt.executeQuery()) {
          if (!rs.next()) {
            throw new IllegalStateException();
          } else {
            return rs.getInt(1);
          }
        }
      }
    }, jdbcTimeoutSeconds, TimeUnit.SECONDS);
  }

  @JmxExport(description = "get a list of all dead owners which hold permits")
  @Nonnull
  public List<OwnerPermits> getDeadOwnerPermits(final int wishPermits) throws SQLException, InterruptedException {
    return jdbc.transactOnConnection((final Connection conn, final long deadlineNanos) -> {
      return getDeadOwnerPermits(conn, deadlineNanos, wishPermits);
    }, jdbcTimeoutSeconds, TimeUnit.SECONDS);
  }

  List<OwnerPermits> getDeadOwnerPermits(final Connection conn, final long deadlineNanos, final int wishPermits)
          throws SQLException {
    List<OwnerPermits> result = new ArrayList<>();
    try (PreparedStatement stmt = conn.prepareStatement(getDeadOwnerPermitsSql)) {
      stmt.setNString(1, semName);
      stmt.setQueryTimeout(JdbcTemplate.getTimeoutToDeadlineSeconds(deadlineNanos));
      try (ResultSet rs = stmt.executeQuery()) {
        int nrPermits = 0;
        while (rs.next()) {
          OwnerPermits ownerPermit = new OwnerPermits(rs.getNString(1), rs.getInt(2));
          result.add(ownerPermit);
          nrPermits += ownerPermit.getNrPermits();
          if (nrPermits >= wishPermits) {
            break;
          }
        }
      }
    }
    return result;
  }

  /**
   * Attempts to release permits for this semaphore owned by dead owners.
   *
   * @param wishPermits - How many permits we would like to get released.
   * @return - the number of permits we actually released.
   * @throws SQLException - something went wrong with the db.
   * @throws InterruptedException - thrown if thread is interrupted.
   */
  @JmxExport(description = "release dead owner permits")
  @CheckReturnValue
  public int releaseDeadOwnerPermits(@JmxExport(value = "wishPermits",
          description = "how many we whish to release") final int wishPermits)
          throws InterruptedException, SQLException {
      return jdbc.transactOnConnection((final Connection conn, final long deadlineNanos) -> {
        List<OwnerPermits> deadOwnerPermits = getDeadOwnerPermits(conn, deadlineNanos, wishPermits);
        int released = 0;
        for (OwnerPermits permit : deadOwnerPermits) {
          try (PreparedStatement stmt = conn.prepareStatement(deleteDeadOwerRecordSql)) {
            String owner = permit.getOwner();
            stmt.setNString(1, owner);
            stmt.setNString(2, semName);
            int nrPermits = permit.getNrPermits();
            stmt.setInt(3, nrPermits);
            stmt.setQueryTimeout(JdbcTemplate.getTimeoutToDeadlineSeconds(deadlineNanos));
            if (stmt.executeUpdate() == 1) { // I can release! if not somebody else is doing it.
              released += nrPermits;
              releaseReservations(conn, deadlineNanos, nrPermits);
              LOG.warn("Released {} reservations from dead owner {}", nrPermits, owner);
            }
          }
        }
        return released;
      }, jdbcTimeoutSeconds, TimeUnit.SECONDS);
  }

  @JmxExport(description = "Change the total available permits to the provided number")
  public void updatePermits(final int nrPermits) throws SQLException, InterruptedException {
    if (nrPermits < 0) {
      throw new IllegalArgumentException("Permits must be positive and not " + nrPermits);
    }
    jdbc.transactOnConnection(new HandlerNano<Connection, Void, SQLException>() {
      @Override
      public Void handle(final Connection conn, final long deadlineNanos) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(updatePermitsSql)) {
          stmt.setQueryTimeout(Math.min(JdbcTemplate.getTimeoutToDeadlineSeconds(deadlineNanos),
                  jdbcTimeoutSeconds));
          stmt.setInt(1, nrPermits);
          stmt.setInt(2, nrPermits);
          stmt.setNString(3, org.spf4j.base.Runtime.PROCESS_ID);
          stmt.setNString(4, semName);
          int rowsUpdated = stmt.executeUpdate();
          if (rowsUpdated != 1) {
            throw new IllegalArgumentException("Cannot reduce nr total permits by " + nrPermits);
          }
        }
        return null;
      }
    }, jdbcTimeoutSeconds, TimeUnit.SECONDS);
  }

  @JmxExport(description = "Reduce the total available permits by the provided number")
  public void reducePermits(final int nrPermits) throws SQLException, InterruptedException {
    jdbc.transactOnConnection(new HandlerNano<Connection, Void, SQLException>() {
      @Override
      public Void handle(final Connection conn, final long deadlineNanos) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(reducePermitsSql)) {
          stmt.setQueryTimeout(Math.min(JdbcTemplate.getTimeoutToDeadlineSeconds(deadlineNanos),
                  jdbcTimeoutSeconds));
          stmt.setInt(1, nrPermits);
          stmt.setInt(2, nrPermits);
          stmt.setNString(3, org.spf4j.base.Runtime.PROCESS_ID);
          stmt.setNString(4, semName);
          stmt.setInt(5, nrPermits);
          int rowsUpdated = stmt.executeUpdate();
          if (rowsUpdated != 1) {
            throw new IllegalArgumentException("Cannot reduce nr total permits by " + nrPermits);
          }
        }
        return null;
      }
    }, jdbcTimeoutSeconds, TimeUnit.SECONDS);
  }

  @JmxExport(description = "Increase the total available permits by the provided number")
  public void increasePermits(final int nrPermits) throws SQLException, InterruptedException {
    jdbc.transactOnConnection(new HandlerNano<Connection, Void, SQLException>() {
      @Override
      public Void handle(final Connection conn, final long deadlineNanos) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(increasePermitsSql)) {
          stmt.setQueryTimeout(Math.min(JdbcTemplate.getTimeoutToDeadlineSeconds(deadlineNanos),
                  jdbcTimeoutSeconds));
          stmt.setInt(1, nrPermits);
          stmt.setInt(2, nrPermits);
          stmt.setNString(3, org.spf4j.base.Runtime.PROCESS_ID);
          stmt.setNString(4, semName);
          int rowsUpdated = stmt.executeUpdate();
          if (rowsUpdated != 1) {
            throw new IllegalArgumentException("Cannot reduce nr total permits by " + nrPermits);
          }
        }
        return null;
      }
    }, jdbcTimeoutSeconds, TimeUnit.SECONDS);
  }

  public int removeDeadHeartBeatAndNotOwnerRows(final long timeoutSeconds) throws SQLException, InterruptedException {
    return jdbc.transactOnConnection(new HandlerNano<Connection, Integer, SQLException>() {
      @Override
      public Integer handle(final Connection conn, final long deadlineNanos) throws SQLException {
        return removeDeadHeartBeatAndNotOwnerRows(conn, deadlineNanos);
      }
    }, timeoutSeconds, TimeUnit.SECONDS);
  }

  private int removeDeadHeartBeatAndNotOwnerRows(final Connection conn, final long deadlineNanos) throws SQLException {
    int removedDeadHeartBeatRows = this.heartBeat.removeDeadHeartBeatRows(conn, deadlineNanos);
    if (removedDeadHeartBeatRows > 0) {
      return removeDeadNotOwnedRowsOnly(conn, deadlineNanos);
    } else {
      return 0;
    }
  }

  private int removeDeadNotOwnedRowsOnly(final Connection conn, final long deadlineNanos) throws SQLException {
    try (PreparedStatement stmt = conn.prepareStatement(deleteDeadOwnerRecordsSql)) {
      stmt.setNString(1, semName);
      stmt.setQueryTimeout(JdbcTemplate.getTimeoutToDeadlineSeconds(deadlineNanos));
      return stmt.executeUpdate();
    }
  }

  @Override
  public String toString() {
    return "JdbcSemaphore{" + "jdbc=" + jdbc
            + ", jdbcTimeoutSeconds=" + jdbcTimeoutSeconds + ", semName=" + semName + '}';
  }

  @Override
  public void close() {
    releaseAll();
    unregisterJmx();
    this.heartBeat.removeLifecycleHook(failureHook);
    isHealthy = false;
  }

  @JmxExport
  public int getJdbcTimeoutSeconds() {
    return jdbcTimeoutSeconds;
  }

  @JmxExport
  public boolean isIsHealthy() {
    return isHealthy;
  }


}
