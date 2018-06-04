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

import java.util.BitSet;
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


    public static <T> void combination(T[] array, int of, Consumer<T[]> result) {
      BitSet bs = new BitSet(array.length);
      for (int i = 0; i < of; i++) {
        bs.set(i);
      }
      T[] ca = (T[]) java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), of);
      do {
        for (int i = 0, j = 0; i < array.length; i++) {
          if (bs.get(i)) {
            ca[j] = array[i];
            j++;
          }
        }
        result.accept(ca);
      } while(!increment(bs, array.length));
    }

    private static  boolean increment(BitSet bs, int size) {
      int length = bs.length();
      if (length == 0) {
        return true;
      }
      if (length < size) {
        bs.set(length);
        bs.clear(length - 1);
        return false;
      } else {
        int i = length - 2;
        int nrb = 1;
        while (i >= 0 && (!bs.get(i) || bs.get(i + 1))) {
          if (bs.get(i)) {
            nrb++;
          }
          i--;
        }
        if (i < 0) {
          return true;
        } else {
          bs.set(i + 1);
          bs.clear(i);
          int l = i + 2 + nrb;
          for (int j = i + 2; j < l; j++) {
            bs.set(j);
          }
          for (int j = l; j < size; j++) {
            bs.clear(j);
          }
          return false;
        }
      }
    }

}
