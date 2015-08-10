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
import java.util.Random;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class SleepSortTest {


    @Test
    public void testSort() throws CompileException, ExecutionException, InterruptedException, IOException {
       String sort = Resources.toString(Resources.getResource(SleepSortTest.class, "sleepSort.zel"),
                Charsets.US_ASCII);
        Program p = Program.compile(sort, "x");
        Integer [] testArray = new Integer [100];
        Random random = new Random();
        for (int i = 0; i < testArray.length; i++) {
            testArray[i] = Math.abs(random.nextInt()) % 100;
        }

        Integer [] resutlSt = testArray.clone();
        p.execute(new Object [] {resutlSt});
        Arrays.sort(testArray);
        Assert.assertArrayEquals(testArray, (Object []) resutlSt);
    }

}
