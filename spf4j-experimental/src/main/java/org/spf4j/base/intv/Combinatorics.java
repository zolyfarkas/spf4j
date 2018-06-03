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

import java.util.function.Consumer;

/**
 * @author Zoltan Farkas
 */
public final class Combinatorics {

  public static <T> void permuttions(T[] array, Consumer<T[]> result) {
    permutations(array, array.length, result);
  }

  private static <T> void permutations(T[] array, int to, Consumer<T[]> result) {
    if (to < 2) {
      result.accept(array);
    } else {
      int lIdx = to - 1;
      T last = array[lIdx];
      permutations(array, lIdx, result);
      for (int i = 0; i < lIdx; i++) {
        array[lIdx] = array[i];
        array[i] = last;
        permutations(array, lIdx, result);
        array[i] = array[lIdx];
        array[lIdx] = last;
      }
    }
  }


}
