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
package org.spf4j.base;

import org.spf4j.ds.RTree;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class RTreeTest {

  /**
   * Test of search method, of class RTree.
   */
  @Test
  public void testSearch() {
    RTree<String> rectangles = new RTree<String>();
    rectangles.insert(new float[]{1, 1}, "Point 1,1");
    rectangles.insert(new float[]{0.5f, 0.5f}, new float[]{1, 1}, "Rectangle 0.5, 0.5, 1, 1");
    List<String> result = rectangles.search(new float[]{0, 0}, new float[]{2, 2});
    Assert.assertEquals(2, result.size());
    List<String> result2 = rectangles.search(new float[]{1.5f, 1.5f}, new float[]{2, 2});
    Assert.assertEquals(1, result2.size());

    RTree<String> ranges = new RTree<String>(1);
    ranges.insert(new float[]{1}, new float[]{1}, "range 1,2");
    ranges.insert(new float[]{2}, new float[]{2}, "range 2,4");
    Assert.assertEquals(0, ranges.search(new float[]{10}, new float[]{0}).size());
  }

}
