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

  public SemaphoreTablesDesc(final String semaphoreTableName, final String semNameColumn,
          final String availablePermitsColumn,
          final String maxReservationsColumn, final String lastModifiedColumn,
          final String lastModifiedAtColumn, final String reservationsByOwnerTableName,
          final String ownerColumn, final String ownerReservationsColumn) {
    this.semaphoreTableName = semaphoreTableName;
    this.semNameColumn = semNameColumn;
    this.availablePermitsColumn = availablePermitsColumn;
    this.totalPermitsColumn = maxReservationsColumn;
    this.lastModifiedByColumn = lastModifiedColumn;
    this.lastModifiedAtColumn = lastModifiedAtColumn;
    this.permitsByOwnerTableName = reservationsByOwnerTableName;
    this.ownerColumn = ownerColumn;
    this.ownerReservationsColumn = ownerReservationsColumn;
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