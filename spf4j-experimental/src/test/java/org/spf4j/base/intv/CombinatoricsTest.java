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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Zoltan Farkas
 */
public class CombinatoricsTest {

  private static final Logger LOG = LoggerFactory.getLogger(CombinatoricsTest.class);

  @Test
  public void testPermutations() {
    Set<List<Integer>> res = new HashSet<>(30);
    Combinatorics.permuttions(new Integer[] {1, 2, 3, 4}, (a) -> {
      LOG.debug("{}", (Object) a);
      if (!res.add(Arrays.asList(a.clone()))) {
        throw new AssertionError();
      }
    });
    Assert.assertEquals(24, res.size());
  }

  @Test
  public void testConbinations() {
    Combinatorics.combination(new Integer[] {1, 2, 3, 4, 5}, 3, (a) -> {
      LOG.debug("{}", (Object) a);
    });
  }

}
