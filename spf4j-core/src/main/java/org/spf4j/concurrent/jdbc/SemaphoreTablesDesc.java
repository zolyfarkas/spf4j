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
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.jdbc.DbType;
import org.spf4j.jdbc.JdbcTemplate;
/**
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class SemaphoreTablesDesc implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final SemaphoreTablesDesc DEFAULT = new SemaphoreTablesDesc(
          System.getProperty("spf4j.jdbc.semaphore.sql.tableName", "SEMAPHORES"),
          System.getProperty("spf4j.jdbc.semaphore.sql.semaphoreNameColumn", "SEMAPHORE_NAME"),
          System.getProperty("spf4j.jdbc.semaphore.sql.availablePermitsColumn", "AVAILABLE_PERMITS"),
          System.getProperty("spf4j.jdbc.semaphore.sql.totalPermitsColumn", "TOTAL_PERMITS"),
          System.getProperty("spf4j.jdbc.semaphore.sql.lastUpdatedByColumn", "LAST_UPDATED_BY"),
          System.getProperty("spf4j.jdbc.semaphore.sql.lastUpdatedAtColumn", "LAST_UPDATED_AT"),
          System.getProperty("spf4j.jdbc.semaphore.sql.permitsByOwnerColumn", "PERMITS_BY_OWNER"),
          HeartBeatTableDesc.DEFAULT.getOwnerColumn(),
          System.getProperty("spf4j.jdbc.semaphore.sql.permitsColumn", "PERMITS"),
          HeartBeatTableDesc.DEFAULT);

  @Nonnull
  private final String semaphoreTableName;
  private final String semNameColumn;
  private final String availablePermitsColumn;
  private final String totalPermitsColumn;
  private final String lastModifiedByColumn;
  private final String lastModifiedAtColumn;
  private final String permitsByOwnerTableName;
  private final String ownerColumn;
  private final String ownerPermitsColumn;
  private final HeartBeatTableDesc heartBeatTableDesc;

  public SemaphoreTablesDesc(final String semaphoreTableName, final String semNameColumn,
          final String availablePermitsColumn,
          final String totalPermitsColumn, final String lastModifiedByColumn,
          final String lastModifiedAtColumn, final String permitsByOwnerTableName,
          final String ownerColumn, final String ownerPermitsColumn,
          final HeartBeatTableDesc heartBeatTableDesc) {
    JdbcTemplate.checkJdbcObjectName(semaphoreTableName);
    JdbcTemplate.checkJdbcObjectName(semNameColumn);
    JdbcTemplate.checkJdbcObjectName(availablePermitsColumn);
    JdbcTemplate.checkJdbcObjectName(totalPermitsColumn);
    JdbcTemplate.checkJdbcObjectName(lastModifiedByColumn);
    JdbcTemplate.checkJdbcObjectName(lastModifiedAtColumn);
    JdbcTemplate.checkJdbcObjectName(permitsByOwnerTableName);
    JdbcTemplate.checkJdbcObjectName(ownerColumn);
    JdbcTemplate.checkJdbcObjectName(ownerPermitsColumn);
    this.semaphoreTableName = semaphoreTableName;
    this.semNameColumn = semNameColumn;
    this.availablePermitsColumn = availablePermitsColumn;
    this.totalPermitsColumn = totalPermitsColumn;
    this.lastModifiedByColumn = lastModifiedByColumn;
    this.lastModifiedAtColumn = lastModifiedAtColumn;
    this.permitsByOwnerTableName = permitsByOwnerTableName;
    this.ownerColumn = ownerColumn;
    this.ownerPermitsColumn = ownerPermitsColumn;
    this.heartBeatTableDesc = heartBeatTableDesc;
  }

  public String getSemaphoreTableName() {
    return semaphoreTableName;
  }

  public String getSemNameColumn() {
    return semNameColumn;
  }

  public String getAvailablePermitsColumn() {
    return availablePermitsColumn;
  }

  public String getTotalPermitsColumn() {
    return totalPermitsColumn;
  }

  public String getLastModifiedByColumn() {
    return lastModifiedByColumn;
  }

  public String getLastModifiedAtColumn() {
    return lastModifiedAtColumn;
  }

  public String getPermitsByOwnerTableName() {
    return permitsByOwnerTableName;
  }

  public String getOwnerColumn() {
    return ownerColumn;
  }

  public String getOwnerPermitsColumn() {
    return ownerPermitsColumn;
  }

  public HeartBeatTableDesc getHeartBeatTableDesc() {
    return heartBeatTableDesc;
  }

  public SemaphoreTablesDesc withDbType(final DbType dbType) {
    return new SemaphoreTablesDesc(semaphoreTableName, semNameColumn, availablePermitsColumn,
            totalPermitsColumn, lastModifiedByColumn, lastModifiedAtColumn, permitsByOwnerTableName,
            ownerColumn, ownerPermitsColumn, heartBeatTableDesc.withDbType(dbType));
  }

  @Override
  public int hashCode() {
    return this.semaphoreTableName.hashCode();
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
    final SemaphoreTablesDesc other = (SemaphoreTablesDesc) obj;
    if (!Objects.equals(this.semaphoreTableName, other.semaphoreTableName)) {
      return false;
    }
    if (!Objects.equals(this.semNameColumn, other.semNameColumn)) {
      return false;
    }
    if (!Objects.equals(this.availablePermitsColumn, other.availablePermitsColumn)) {
      return false;
    }
    if (!Objects.equals(this.totalPermitsColumn, other.totalPermitsColumn)) {
      return false;
    }
    if (!Objects.equals(this.lastModifiedByColumn, other.lastModifiedByColumn)) {
      return false;
    }
    if (!Objects.equals(this.lastModifiedAtColumn, other.lastModifiedAtColumn)) {
      return false;
    }
    if (!Objects.equals(this.permitsByOwnerTableName, other.permitsByOwnerTableName)) {
      return false;
    }
    if (!Objects.equals(this.ownerColumn, other.ownerColumn)) {
      return false;
    }
    if (!Objects.equals(this.ownerPermitsColumn, other.ownerPermitsColumn)) {
      return false;
    }
    return Objects.equals(this.heartBeatTableDesc, other.heartBeatTableDesc);
  }



  @Override
  public String toString() {
    return "SemaphoreTablesDesc{" + "semaphoreTableName=" + semaphoreTableName + ", semNameColumn="
            + semNameColumn + ", availablePermitsColumn=" + availablePermitsColumn
            + ", totalPermitsColumn=" + totalPermitsColumn + ", lastModifiedByColumn="
            + lastModifiedByColumn + ", lastModifiedAtColumn=" + lastModifiedAtColumn
            + ", permitsByOwnerTableName=" + permitsByOwnerTableName + ", ownerColumn="
            + ownerColumn + ", ownerReservationsColumn=" + ownerPermitsColumn + '}';
  }

}