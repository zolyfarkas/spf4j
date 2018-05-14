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

/**
 *
 * @author Zoltan Farkas
 */
public class Heaps {

  private static <T extends Comparable<T>> void fix(T[] arr, final int start, final int end) {
    int i = start;
    while (true) {
      int lIdx = (i << 1) + 1;
      if (lIdx >= end) {
        break;
      }
      int rIdx = lIdx + 1;
      T n = arr[i];
      T l = arr[lIdx];
      if (rIdx >= end) {
        if (l.compareTo(n) > 0) {
          arr[i] = l;
          arr[lIdx] = n;
          i = lIdx;
        } else {
          break;
        }
      } else {
        T r = arr[rIdx];
        if (l.compareTo(r) > 0) {
          if (l.compareTo(n) > 0) {
            arr[i] = l;
            arr[lIdx] = n;
            i = lIdx;
          } else {
            break;
          }
        } else {
          if (r.compareTo(n) > 0) {
            arr[i] = r;
            arr[rIdx] = n;
            i = rIdx;
          } else {
            break;
          }
        }
      }
    }

  }

  public static <T extends Comparable<T>> void heapify(T[] arr) {
    int l = arr.length;
    if (l <= 1) {
      return;
    }
    int i = l - 1;
    i = (i % 2) == 0 ? (i - 2) >>> 1 : (i - 1) >>> 1;
    while (i >= 0) {
      fix(arr, i, l);
      i--;
    }
  }

  public static <T extends Comparable<T>> void sort(T[] arr) {
    heapify(arr);
    int i = arr.length - 1;
    while (i > 0) {
      T tmp = arr[0];
      arr[0] = arr[i];
      arr[i] = tmp;
      fix(arr, 0, i);
      i--;
    }
  }


}
