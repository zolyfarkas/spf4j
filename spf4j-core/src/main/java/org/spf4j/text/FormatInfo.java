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
package org.spf4j.text;

import java.io.Serializable;
import java.text.Format;
import java.util.Objects;
import javax.annotation.Nullable;

/**
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
    int hash = Objects.hashCode(this.format);
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
