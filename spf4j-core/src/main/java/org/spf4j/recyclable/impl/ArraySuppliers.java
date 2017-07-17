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
package org.spf4j.recyclable.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.spf4j.base.ReferenceType;
import org.spf4j.recyclable.SizedRecyclingSupplier;

/**
 *
 * @author zoly
 */
public final class ArraySuppliers {

  private ArraySuppliers() {
  }

  public static final class Objects {

    public static final SizedRecyclingSupplier<Object[]> TL_SUPPLIER
            = new Powerof2ThreadLocalRecyclingSupplier<>(new SizedRecyclingSupplier.Factory<Object[]>() {

      @Override
      @SuppressFBWarnings("SUA_SUSPICIOUS_UNINITIALIZED_ARRAY")
      public Object[] create(final int size) {
        return new Object[size];
      }

      @Override
      public int size(final Object[] object) {
        return object.length;
      }
    }, ReferenceType.SOFT);

    private Objects() {
    }

  }

  public static final class Bytes {

    private static final SizedRecyclingSupplier.Factory<byte[]> FACTORY
            = new SizedRecyclingSupplier.Factory<byte[]>() {

      @Override
      @SuppressFBWarnings("SUA_SUSPICIOUS_UNINITIALIZED_ARRAY")
      public byte[] create(final int size) {
        return new byte[size];
      }

      @Override
      public int size(final byte[] object) {
        return object.length;
      }
    };

    public static final SizedRecyclingSupplier<byte[]> TL_SUPPLIER
            = new Powerof2ThreadLocalRecyclingSupplier<>(FACTORY, ReferenceType.SOFT);

    public static final SizedRecyclingSupplier<byte[]> GL_SUPPLIER
            = new Powerof2SizedGlobalRecyclingSupplier<>(FACTORY, ReferenceType.SOFT);

    public static final SizedRecyclingSupplier<byte[]> JAVA_NEW
            = new SizedRecyclingSupplier<byte[]>() {
      @Override
      @SuppressFBWarnings("SUA_SUSPICIOUS_UNINITIALIZED_ARRAY")
      public byte[] get(final int size) {
        return new byte[size];
      }

      @Override
      public void recycle(final byte[] object) {
        // Let the GC deal with this
      }
    };

    private Bytes() {
    }

  }

  public static final class Chars {

    private static final SizedRecyclingSupplier.Factory<char[]> FACTORY
            = new SizedRecyclingSupplier.Factory<char[]>() {

      @Override
      @SuppressFBWarnings("SUA_SUSPICIOUS_UNINITIALIZED_ARRAY")
      public char[] create(final int size) {
        return new char[size];
      }

      @Override
      public int size(final char[] object) {
        return object.length;
      }
    };

    public static final SizedRecyclingSupplier<char[]> TL_SUPPLIER
            = new Powerof2ThreadLocalRecyclingSupplier<>(FACTORY, ReferenceType.SOFT);

    public static final SizedRecyclingSupplier<char[]> GL_SUPPLIER
            = new Powerof2SizedGlobalRecyclingSupplier<>(FACTORY, ReferenceType.SOFT);

    public static final SizedRecyclingSupplier<char[]> JAVA_NEW
            = new SizedRecyclingSupplier<char[]>() {

      @Override
      @SuppressFBWarnings("SUA_SUSPICIOUS_UNINITIALIZED_ARRAY")
      public char[] get(final int size) {
        return new char[size];
      }

      @Override
      public void recycle(final char[] object) {
        // Let the GC deal with this
      }
    };

    private Chars() {
    }

  }

}
