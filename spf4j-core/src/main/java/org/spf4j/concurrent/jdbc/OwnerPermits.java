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
public final class OwnerPermits implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String owner;

  private final int nrPermits;

  @java.beans.ConstructorProperties({ "owner", "nrPermits" })
  public OwnerPermits(final String owner, final int nrPermits) {
    this.owner = owner;
    this.nrPermits = nrPermits;
  }

  public String getOwner() {
    return owner;
  }

  public int getNrPermits() {
    return nrPermits;
  }

  @Override
  public String toString() {
    return "OwnerPermits{" + "owner=" + owner + ", nrPermits=" + nrPermits + '}';
  }

}
