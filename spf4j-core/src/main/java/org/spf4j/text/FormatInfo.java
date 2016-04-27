
package org.spf4j.text;

import java.io.Serializable;
import java.text.Format;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 *
 * @author zoly
 */
final class FormatInfo implements Serializable, Cloneable {

  private static final long serialVersionUID = 1L;
  private final Format format;
  
  private final int offset;
  
  private final int argumentNumber;

  FormatInfo(@Nullable final Format format, final int offset, final int argumentNumber) {
    this.format = format;
    this.offset = offset;
    this.argumentNumber = argumentNumber;
  }

  @Nullable
  public Format getFormat() {
    return format;
  }

  public int getOffset() {
    return offset;
  }

  public int getArgumentNumber() {
    return argumentNumber;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 31 * hash + Objects.hashCode(this.format);
    hash = 31 * hash + this.offset;
    return 31 * hash + this.argumentNumber;
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
    final FormatInfo other = (FormatInfo) obj;
    if (this.offset != other.offset) {
      return false;
    }
    if (this.argumentNumber != other.argumentNumber) {
      return false;
    }
    return Objects.equals(this.format, other.format);
  }

  @Override
  public String toString() {
    return "FormatInfo{" + "format=" + format + ", offset=" + offset + ", argumentNumber=" + argumentNumber + '}';
  }

  @Override
  protected FormatInfo clone() {
    return new FormatInfo((Format) format.clone(), offset, argumentNumber);
  }
  
  
  
}
