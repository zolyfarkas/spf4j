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
package org.spf4j.zel.vm;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.base.IntMath;

/**
 *
 * @author zoly
 */
public final class QSortTest {

    @Test
    public void test() throws CompileException, ExecutionException, InterruptedException, IOException {
        String qsort = Resources.toString(Resources.getResource(QSortTest.class, "sort.zel"),
                Charsets.US_ASCII);
        Program p = Program.compile(qsort);
        System.out.println(p);
        Integer result = (Integer) p.execute();
        Assert.assertEquals(10000, result.intValue());
    }

    @Test
    public void testSort() throws CompileException, ExecutionException, InterruptedException, IOException {
       String qsort = Resources.toString(Resources.getResource(QSortTest.class, "sortFunc.zel"),
                Charsets.US_ASCII);
        Program p = Program.compile(qsort, "x");
        Integer [] testArray = new Integer [10000];
        IntMath.XorShift32 random = new IntMath.XorShift32();
        for (int i = 0; i < testArray.length; i++) {
            testArray[i] = random.nextInt();
        }

        Integer [] resultPar = null;
        for (int i = 0; i < 3; i++) {
            long startTime = System.currentTimeMillis();
            resultPar = testArray.clone();
            p.execute(new Object [] {resultPar});
            System.out.println("Parallel exec time = " + (System.currentTimeMillis() - startTime));
        }

        Integer [] resultSt = null;
        for (int i = 0; i < 3; i++) {
            long startTime = System.currentTimeMillis();
            resultSt = testArray.clone();
            p.execute(Executors.newSingleThreadExecutor(), new Object [] {resultSt});
            System.out.println("ST exec time = " + (System.currentTimeMillis() - startTime));
        }

        Arrays.sort(testArray);

        Assert.assertArrayEquals((Object []) resultSt, (Object []) resultPar);
        Assert.assertArrayEquals((Object []) resultSt, testArray);
    }

}
