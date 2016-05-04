
package org.spf4j.concurrent.jdbc;

/**
 *
 * @author zoly
 */
public final class OwnerPermits {

  private final String owner;

  private final int nrPermits;

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
