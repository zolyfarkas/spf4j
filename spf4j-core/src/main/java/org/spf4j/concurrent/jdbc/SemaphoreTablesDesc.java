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

import java.io.Serializable;
/**
 *
 * @author zoly
 */
public final class SemaphoreTablesDesc implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String semaphoreTableName;
  private final String semNameColumn;
  private final String availablePermitsColumn;
  private final String totalPermitsColumn;
  private final String lastModifiedByColumn;
  private final String lastModifiedAtColumn;
  private final String permitsByOwnerTableName;
  private final String ownerColumn;
  private final String ownerReservationsColumn;
  private final HeartBeatTableDesc heartBeatTableDesc;

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


  public SemaphoreTablesDesc(final String semaphoreTableName, final String semNameColumn,
          final String availablePermitsColumn,
          final String totalPermitsColumn, final String lastModifiedColumn,
          final String lastModifiedAtColumn, final String reservationsByOwnerTableName,
          final String ownerColumn, final String ownerReservationsColumn,
          final HeartBeatTableDesc heartBeatTableDesc) {
    this.semaphoreTableName = semaphoreTableName;
    this.semNameColumn = semNameColumn;
    this.availablePermitsColumn = availablePermitsColumn;
    this.totalPermitsColumn = totalPermitsColumn;
    this.lastModifiedByColumn = lastModifiedColumn;
    this.lastModifiedAtColumn = lastModifiedAtColumn;
    this.permitsByOwnerTableName = reservationsByOwnerTableName;
    this.ownerColumn = ownerColumn;
    this.ownerReservationsColumn = ownerReservationsColumn;
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

  public String getOwnerReservationsColumn() {
    return ownerReservationsColumn;
  }

  public HeartBeatTableDesc getHeartBeatTableDesc() {
    return heartBeatTableDesc;
  }


  @Override
  public String toString() {
    return "SemaphoreTablesDesc{" + "semaphoreTableName=" + semaphoreTableName + ", semNameColumn="
            + semNameColumn + ", availablePermitsColumn=" + availablePermitsColumn
            + ", totalPermitsColumn=" + totalPermitsColumn + ", lastModifiedByColumn="
            + lastModifiedByColumn + ", lastModifiedAtColumn=" + lastModifiedAtColumn
            + ", permitsByOwnerTableName=" + permitsByOwnerTableName + ", ownerColumn="
            + ownerColumn + ", ownerReservationsColumn=" + ownerReservationsColumn + '}';
  }

}