/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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

      public static final SizedThreadLocalRecyclingSupplier<byte[]> SUPPLIER
              = new SizedThreadLocalRecyclingSupplier<>(
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
    }

}
