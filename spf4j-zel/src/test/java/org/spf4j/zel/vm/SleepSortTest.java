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
package org.spf4j.zel.vm;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.base.IntMath;

/**
 *
 * @author zoly
 */
public final class SleepSortTest {

  @Test
  public void testSort() throws CompileException, ExecutionException, InterruptedException, IOException {
    String sort = Resources.toString(Resources.getResource(SleepSortTest.class, "sleepSort.zel"),
            StandardCharsets.US_ASCII);
    Program p = Program.compile(sort, "x");
    Integer[] testArray = new Integer[100];
    IntMath.XorShift32 random = new IntMath.XorShift32();
    for (int i = 0; i < testArray.length; i++) {
      testArray[i] = Math.abs(random.nextInt()) % 100;
    }

    Integer[] resutlSt = testArray.clone();
    p.execute(new Object[]{resutlSt});
    Arrays.sort(testArray);
    Assert.assertArrayEquals(testArray, (Object[]) resutlSt);
  }

}
