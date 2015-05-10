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
package org.spf4j.perf.impl;

import java.util.Arrays;
import static org.junit.Assert.assertArrayEquals;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class QuantizedRecorderTest {
    
    public QuantizedRecorderTest() {
    }


    /**
     * Test of record method, of class QuantizedAccumulator.
     */
    @Test
    public void testRecord1() {
        System.out.println("record");
        QuantizedAccumulator instance = new QuantizedAccumulator("test", "", "ms",
                10, 0, 3, 10);
        instance.record(0);
        instance.record(1);
        instance.record(2);
        instance.record(10);
        instance.record(11);
        instance.record(250);
        instance.record(15000);
        System.out.println(instance);
    }

        @Test
    public void testRecord2() {
        System.out.println("record");
        QuantizedAccumulator instance = new QuantizedAccumulator("test", "", "ms",
                10, -3, 3, 10);
        instance.record(0);
        instance.record(1);
        instance.record(2);
        instance.record(10);
        instance.record(11);
        instance.record(250);
        instance.record(15000);
        
        System.out.println(instance);
    }
    
    @Test
    public void testRecord3() {
        System.out.println("record");
        QuantizedAccumulator instance = new QuantizedAccumulator("test", "", "ms",
                10, 0, 1, 10);
        instance.record(0);
        instance.record(1);
        instance.record(2);
        instance.record(10);
        instance.record(11);
        instance.record(250);
        instance.record(15000);
        assertArrayEquals(new long[] {0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 4}, instance.getQuatizedMeasurements());
        System.out.println(instance);
        long[] result = instance.get();
        System.out.println(Arrays.toString(result));
    }
  
    
        @Test
    public void testRecord4() {
        System.out.println("record");
        QuantizedAccumulator instance = new QuantizedAccumulator("test", "", "ms",
                10, -1, 1, 10);
        instance.record(0);
        instance.record(1);
        instance.record(2);
        instance.record(10);
        instance.record(11);
        instance.record(250);
        instance.record(15000);
        instance.record(-15000);
        instance.record(-10);
        instance.record(-10);
        assertArrayEquals(new long[] {1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 4},
                instance.getQuatizedMeasurements());
        System.out.println(instance);
        long[] result = instance.get();
        System.out.println(Arrays.toString(result));
    }


}
