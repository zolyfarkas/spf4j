package org.spf4j.concurrent.jdbc;

import java.io.Serializable;

/*
  CREATE TABLE HEARTBEATS (
   OWNER VARCHAR(255) NOT NULL,
   INTERVAL_MILLIS bigint(20) NOT NULL,
   LAST_HEARTBEAT_INSTANT_MILLIS bigint(20) NOT NULL,
   PRIMARY KEY (OWNER),
   UNIQUE KEY HEARTBEATS_PK (OWNER)
  );
 */
public final class HeartBeatTableDesc  implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String tableName;
  private final String ownerColumn;
  private final String intervalColumn;
  private final String lastHeartbeatColumn;

  public HeartBeatTableDesc(final String tableName, final String ownerColun,
          final String intervalColumn, final String lastHeartbeatColumn) {
    this.tableName = tableName;
    this.ownerColumn = ownerColun;
    this.intervalColumn = intervalColumn;
    this.lastHeartbeatColumn = lastHeartbeatColumn;
  }

  public String getTableName() {
    return tableName;
  }

  public String getOwnerColumn() {
    return ownerColumn;
  }

  public String getIntervalColumn() {
    return intervalColumn;
  }

  public String getLastHeartbeatColumn() {
    return lastHeartbeatColumn;
  }

  @Override
  public String toString() {
    return "HeartbeatTableDesc{" + "tableName=" + tableName + ", ownerColun=" + ownerColumn + ", intervalColumn="
            + intervalColumn + ", lastHeartbeatColumn=" + lastHeartbeatColumn + '}';
  }

}
