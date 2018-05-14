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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Zoltan Farkas
 */
public class HeapsTest {

  private static final Logger LOG = LoggerFactory.getLogger(HeapsTest.class);


  @Test
  public void testheapify() {
    Integer[] arr = new Integer[] { 3, 4,5, 6, 7, 8, 9, 0};
    Heaps.heapify(arr);
    LOG.debug("Arr={}", (Object) arr);
    Assert.assertArrayEquals(new Integer[] {9, 7, 8, 6, 4, 3, 5, 0}, arr);
    Heaps.sort(arr);
    LOG.debug("Arr={}", (Object) arr);
    Assert.assertArrayEquals(new Integer[] {0, 3, 4, 5, 6, 7, 8, 9}, arr);
  }

}
