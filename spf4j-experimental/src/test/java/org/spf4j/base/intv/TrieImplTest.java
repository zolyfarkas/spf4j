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

import org.spf4j.base.intv.TrieMap;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Zoltan Farkas
 */
public class TrieImplTest {

  private static final Logger LOG = LoggerFactory.getLogger(TrieImplTest.class);

  @Test
  public void testTrie() {
    TrieMap<String> trie = new TrieMap();
    Assert.assertEquals(Optional.empty(), trie.get(""));
    Assert.assertEquals(Optional.empty(), trie.put("", "es"));
    Assert.assertEquals("es", trie.get("").get());
    Assert.assertEquals(Optional.empty(), trie.put("abcd", "something"));
    Assert.assertEquals(Optional.empty(), trie.put("abcde", "something else"));
    Assert.assertEquals(Optional.empty(), trie.put("cdef", "other"));
    Assert.assertEquals("something", trie.get("abcd").get());
    Assert.assertEquals("something else", trie.get("abcde").get());
    Assert.assertEquals("other", trie.get("cdef").get());
    Assert.assertEquals(Optional.empty(), trie.get("cde"));
    trie.forEach((k, v) -> {LOG.debug("k = {}; v = {}", k, v);});
  }

  public static void addAllSuffixes() {

  }


  @Test
  public void testTrieLongestCommon() {
    TrieMap<Integer> trie = new TrieMap();
  }


}
