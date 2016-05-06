package org.spf4j.concurrent.jdbc;

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
import javax.sql.DataSource;
import org.spf4j.base.HandlerNano;
import org.spf4j.concurrent.DefaultExecutor;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.jdbc.JdbcTemplate;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.Registry;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings(value = {"SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING",
  "PMB_POSSIBLE_MEMORY_BLOAT"}, justification = "The db object names are configurable,"
        + "we for know allow heartbeats to multiple data sources, should be one mostly")
@ParametersAreNonnullByDefault
public final class JdbcHeartBeat {

  private final JdbcTemplate jdbc;

  private final String updateHeartbeatSql;

  private final int jdbcTimeoutSeconds;

  private final long intervalMillis;

  private final HeartBeatTableDesc hbTableDesc;

  private final String deleteSql;

  private volatile long lastRun;

  public interface FailureHook {

    void onError(final Error error);
  }

  private final List<FailureHook> failureHooks;

  private JdbcHeartBeat(final DataSource dataSource, final long intervalMillis,
          final int jdbcTimeoutSeconds) throws SQLException, InterruptedException {
    this(dataSource, new HeartBeatTableDesc("HEARTBEATS",
            "OWNER", "INTERVAL_MILLIS", "LAST_HEARTBEAT_INSTANT_MILLIS",
            "TIMESTAMPDIFF('MILLISECOND', timestamp '1970-01-01 00:00:00', CURRENT_TIMESTAMP())"),
            intervalMillis, jdbcTimeoutSeconds);
  }

  private JdbcHeartBeat(final DataSource dataSource, final HeartBeatTableDesc hbTableDesc, final long intervalMillis,
          final int jdbcTimeoutSeconds) throws SQLException, InterruptedException {
    this.jdbc = new JdbcTemplate(dataSource);
    this.jdbcTimeoutSeconds = jdbcTimeoutSeconds;
    this.intervalMillis = intervalMillis;
    this.hbTableDesc = hbTableDesc;
    String hbTableName = hbTableDesc.getTableName();
    String lastHeartbeatColumn = hbTableDesc.getLastHeartbeatColumn();
    String currentTimeMillisFunc = hbTableDesc.getCurrentTimeMillisFunc();
    String intervalColumn = hbTableDesc.getIntervalColumn();

    this.updateHeartbeatSql = "UPDATE " + hbTableName + " SET " + lastHeartbeatColumn + " = " + currentTimeMillisFunc
            + " WHERE " + hbTableDesc.getOwnerColumn() + " = ? AND " + lastHeartbeatColumn + " + " + intervalColumn
            + " * 2 > " + currentTimeMillisFunc;
    this.deleteSql = "DELETE FROM " + hbTableName + " WHERE " + lastHeartbeatColumn + " + " + intervalColumn
            + " * 2 < " + currentTimeMillisFunc;
    this.failureHooks = new CopyOnWriteArrayList<>();
    createHeartbeatRow();
  }

  public void registerJmx() {
    Registry.export(this);
  }

  public void addFailureHook(final FailureHook hook) {
    failureHooks.add(hook);
  }

  void createHeartbeatRow()
          throws SQLException, InterruptedException {

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

  @JmxExport(description = "Remove all dead hearbeat rows async")
  public Future<Integer> removeDeadHeartBeatRowsAsync(final int timeoutSeconds) {
    return DefaultExecutor.INSTANCE.submit(new Callable<Integer>() {
      @Override
      public Integer call() throws SQLException, InterruptedException {
        return removeDeadHeartBeatRows(timeoutSeconds);
      }
    });
  }

  public ScheduledFuture<?> scheduleHeartbeat() {
    return DefaultScheduler.INSTANCE.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        try {
          long lr = lastRun;
          long currentTimeMillis = System.currentTimeMillis();
          if (lr != 0
                  && ((intervalMillis * 2) < (currentTimeMillis - lr))) {
            Error err = new Error("System to busy to provide regular heartbeat, lastRun = " + lr
                + ", intervalMillis = " + intervalMillis + ", currentTimeMillis = " + currentTimeMillis);
            for (FailureHook hook : failureHooks) {
              hook.onError(err);
            }
            throw err;
          }
          beat();
          lastRun = currentTimeMillis;
        } catch (RuntimeException | SQLException | InterruptedException ex) {
          Error err = new Error("System failed heartbeat", ex);
          for (FailureHook hook : failureHooks) {
            hook.onError(err);
          }
          throw err;

        }
      }
    }, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
  }

  @JmxExport
  public void beat() throws SQLException, InterruptedException {
    jdbc.transactOnConnection(new HandlerNano<Connection, Void, SQLException>() {
      @Override
      @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
      public Void handle(final Connection conn, final long deadlineNanos) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(updateHeartbeatSql)) {
          stmt.setQueryTimeout(10);
          stmt.setNString(1, org.spf4j.base.Runtime.PROCESS_ID);
          int rowsUpdated = stmt.executeUpdate();
          if (rowsUpdated != 1) {
            throw new IllegalStateException("Broken Heartbeat for "
                    + org.spf4j.base.Runtime.PROCESS_ID + "sql : " + updateHeartbeatSql +  " rows : " + rowsUpdated);
          }
          return null;
        }
      }
    }, jdbcTimeoutSeconds, TimeUnit.SECONDS);
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
              long result =  rs.getLong(1);
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
    return  ZonedDateTime.ofInstant(Instant.ofEpochMilli(lastRun), ZoneId.systemDefault()).toString();
  }

  private static final Map<DataSource, JdbcHeartBeat> HEARTBEATS = new IdentityHashMap<>();

  private static final int HEARTBEAT_INTERVAL_MILLIS = Integer.getInteger("spf4j.heartbeat.intervalMillis", 10000);

  public static synchronized JdbcHeartBeat getHeartbeat(final DataSource dataSource, @Nullable final FailureHook hook)
          throws SQLException, InterruptedException {
    JdbcHeartBeat beat = HEARTBEATS.get(dataSource);
    if (beat == null) {
      beat = new JdbcHeartBeat(dataSource, HEARTBEAT_INTERVAL_MILLIS, 5);
      beat.scheduleHeartbeat();
      beat.registerJmx();
      HEARTBEATS.put(dataSource, beat);
    }
    if (hook != null) {
      beat.addFailureHook(hook);
    }
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

}
