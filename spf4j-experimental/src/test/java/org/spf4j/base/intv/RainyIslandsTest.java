/*
 * Copyright 2018 SPF4J.
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
package org.spf4j.base.intv;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Zoltan Farkas
 */
public class RainyIslandsTest {


  @Test
  public void testWaterVolume() {
    Assert.assertEquals(0, RainyIslands.waterVolume(new int[] {}));
    Assert.assertEquals(0, RainyIslands.waterVolume(new int[] {1}));
    Assert.assertEquals(0, RainyIslands.waterVolume(new int[] {1, 1}));
    Assert.assertEquals(0, RainyIslands.waterVolume(new int[] {1, 2, 3}));
    Assert.assertEquals(0, RainyIslands.waterVolume(new int[] {3, 2, 1}));
  }

  @Test
  public void testWaterVolume2() {
    Assert.assertEquals(7, RainyIslands.waterVolume(new int[] {1, 3, 2, 1, 2, 0, 5, 1}));
    Assert.assertEquals(10, RainyIslands.waterVolume(new int[] {0, 2, 1, 3, 2, 1, 2, 0, 5, 1, 3, 1}));
  }


  @Test
  public void testWaterVolumeO() {
    Assert.assertEquals(0, RainyIslands.waterVolume2(new int[] {}));
    Assert.assertEquals(0, RainyIslands.waterVolume2(new int[] {1}));
    Assert.assertEquals(0, RainyIslands.waterVolume2(new int[] {1, 1}));
    Assert.assertEquals(0, RainyIslands.waterVolume2(new int[] {1, 2, 3}));
    Assert.assertEquals(0, RainyIslands.waterVolume2(new int[] {3, 2, 1}));
  }

  @Test
  public void testWaterVolumeO2() {
    Assert.assertEquals(7, RainyIslands.waterVolume2(new int[] {1, 3, 2, 1, 2, 0, 5, 1}));
    Assert.assertEquals(10, RainyIslands.waterVolume2(new int[] {0, 2, 1, 3, 2, 1, 2, 0, 5, 1, 3, 1}));
  }


}
