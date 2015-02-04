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

package org.spf4j.recyclable.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.spf4j.base.ReferenceType;
import org.spf4j.recyclable.SizedRecyclingSupplier;

/**
 *
 * @author zoly
 */
public final class ArraySuppliers {

    private ArraySuppliers() { }

    public static final class Bytes {

        private Bytes() { }

      public static final SizedRecyclingSupplier<byte[]> TL_SUPPLIER
              = new SizedThreadLocalRecyclingSupplier2<>(
                new SizedRecyclingSupplier.Factory<byte[]>() {

            @Override
            @SuppressFBWarnings("SUA_SUSPICIOUS_UNINITIALIZED_ARRAY")
            public byte[] create(final int size) {
                return new byte[size];
            }

            @Override
            public int size(final byte[] object) {
                return object.length;
            }
        }, ReferenceType.SOFT);

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


    }

}
