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
package org.spf4j.stackmonitor;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.avro.Method;

/**
 *
 * @author Zoltan Farkas
 */
public class MethodMapTest {

  private static final Logger LOG = LoggerFactory.getLogger(MethodMapTest.class);


  @Test
  public void test() {
    MethodMap<Integer> map = new MethodMap(0);
    Assert.assertEquals(0, map.size());
    Method m = new Method("org.apache.avro.Schema", "toString");
    Assert.assertNull(map.get(m));
    Assert.assertNull(map.get(null));
    map.forEachEntry((a, b) -> true);
    map.forEachKey((a) -> true);
    map.forEachValue((a) -> true);
    Assert.assertTrue(map.entrySet().isEmpty());
    Assert.assertTrue(map.values().isEmpty());
    Assert.assertTrue(map.keySet().isEmpty());
    Method m2 = new Method("org.apache.avro.Schema", "toString2");
    map.put(m2, Integer.MIN_VALUE);
    map.put(m, Integer.MAX_VALUE);
    Assert.assertEquals(2, map.size());
    Assert.assertEquals((Integer) Integer.MIN_VALUE, map.get(m2));
    Assert.assertEquals((Integer) Integer.MAX_VALUE, map.get(m));
  }

  @Test
  public void test2() {
    MethodMap<Integer> map = new MethodMap(0);
    Assert.assertEquals(0, map.capacity());
    Method m1 = new Method("x", "m1");
    map.put(m1, 0);
    Assert.assertEquals(3, map.capacity());
    Assert.assertEquals(1, map._size);
    map.put(m1, 1);
    Assert.assertEquals(3, map.capacity());
    Assert.assertEquals(1, map._size);
    Assert.assertEquals(2, map._maxSize);
    Method m2 = new Method("x", "m2");
    map.put(m2, 2);
    Assert.assertEquals(3, map.capacity());
    Assert.assertEquals(2, map._size);
    Assert.assertEquals(2, map._maxSize);
    Method m3 = new Method("x", "m3");
    map.put(m3, 3);
    Assert.assertEquals(7, map.capacity());
    Assert.assertEquals(3, map._size);
    Assert.assertEquals(4, map._maxSize);
    Assert.assertEquals((Integer) 1, map.get(m1));
    Assert.assertEquals((Integer) 2, map.get(m2));
    Assert.assertEquals((Integer) 3, map.get(m3));
    Method ym3 = new Method("y", "m3");
    Assert.assertNull(map.get(ym3));
    map.put(ym3, 13);
    Assert.assertEquals((Integer) 13, map.get(ym3));
    List<Integer> values = new ArrayList<>();
    map.forEachEntry((k, v) -> {
      values.add(v);
      return true;
    });
    Assert.assertEquals(4, values.size());

  }
}
