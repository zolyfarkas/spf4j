/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
        byte[] array = ArraySuppliers.Bytes.SUPPLIER.get(SIZE);
        try {
        System.arraycopy(TEST_ARRAY, 0, array, 0, SIZE);
        return Strings.fromUtf8(array);
        } finally {
            ArraySuppliers.Bytes.SUPPLIER.recycle(array);
        }
    }
}