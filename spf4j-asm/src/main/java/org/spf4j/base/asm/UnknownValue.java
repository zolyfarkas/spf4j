
package org.spf4j.base.asm;

public final class UnknownValue {
  private final int generatedBy;

  public UnknownValue(final int gerenatedBy) {
    this.generatedBy = gerenatedBy;
  }

  public int getGeneratedBy() {
    return generatedBy;
  }

  @Override
  public String toString() {
    return "UnknownValue{" + "generatedBy=" + generatedBy + '}';
  }

}
