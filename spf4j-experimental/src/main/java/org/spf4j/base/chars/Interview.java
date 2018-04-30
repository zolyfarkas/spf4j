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
package org.spf4j.base.chars;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author Zoltan Farkas
 */
public class Interview {

  /**
   *  A string consists of ‘0’, ‘1’ and '?'. The question mark can be either '0' or '1'.
   * Find all possible combinations for a string.
   */
  public static void combinations(final CharSequence str, final Consumer<CharSequence> result) {
    StringBuilder sb = new StringBuilder(str);
    List<Integer> wcpos = new ArrayList<>();
    for (int i = 0, l = sb.length(); i < l; i++) {
      char c = sb.charAt(i);
      if (c == '?') {
        sb.setCharAt(i, '0');
        wcpos.add(i);
      }
    }
    int nrQ = wcpos.size();
    if (nrQ <= 0) {
      result.accept(str);
      return;
    }
    boolean carry;
    do {
      int j = 0;
      carry = true;
      do {
        Integer sPos = wcpos.get(j);
        char c = sb.charAt(sPos);
        if (c == '0') {
          sb.setCharAt(sPos, '1');
          carry = false;
          break;
        } else {
          sb.setCharAt(sPos, '0');
          j++;
        }
      } while (j < nrQ);
      result.accept(sb);
    } while (!carry);
  }


}
