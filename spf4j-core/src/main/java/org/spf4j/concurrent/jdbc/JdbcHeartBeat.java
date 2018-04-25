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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.base.Iterables;
import org.spf4j.base.TimeSource;
import org.spf4j.base.Timing;
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
 * This is typically done by registering a default uncaught exception handler with:
 * Thread.setDefaultUncaughtExceptionHandler
 *
 *
 * @author zoly
 */
@SuppressFBWarnings(value = {"SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING",
  "PMB_POSSIBLE_MEMORY_BLOAT", "SQL_INJECTION_JDBC"}, justification = "The db object names are configurable,"
        + "we for know allow heartbeats to multiple data sources, should be one mostly")
@ParametersAreNonnullByDefault
@ThreadSafe
public final class JdbcHeartBeat implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(JdbcHeartBeat.class);

  private static final Map<DataSource, JdbcHeartBeat> HEARTBEATS = new IdentityHashMap<>();

  private static final int HEARTBEAT_INTERVAL_MILLIS =
          Integer.getInteger("spf4j.jdbc.heartBeats.defaultIntervalMillis", 10000);

  @GuardedBy("HEARTBEATS")
  private static boolean isShuttingdown = false;

  private final List<LifecycleHook> lifecycleHooks;

  private final JdbcTemplate jdbc;

  private final String insertHeartbeatSql;

  private final String updateHeartbeatSql;

  private final String selectLastRunSql;

  private final int jdbcTimeoutSeconds;

  private final long intervalNanos;

  private final long intervalMillis;

  private final HeartBeatTableDesc hbTableDesc;

  private final String deleteSql;

  private final String deleteHeartBeatSql;

  private volatile long lastRunNanos;

  private boolean isClosed;

  private ListenableScheduledFuture<?> scheduledHearbeat;

  private final long beatDurationNanos;

  private ScheduledHeartBeat heartbeatRunnable;

  private final double missedHBRatio;

  private final long maxMissedNanos;

  private final long tryBeatThresholdNanos;

  @Override
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
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

    void onError(Error error);

    void onClose() throws SQLException;

  }
  
  private JdbcHeartBeat(final DataSource dataSource, final HeartBeatTableDesc hbTableDesc, final long intervalMillis,
          final int jdbcTimeoutSeconds, final double missedHBRatio) throws InterruptedException, SQLException {
    if (intervalMillis < 1000) {
      throw new IllegalArgumentException("The heartbeat interval should be at least 1s and not "
              + intervalMillis + " ms");
    }
    this.missedHBRatio = missedHBRatio;
    this.jdbc = new JdbcTemplate(dataSource);
    this.jdbcTimeoutSeconds = jdbcTimeoutSeconds;
    this.intervalMillis = intervalMillis;
    this.intervalNanos = TimeUnit.MILLISECONDS.toNanos(intervalMillis);
    this.tryBeatThresholdNanos = intervalNanos / 2;
    this.maxMissedNanos = (long) ((double) intervalNanos * (1 + missedHBRatio));
    this.hbTableDesc = hbTableDesc;
    this.isClosed = false;
    String hbTableName = hbTableDesc.getTableName();
    String lastHeartbeatColumn = hbTableDesc.getLastHeartbeatColumn();
    String currentTimeMillisFunc = hbTableDesc.getDbType().getCurrTSSqlFn();
    String intervalColumn = hbTableDesc.getIntervalColumn();
    String ownerColumn = hbTableDesc.getOwnerColumn();
    this.insertHeartbeatSql = "insert into " + hbTableName + " (" + ownerColumn + ',' + intervalColumn + ','
                + lastHeartbeatColumn + ") VALUES (?, ?, " + currentTimeMillisFunc + ")";
    this.updateHeartbeatSql = "UPDATE " + hbTableName + " SET " + lastHeartbeatColumn + " = " + currentTimeMillisFunc
            + " WHERE " + ownerColumn + " = ? AND " + lastHeartbeatColumn + " + " + intervalColumn
            + " * 2 > " + currentTimeMillisFunc;
    this.deleteHeartBeatSql = "DELETE FROM " + hbTableName
            + " WHERE " + ownerColumn + " = ?";
    this.deleteSql = "DELETE FROM " + hbTableName + " WHERE " + lastHeartbeatColumn + " + " + intervalColumn
            + " * 2 < " + currentTimeMillisFunc;
    this.selectLastRunSql = "select " + lastHeartbeatColumn + " FROM " + hbTableName
            + " where " + ownerColumn + " = ?";
    this.lifecycleHooks = new CopyOnWriteArrayList<>();
    long startTimeNanos =  TimeSource.nanoTime();
    createHeartbeatRow();
    long currTime = TimeSource.nanoTime();
    this.lastRunNanos = currTime;
    long duration = currTime - startTimeNanos;
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


  private void createHeartbeatRow() throws InterruptedException, SQLException {
      jdbc.transactOnConnection((final Connection conn, final long deadlineNanos) -> {

        try (PreparedStatement insert = conn.prepareStatement(insertHeartbeatSql)) {
          insert.setNString(1, org.spf4j.base.Runtime.PROCESS_ID);
          insert.setLong(2, this.intervalMillis);
          insert.setQueryTimeout(JdbcTemplate.getTimeoutToDeadlineSeconds(deadlineNanos));
          insert.executeUpdate();
        }
        return null;
      }, jdbcTimeoutSeconds, TimeUnit.SECONDS);
      LOG.debug("Start Heart Beat for {}", org.spf4j.base.Runtime.PROCESS_ID);
  }

  @JmxExport(description = "Remove all dead hearbeat rows")
  public int removeDeadHeartBeatRows(@JmxExport("timeoutSeconds") final long timeoutSeconds)
          throws SQLException, InterruptedException {
    return jdbc.transactOnConnection((final Connection conn, final long deadlineNanos) -> {
      return JdbcHeartBeat.this.removeDeadHeartBeatRows(conn, deadlineNanos);
    }, timeoutSeconds, TimeUnit.SECONDS);
  }

  @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
  int removeDeadHeartBeatRows(final Connection conn, final long deadlineNanos) throws SQLException {
    try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
      stmt.setQueryTimeout(JdbcTemplate.getTimeoutToDeadlineSeconds(deadlineNanos));
      return stmt.executeUpdate();
    }
  }

  private void removeHeartBeatRow(final int timeoutSeconds)
          throws SQLException {
    jdbc.transactOnConnectionNonInterrupt((final Connection conn, final long deadlineNanos) -> {
      try (PreparedStatement stmt = conn.prepareStatement(deleteHeartBeatSql)) {
        stmt.setNString(1, org.spf4j.base.Runtime.PROCESS_ID);
        stmt.setQueryTimeout(JdbcTemplate.getTimeoutToDeadlineSeconds(deadlineNanos));
        int nrDeleted = stmt.executeUpdate();
        if (nrDeleted != 1) {
          throw new IllegalStateException("Heartbeat rows deleted: " + nrDeleted
                  + " for " + org.spf4j.base.Runtime.PROCESS_ID);
        }
      }
      return null;
    }, timeoutSeconds, TimeUnit.SECONDS);
  }

  @JmxExport(value = "removeDeadHeartBeatRowsAsync", description = "Remove all dead hearbeat rows async")
  public void removeDeadHeartBeatRowsAsyncNoReturn(@JmxExport("timeoutSeconds") final long timeoutSeconds) {
    DefaultExecutor.INSTANCE.execute(new AbstractRunnable(true) {
      @Override
      public void doRun() throws SQLException, InterruptedException {
        removeDeadHeartBeatRows(timeoutSeconds);
      }
    });
  }

  public Future<Integer> removeDeadHeartBeatRowsAsync(final long timeoutSeconds) {
    return DefaultExecutor.INSTANCE.submit(() -> removeDeadHeartBeatRows(timeoutSeconds));
  }

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
        long lrn = lastRunNanos;
        long nanosSincelLastHB = TimeSource.nanoTime() - lrn;
        long delayNanos = intervalNanos - nanosSincelLastHB;
        if (delayNanos < (-intervalNanos) * missedHBRatio) {
          throw new HeartBeatError("Missed heartbeat since last one was " + nanosSincelLastHB + " ns ago");
        }
        if (delayNanos < 0) {
          delayNanos = 0;
        }
        ListenableScheduledFuture<?> scheduleFut = DefaultScheduler.LISTENABLE_INSTANCE.schedule(
                getHeartBeatRunnable(), delayNanos, TimeUnit.NANOSECONDS);
        scheduledHearbeat = scheduleFut;
        Futures.addCallback(scheduleFut, new FutureCallback() {
          @Override
          public void onSuccess(final Object result) {
            synchronized (jdbc) {
              if (!isClosed) {
                scheduledHearbeat = null;
                scheduleHeartbeat();
              }
            }
          }

          @Override
          @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
          public void onFailure(final Throwable t) {
            if (t instanceof Error) {
              throw (Error) t;
            } else if (!(t instanceof CancellationException)) {
              throw new HeartBeatError(t);
            }
          }
        }, DefaultExecutor.INSTANCE);
      }
    }
  }

  @JmxExport
  public void beat() throws SQLException, InterruptedException {
    jdbc.transactOnConnection((final Connection conn, final long deadlineNanos) -> {
      beat(conn, deadlineNanos);
      return null;
    }, jdbcTimeoutSeconds, TimeUnit.SECONDS);
    lastRunNanos = TimeSource.nanoTime();
  }

  void beat(final Connection conn, final long deadlineNanos) {
    synchronized (jdbc) {
      if (isClosed) {
        throw new HeartBeatError("Heartbeater is closed " + this);
      }
    }
    try (PreparedStatement stmt = conn.prepareStatement(updateHeartbeatSql)) {
      stmt.setQueryTimeout(JdbcTemplate.getTimeoutToDeadlineSeconds(deadlineNanos));
      stmt.setNString(1, org.spf4j.base.Runtime.PROCESS_ID);
      int rowsUpdated = stmt.executeUpdate();
      if (rowsUpdated != 1) {
        throw new IllegalStateException("Broken Heartbeat for "
                + org.spf4j.base.Runtime.PROCESS_ID + "sql : " + updateHeartbeatSql + " rows : " + rowsUpdated);
      }
      LOG.debug("Heart Beat for {}", org.spf4j.base.Runtime.PROCESS_ID);
    } catch (SQLException ex) {
      throw new HeartBeatError(ex);
    }
  }


  boolean tryBeat(final Connection conn, final long currentTimeNanos, final long deadlineNanos) {
    if (currentTimeNanos - lastRunNanos > tryBeatThresholdNanos) {
      beat(conn, deadlineNanos);
      return true;
    } else {
      return false;
    }
  }

  void updateLastRunNanos(final long lastRunTime) {
    lastRunNanos = lastRunTime;
  }


  @JmxExport(description = "The last run time recorded in the DB by this process")
  public long getLastRunDB() throws SQLException, InterruptedException {
    return jdbc.transactOnConnection((final Connection conn, final long deadlineNanos) -> {
      try (PreparedStatement stmt = conn.prepareStatement(selectLastRunSql)) {
        stmt.setQueryTimeout(JdbcTemplate.getTimeoutToDeadlineSeconds(deadlineNanos));
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
    }, jdbcTimeoutSeconds, TimeUnit.SECONDS);
  }

  @JmxExport(description = "The heartbeat interval in miliseconds")
  public long getIntervalMillis() {
    return intervalMillis;
  }

  @JmxExport(description =  "The TimeSource nanos time  the jdbc heartbeat run last")
  public long getLastRunNanos() {
    return lastRunNanos;
  }

  @JmxExport
  public String getLastRunTimeStampString() {
    return ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(Timing.getCurrentTiming().fromNanoTimeToEpochMillis(lastRunNanos)),
            ZoneId.systemDefault()).toString();
  }

  /**
   * Get a reference to the hearbeat instance.
   * @param dataSource  the datasource the hearbeat goes against.
   * @param hbTableDesc - heartbeat table description.
   * @param hook  a hook to notify when heartbeat fails.
   * @return the heartbeat instance.
   */
  public static JdbcHeartBeat getHeartBeatAndSubscribe(final DataSource dataSource,
          final HeartBeatTableDesc hbTableDesc,
          @Nullable final LifecycleHook hook) throws InterruptedException, SQLException {
    return getHeartBeatAndSubscribe(dataSource, hbTableDesc,
            hook, HEARTBEAT_INTERVAL_MILLIS, HEARTBEAT_INTERVAL_MILLIS / 1000);
  }

  public static JdbcHeartBeat getHeartBeatAndSubscribe(final DataSource dataSource,
          final HeartBeatTableDesc hbTableDesc,
          @Nullable final LifecycleHook hook, final int heartBeatIntevalMillis, final int jdbcTimeoutSeconds)
          throws InterruptedException, SQLException {
    JdbcHeartBeat beat;
    synchronized (HEARTBEATS) {
      if (isShuttingdown) {
        throw new IllegalStateException("Process is shutting down, no heartbeats are accepted for " + dataSource);
      }
      beat = HEARTBEATS.get(dataSource);
      if (beat == null) {
        beat = new JdbcHeartBeat(dataSource, hbTableDesc, heartBeatIntevalMillis, jdbcTimeoutSeconds, 0.5);
        beat.registerJmx();
        beat.addLyfecycleHook(new LifecycleHook() {
          @Override
          public void onError(final Error error) {
            // this hook is only to remove the heartbeat from the registry.
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
            } catch (SQLException | HeartBeatError ex) {
              // logging in shutdownhooks is not reliable.
              org.spf4j.base.Runtime.error("WARN: Could not clean heartbeat record,"
                      + " this error can be ignored since it is a best effort attempt, detail:", ex);
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

  public static void stopHeartBeats() {
   synchronized (HEARTBEATS) {
     Exception e = org.spf4j.base.Closeables.closeAll(HEARTBEATS.values());
     if (e != null) {
       throw new RuntimeException(e);
     }
     HEARTBEATS.clear();
   }
  }


  public HeartBeatTableDesc getHbTableDesc() {
    return hbTableDesc;
  }

  @Override
  public String toString() {
    return "JdbcHeartBeat{" + "jdbc=" + jdbc + ", jdbcTimeoutSeconds=" + jdbcTimeoutSeconds + ", intervalMillis="
            + intervalMillis + ", hbTableDesc=" + hbTableDesc + ", lastRunNanos=" + lastRunNanos + '}';
  }

  private class ScheduledHeartBeat implements Runnable {

    @Override
    public void run() {
        long lrn = lastRunNanos;
        long currentTimeNanos = TimeSource.nanoTime();
        // not first beat.
        long nanosSinceLastBeat = currentTimeNanos - lrn;
        if (maxMissedNanos < nanosSinceLastBeat) {
          // Unable to beat at inteval!
          HeartBeatError err = new HeartBeatError("System to busy to provide regular heartbeat, last heartbeat "
                  + nanosSinceLastBeat + " ns ago");

          handleError(err);
        }
        if (nanosSinceLastBeat > tryBeatThresholdNanos) {
          try {
            beat();
          } catch (RuntimeException | SQLException | InterruptedException ex) {
            HeartBeatError err = new HeartBeatError("System failed heartbeat", ex);
            handleError(err);
          }
        }
    }

    public void handleError(final HeartBeatError err) {
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
