/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
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
 */


package org.spf4j.concurrent.jdbc;

import com.google.common.util.concurrent.ListenableScheduledFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.ThreadSafe;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.HandlerNano;
import org.spf4j.base.Iterables;
import org.spf4j.base.Throwables;
import org.spf4j.concurrent.DefaultExecutor;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.jdbc.JdbcTemplate;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.Registry;

/**
 * A class that does "heartbeats" (at a arbitrary inteval) to a database table.
 * This is to detect the death of a process.
 * The process is considered dead when: currentTime - lastheartbeat > beatInterval * 2
 * When this class mechanism detects that it cannot perform the heartbeats it throws a Error.
 * The sensible this for the process is to go down (and restart if it is a daemon).
 *
 * @author zoly
 */
@SuppressFBWarnings(value = {"SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING",
  "PMB_POSSIBLE_MEMORY_BLOAT"}, justification = "The db object names are configurable,"
        + "we for know allow heartbeats to multiple data sources, should be one mostly")
@ParametersAreNonnullByDefault
@ThreadSafe
public final class JdbcHeartBeat implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(JdbcHeartBeat.class);

  private final JdbcTemplate jdbc;

  private final String updateHeartbeatSql;

  private final int jdbcTimeoutSeconds;

  private final long intervalMillis;

  private final HeartBeatTableDesc hbTableDesc;

  private final String deleteSql;

  private final String deleteHeartBeatSql;

  private volatile long lastRun;

  private boolean isClosed;

  private ListenableScheduledFuture<?> scheduledHearbeat;

  private final long beatDurationNanos;

  @Override
  public void close() throws SQLException {
    boolean weClosed = false;
    synchronized (jdbc) {
      if (!isClosed) {
        weClosed = true;
        isClosed = true;
      }
    }
    if (weClosed) {
        unregisterJmx();
        ScheduledFuture<?> running = scheduledHearbeat;
        if (running != null) {
          scheduledHearbeat.cancel(true);
        }
        removeHeartBeatRow(jdbcTimeoutSeconds);
        for (LifecycleHook hook : lifecycleHooks) {
          hook.onClose();
        }
    }
  }

  public interface LifecycleHook {

    void onError(final Error error);

    void onClose() throws SQLException;

  }

  private final List<LifecycleHook> lifecycleHooks;

  private JdbcHeartBeat(final DataSource dataSource, final long intervalMillis,
          final int jdbcTimeoutSeconds) throws InterruptedException {
    this(dataSource, new HeartBeatTableDesc("HEARTBEATS",
            "OWNER", "INTERVAL_MILLIS", "LAST_HEARTBEAT_INSTANT_MILLIS",
            "TIMESTAMPDIFF('MILLISECOND', timestamp '1970-01-01 00:00:00', CURRENT_TIMESTAMP())"),
            intervalMillis, jdbcTimeoutSeconds);
  }

  private JdbcHeartBeat(final DataSource dataSource, final HeartBeatTableDesc hbTableDesc, final long intervalMillis,
          final int jdbcTimeoutSeconds) throws InterruptedException {
    if (intervalMillis < 1000) {
      throw new IllegalArgumentException("The heartbeat interval should be at least 1s and not "
              + intervalMillis + " ms");
    }

    this.jdbc = new JdbcTemplate(dataSource);
    this.jdbcTimeoutSeconds = jdbcTimeoutSeconds;
    this.intervalMillis = intervalMillis;
    this.hbTableDesc = hbTableDesc;
    this.isClosed = false;
    String hbTableName = hbTableDesc.getTableName();
    String lastHeartbeatColumn = hbTableDesc.getLastHeartbeatColumn();
    String currentTimeMillisFunc = hbTableDesc.getCurrentTimeMillisFunc();
    String intervalColumn = hbTableDesc.getIntervalColumn();
    String ownerColumn = hbTableDesc.getOwnerColumn();

    this.updateHeartbeatSql = "UPDATE " + hbTableName + " SET " + lastHeartbeatColumn + " = " + currentTimeMillisFunc
            + " WHERE " + ownerColumn + " = ? AND " + lastHeartbeatColumn + " + " + intervalColumn
            + " * 2 > " + currentTimeMillisFunc;
    this.deleteHeartBeatSql = "DELETE FROM " + hbTableName
            + " WHERE " + ownerColumn + " = ?";
    this.deleteSql = "DELETE FROM " + hbTableName + " WHERE " + lastHeartbeatColumn + " + " + intervalColumn
            + " * 2 < " + currentTimeMillisFunc;
    this.lifecycleHooks = new CopyOnWriteArrayList<>();
    long startTimeNanos =  System.nanoTime();
    createHeartbeatRow();
    long duration = System.nanoTime() - startTimeNanos;
    this.beatDurationNanos = Math.max(duration, TimeUnit.MILLISECONDS.toNanos(10));
  }

  public long getBeatDurationNanos() {
    return beatDurationNanos;
  }

  public void registerJmx() {
    Registry.export(this);
  }

  public void unregisterJmx() {
    Registry.unregister(this);
  }

  public void addLyfecycleHook(final LifecycleHook hook) {
    lifecycleHooks.add(hook);
  }

  public void removeLifecycleHook(final LifecycleHook hook) {
    lifecycleHooks.remove(hook);
  }

  void createHeartbeatRow() throws InterruptedException {
    try {
      jdbc.transactOnConnection((final Connection conn, final long deadlineNanos) -> {

        try (final PreparedStatement insert = conn.prepareStatement("insert into " + hbTableDesc.getTableName()
                + " (" + hbTableDesc.getOwnerColumn() + ',' + hbTableDesc.getIntervalColumn() + ','
                + hbTableDesc.getLastHeartbeatColumn() + ") VALUES (?, ?, "
                + hbTableDesc.getCurrentTimeMillisFunc() + ")")) {
          insert.setNString(1, org.spf4j.base.Runtime.PROCESS_ID);
          insert.setLong(2, this.intervalMillis);
          insert.setQueryTimeout((int) TimeUnit.NANOSECONDS.toSeconds(deadlineNanos - System.nanoTime()));
          insert.executeUpdate();
        }
        return null;
      }, jdbcTimeoutSeconds, TimeUnit.SECONDS);
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }

  @JmxExport(description = "Remove all dead hearbeat rows")
  public int removeDeadHeartBeatRows(@JmxExport("timeoutSeconds") final int timeoutSeconds)
          throws SQLException, InterruptedException {
    return jdbc.transactOnConnection((final Connection conn, final long deadlineNanos) -> {
      return JdbcHeartBeat.this.removeDeadHeartBeatRows(conn, deadlineNanos);
    }, timeoutSeconds, TimeUnit.SECONDS);
  }

  @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
  int removeDeadHeartBeatRows(final Connection conn, final long deadlineNanos) throws SQLException {
    try (final PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
      stmt.setQueryTimeout((int) TimeUnit.NANOSECONDS.toSeconds(deadlineNanos - System.nanoTime()));
      return stmt.executeUpdate();
    }
  }

  void removeHeartBeatRow(final int timeoutSeconds)
          throws SQLException {
    jdbc.transactOnConnectionNonInterrupt((final Connection conn, final long deadlineNanos) -> {
      try (final PreparedStatement stmt = conn.prepareStatement(deleteHeartBeatSql)) {
        stmt.setNString(1, org.spf4j.base.Runtime.PROCESS_ID);
        stmt.setQueryTimeout((int) TimeUnit.NANOSECONDS.toSeconds(deadlineNanos - System.nanoTime()));
        int nrDeleted = stmt.executeUpdate();
        if (nrDeleted != 1) {
          throw new IllegalStateException("Heartbeat rows deleted: " + nrDeleted
                  + " for " + org.spf4j.base.Runtime.PROCESS_ID);
        }
      }
      return null;
    }, timeoutSeconds, TimeUnit.SECONDS);
  }

  @JmxExport(description = "Remove all dead hearbeat rows async")
  public Future<Integer> removeDeadHeartBeatRowsAsync(final int timeoutSeconds) {
    return DefaultExecutor.INSTANCE.submit(new Callable<Integer>() {
      @Override
      public Integer call() throws SQLException, InterruptedException {
        return removeDeadHeartBeatRows(timeoutSeconds);
      }
    });
  }

  private ScheduledHeartBeat heartbeatRunnable;

  private ScheduledHeartBeat getHeartBeatRunnable() {
    if (heartbeatRunnable == null) {
      heartbeatRunnable = new ScheduledHeartBeat();
    }
    return heartbeatRunnable;
  }

  public void scheduleHeartbeat() {
    synchronized (jdbc) {
      if (isClosed) {
        throw new IllegalStateException("Heartbeater is closed " + this);
      }
      if (scheduledHearbeat == null) {
        long lr = lastRun;
        long delay;
        if (lr == 0) {
          delay = intervalMillis;
        } else if (lr > 0) {
          delay = intervalMillis - (System.currentTimeMillis() - lr);
        } else {
          throw new IllegalStateException("The end of times are upon us :-) " + lr);
        }
        scheduledHearbeat = DefaultScheduler.LISTENABLE_INSTANCE.schedule(
                getHeartBeatRunnable(), delay, TimeUnit.MILLISECONDS);
        scheduledHearbeat.addListener(() -> {
          synchronized (jdbc) {
            if (!isClosed) {
              scheduledHearbeat = null;
              scheduleHeartbeat();
            }
          }
        }, DefaultExecutor.INSTANCE);
      }
    }
  }

  @JmxExport
  public void beat() throws SQLException, InterruptedException {
    jdbc.transactOnConnection(new HandlerNano<Connection, Void, SQLException>() {
      @Override
      @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
      public Void handle(final Connection conn, final long deadlineNanos) throws SQLException {
        beat(conn, deadlineNanos);
        return null;
      }
    }, jdbcTimeoutSeconds, TimeUnit.SECONDS);
    lastRun = System.currentTimeMillis();
  }

  void beat(final Connection conn, final long deadlineNanos) throws SQLException {
    synchronized (jdbc) {
      if (isClosed) {
        throw new IllegalStateException("Heartbeater is closed " + this);
      }
    }
    try (PreparedStatement stmt = conn.prepareStatement(updateHeartbeatSql)) {
      stmt.setQueryTimeout((int) TimeUnit.NANOSECONDS.toSeconds(deadlineNanos - System.nanoTime()));
      stmt.setNString(1, org.spf4j.base.Runtime.PROCESS_ID);
      int rowsUpdated = stmt.executeUpdate();
      if (rowsUpdated != 1) {
        throw new IllegalStateException("Broken Heartbeat for "
                + org.spf4j.base.Runtime.PROCESS_ID + "sql : " + updateHeartbeatSql + " rows : " + rowsUpdated);
      }
      LOG.debug("Heart Beat for {}", org.spf4j.base.Runtime.PROCESS_ID);
    }
  }

  boolean tryBeat(final Connection conn, final long deadlineNanos) throws SQLException {
    if (System.currentTimeMillis() - lastRun > intervalMillis / 2) {
      beat(conn, deadlineNanos);
      return true;
    } else {
      return false;
    }
  }

  void updateLastRun(final long lastRunTime) {
    lastRun = lastRunTime;
  }


  @JmxExport
  public long getLastRunDB() throws SQLException, InterruptedException {
    return jdbc.transactOnConnection(new HandlerNano<Connection, Long, SQLException>() {
      @Override
      @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
      public Long handle(final Connection conn, final long deadlineNanos) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("select " + hbTableDesc.getLastHeartbeatColumn()
                + " FROM " + hbTableDesc.getTableName() + " where " + hbTableDesc.getOwnerColumn() + " = ?")) {
          stmt.setQueryTimeout(10);
          stmt.setNString(1, org.spf4j.base.Runtime.PROCESS_ID);
          try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
              long result = rs.getLong(1);
              if (rs.next()) {
                throw new IllegalStateException("Multible beats for same owner " + org.spf4j.base.Runtime.PROCESS_ID);
              }
              return result;
            } else {
              return 0L;
            }
          }
        }
      }
    }, jdbcTimeoutSeconds, TimeUnit.SECONDS);
  }

  @JmxExport
  public long getIntervalMillis() {
    return intervalMillis;
  }

  @JmxExport
  public long getLastRunOwner() {
    return lastRun;
  }

  @JmxExport
  public String getLastRunDateTimeOwner() {
    return ZonedDateTime.ofInstant(Instant.ofEpochMilli(lastRun), ZoneId.systemDefault()).toString();
  }

  private static final Map<DataSource, JdbcHeartBeat> HEARTBEATS = new IdentityHashMap<>();

  private static final int HEARTBEAT_INTERVAL_MILLIS = Integer.getInteger("spf4j.heartbeat.intervalMillis", 10000);

  private static boolean isShuttingdown = false;

  /**
   * Get a reference to the hearbeat instance.
   * @param dataSource  the datasource the hearbeat goes against.
   * @param hbTableDesc - heartbeat table description.
   * @param hook  a hook to notify when heartbeat fails.
   * @return the heartbeat instance.
   */
  public static JdbcHeartBeat getHeartBeatAndSubscribe(final DataSource dataSource,
          final HeartBeatTableDesc hbTableDesc,
          @Nullable final LifecycleHook hook) throws InterruptedException {
    JdbcHeartBeat beat;
    synchronized (HEARTBEATS) {
      if (isShuttingdown) {
        throw new IllegalStateException("Process is shutting down, no heartbeats are accepted for " + dataSource);
      }
      beat = HEARTBEATS.get(dataSource);
      if (beat == null) {
        beat = new JdbcHeartBeat(dataSource, hbTableDesc, HEARTBEAT_INTERVAL_MILLIS, 5);
        beat.registerJmx();
        beat.addLyfecycleHook(new LifecycleHook() {
          @Override
          public void onError(final Error error) {
          }

          @Override
          public void onClose() {
            synchronized (HEARTBEATS) {
              HEARTBEATS.remove(dataSource);
            }
          }
        });
        final JdbcHeartBeat fbeat = beat;
        org.spf4j.base.Runtime.queueHookAtBeginning(new Runnable() {
          @Override
          public void run() {
            synchronized (HEARTBEATS) {
              isShuttingdown = true;
            }
            try {
              fbeat.close();
            } catch (SQLException ex) {
              // logging in shutdownhooks is not reliable.
              System.err.println("WARN: Could not clean heartbeat record,"
                      + " this error can be ignored since it is a best effort attempt, detail:");
              Throwables.writeTo(ex, System.err, Throwables.Detail.STANDARD);
            }
          }
        });
        HEARTBEATS.put(dataSource, beat);
      }
    }
    if (hook != null) {
      beat.addLyfecycleHook(hook);
    }
    beat.scheduleHeartbeat();
    return beat;
  }

  public HeartBeatTableDesc getHbTableDesc() {
    return hbTableDesc;
  }

  @Override
  public String toString() {
    return "JdbcHeartBeat{" + "jdbc=" + jdbc + ", jdbcTimeoutSeconds=" + jdbcTimeoutSeconds + ", intervalMillis="
            + intervalMillis + ", hbTableDesc=" + hbTableDesc + ", lastRun=" + lastRun + '}';
  }

  private class ScheduledHeartBeat implements Runnable {

    @Override
    public void run() {
      try {
        long lr = lastRun;
        long currentTimeMillis = System.currentTimeMillis();
        if (lr != 0) {
          // not first beat.
          long millisSinceLastBeat = currentTimeMillis - lr;
          if (millisSinceLastBeat < intervalMillis / 2) {
            return;
          } else if (((intervalMillis * 2) < millisSinceLastBeat)) {
            // Unable to beat at inteval!
            Error err = new Error("System to busy to provide regular heartbeat, lastRun = " + lr
                    + ", intervalMillis = " + intervalMillis + ", currentTimeMillis = " + currentTimeMillis);

            handleError(err);
          }
        }
        beat();
      } catch (RuntimeException | SQLException | InterruptedException ex) {
        Error err = new Error("System failed heartbeat", ex);
        handleError(err);
      }
    }

    public void handleError(final Error err) throws Error {
      for (LifecycleHook hook : lifecycleHooks) {
        hook.onError(err);
      }
      RuntimeException ex = Iterables.forAll(lifecycleHooks, (final LifecycleHook t) -> {
        try {
          t.onClose();
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      });
      if (ex != null) {
        err.addSuppressed(ex);
      }
      lifecycleHooks.clear();
      throw err;
    }
  }

}
