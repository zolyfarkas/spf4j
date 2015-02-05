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
package org.spf4j.recyclable;

import java.io.IOException;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.spf4j.base.IntMath;
import org.spf4j.base.Strings;
import org.spf4j.recyclable.impl.ArraySuppliers;

@State(Scope.Benchmark)
@Fork(2)
@Threads(value = 4)
public class TestRecyclingAlocator {


    private static byte [] testArray(final int size) {
        IntMath.XorShift32 rnd = new IntMath.XorShift32();
        byte [] result = new byte[size];
        for (int i = 0; i < size; i++) {
            result[i] = (byte) ('A' + Math.abs(rnd.nextInt()) % 22);
        }
        return result;
    }

    private static final int SIZE = 1024 * 8;

    private static final byte[] TEST_ARRAY = testArray(SIZE);



    @Benchmark
    public String testNew() throws IOException {
        byte[] array = new byte[SIZE];
        System.arraycopy(TEST_ARRAY, 0, array, 0, SIZE);
        return Strings.fromUtf8(array);
    }

    @Benchmark
    public String testRecycler() throws IOException {
        byte[] array = ArraySuppliers.Bytes.TL_SUPPLIER.get(SIZE);
        try {
        System.arraycopy(TEST_ARRAY, 0, array, 0, SIZE);
        return Strings.fromUtf8(array);
        } finally {
            ArraySuppliers.Bytes.TL_SUPPLIER.recycle(array);
        }
    }
}