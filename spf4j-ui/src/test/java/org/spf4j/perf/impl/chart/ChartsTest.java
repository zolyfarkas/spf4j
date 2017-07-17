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
package org.spf4j.perf.impl.chart;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.base.Pair;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("CLI_CONSTANT_LIST_INDEX")
public class ChartsTest {

  @Test
  public void testGapsFiller1() {
    Pair<long[], double[][]> fillGaps = Charts.fillGaps(new long[]{0L}, new long[][]{{1L, 2L, 3L}},
            10, 3);
    Assert.assertEquals(0L, fillGaps.getFirst()[0]);
    Assert.assertEquals(3d, fillGaps.getSecond()[0][2], 0.01);
  }

  @Test
  public void testGapsFiller2() {
    Pair<long[], double[][]> fillGaps = Charts.fillGaps(new long[]{0L, 30L},
            new long[][]{{1L, 2L, 3L}, {10L, 20L, 30L}},
            10, 3);
    long[] first = fillGaps.getFirst();
    Assert.assertEquals(4, first.length);
    double[][] second = fillGaps.getSecond();
    Assert.assertEquals(4, second.length);
    Assert.assertEquals(10L, first[1]);
    Assert.assertEquals(20L, first[2]);
    Assert.assertEquals(Double.NaN, second[1][2], 0.01);
    Assert.assertEquals(Double.NaN, second[2][2], 0.01);
  }

}
