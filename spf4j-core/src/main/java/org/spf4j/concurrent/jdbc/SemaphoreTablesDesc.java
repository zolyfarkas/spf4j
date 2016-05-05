package org.spf4j.concurrent.jdbc;

import java.io.Serializable;

public final class SemaphoreTablesDesc implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String semaphoreTableName;
  private final String semNameColumn;
  private final String availableReservationsColumn;
  private final String maxReservationsColumn;
  private final String lastModifiedByColumn;
  private final String lastModifiedAtColumn;
  private final String reservationsByOwnerTableName;
  private final String ownerColumn;
  private final String ownerReservationsColumn;

  public SemaphoreTablesDesc(final String semaphoreTableName, final String semNameColumn,
          final String availableReservationsColumn,
          final String maxReservationsColumn, final String lastModifiedColumn,
          final String lastModifiedAtColumn, final String reservationsByOwnerTableName,
          final String ownerColumn, final String ownerReservationsColumn) {
    this.semaphoreTableName = semaphoreTableName;
    this.semNameColumn = semNameColumn;
    this.availableReservationsColumn = availableReservationsColumn;
    this.maxReservationsColumn = maxReservationsColumn;
    this.lastModifiedByColumn = lastModifiedColumn;
    this.lastModifiedAtColumn = lastModifiedAtColumn;
    this.reservationsByOwnerTableName = reservationsByOwnerTableName;
    this.ownerColumn = ownerColumn;
    this.ownerReservationsColumn = ownerReservationsColumn;
  }

  public String getSemaphoreTableName() {
    return semaphoreTableName;
  }

  public String getSemNameColumn() {
    return semNameColumn;
  }

  public String getAvailableReservationsColumn() {
    return availableReservationsColumn;
  }

  public String getMaxReservationsColumn() {
    return maxReservationsColumn;
  }

  public String getLastModifiedByColumn() {
    return lastModifiedByColumn;
  }

  public String getLastModifiedAtColumn() {
    return lastModifiedAtColumn;
  }

  public String getReservationsByOwnerTableName() {
    return reservationsByOwnerTableName;
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
            + semNameColumn + ", availableReservationsColumn=" + availableReservationsColumn
            + ", maxReservationsColumn=" + maxReservationsColumn + ", lastModifiedByColumn="
            + lastModifiedByColumn + ", lastModifiedAtColumn=" + lastModifiedAtColumn
            + ", reservationsByOwnerTableName=" + reservationsByOwnerTableName + ", ownerColumn="
            + ownerColumn + ", ownerReservationsColumn=" + ownerReservationsColumn + '}';
  }




}