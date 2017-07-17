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

import java.io.Serializable;
import java.util.Objects;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.jdbc.DbType;
import org.spf4j.jdbc.JdbcTemplate;

/*
 * CREATE TABLE HEARTBEATS (
 *  OWNER VARCHAR(255) NOT NULL,
 *  INTERVAL_MILLIS bigint(20) NOT NULL,
 *  LAST_HEARTBEAT_INSTANT_MILLIS bigint(20) NOT NULL,
 *  PRIMARY KEY (OWNER),
 *  UNIQUE KEY HEARTBEATS_PK (OWNER)
 * );
 *
 * Table description for storing heartbeats for processes. (OWNER)
 * The main purpose of this table is to detect dead OWNERS (when their heart stops beating)
 * OWNER = String column uniquely identifying a process.
 * INTERVAL_MILLIS - the delay between heartbeats.
 * LAST_HEARTBEAT_INSTANT_MILLIS - the millis since epoch when the last heartbeat happened.
 */
@ParametersAreNonnullByDefault
public final class HeartBeatTableDesc implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final HeartBeatTableDesc DEFAULT = new HeartBeatTableDesc(
          System.getProperty("spf4j.jdbc.heartBeats.sql.tableName", "HEARTBEATS"),
          System.getProperty("spf4j.jdbc.heartBeats.sql.ownerColumn", "OWNER"),
          System.getProperty("spf4j.jdbc.heartBeats.sql.intervalMillisColumn", "INTERVAL_MILLIS"),
          System.getProperty("spf4j.jdbc.heartBeats.sql.lastHeartBeatMillisColumn", "LAST_HEARTBEAT_INSTANT_MILLIS"),
          DbType.valueOf(System.getProperty("spf4j.jdbc.heartBeats.sql.dbType", "H2")));


  private final String tableName;
  private final String ownerColumn;
  private final String intervalColumn;
  private final String lastHeartbeatColumn;

  /**
   * MSSQL = DATEDIFF(ms, '1970-01-01 00:00:00', GETUTCDATE())
   * ORACLE = (SYSDATE - TO_DATE('01-01-1970 00:00:00', 'DD-MM-YYYY HH24:MI:SS')) * 24 * 3600000
   * H2 = TIMESTAMPDIFF('MILLISECOND', timestamp '1970-01-01 00:00:00', CURRENT_TIMESTAMP())
   * POSTGRES = extract(epoch FROM now()) * 1000
   */
  private final DbType dbType;

  public HeartBeatTableDesc(final String tableName, final String ownerColun,
        final String intervalColumn, final String lastHeartbeatColumn, final DbType dbType) {
    JdbcTemplate.checkJdbcObjectName(tableName);
    JdbcTemplate.checkJdbcObjectName(ownerColun);
    JdbcTemplate.checkJdbcObjectName(intervalColumn);
    JdbcTemplate.checkJdbcObjectName(lastHeartbeatColumn);
    this.tableName = tableName;
    this.ownerColumn = ownerColun;
    this.intervalColumn = intervalColumn;
    this.lastHeartbeatColumn = lastHeartbeatColumn;
    this.dbType = dbType;
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

  public DbType getDbType() {
    return dbType;
  }

  public HeartBeatTableDesc withDbType(final DbType pdbType) {
    return new HeartBeatTableDesc(tableName, ownerColumn, intervalColumn, lastHeartbeatColumn, pdbType);
  }

  @Override
  public int hashCode() {
    return tableName.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final HeartBeatTableDesc other = (HeartBeatTableDesc) obj;
    if (!Objects.equals(this.tableName, other.tableName)) {
      return false;
    }
    if (!Objects.equals(this.ownerColumn, other.ownerColumn)) {
      return false;
    }
    if (!Objects.equals(this.intervalColumn, other.intervalColumn)) {
      return false;
    }
    if (!Objects.equals(this.lastHeartbeatColumn, other.lastHeartbeatColumn)) {
      return false;
    }
    return this.dbType == other.dbType;
  }




  @Override
  public String toString() {
    return "HeartbeatTableDesc{" + "tableName=" + tableName + ", ownerColun=" + ownerColumn + ", intervalColumn="
            + intervalColumn + ", lastHeartbeatColumn=" + lastHeartbeatColumn + '}';
  }

}
