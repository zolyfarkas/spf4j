
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
