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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Zoltan Farkas
 */
public class MergesTest {

  private static final Logger LOG = LoggerFactory.getLogger(MergesTest.class);


  @Test
  public void testmerges() {
    ArrayList<Integer> result = new ArrayList<>();
    Merges.merge(Arrays.asList(1, 3, 5, 7), Arrays.asList(2, 4), result::add);
    LOG.info("res={}", result);
    Assert.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 7), result);
  }

  @Test
  public void testmerges2() {
    ArrayList<Integer> result = new ArrayList<>();
    Merges.merge(Arrays.asList(2, 4), Arrays.asList(1, 3, 5, 7), result::add);
    LOG.info("res={}", result);
    Assert.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 7), result);
  }

  @Test
  public void testmerges3() {
    ArrayList<Integer> result = new ArrayList<>();
    Merges.merge(Collections.EMPTY_LIST, Arrays.asList(1, 3, 5, 7), result::add);
    LOG.info("res={}", result);
    Assert.assertEquals(Arrays.asList(1, 3, 5, 7), result);
  }

}
