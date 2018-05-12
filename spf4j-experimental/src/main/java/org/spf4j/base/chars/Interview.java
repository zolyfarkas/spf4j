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
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;

/**
 * A set of interview questions for hands on phone screens.
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


  public static class TreeNode<T extends Comparable> {
    T value;
    TreeNode<T> left;
    TreeNode<T> right;
  }

  public static  <T extends Comparable> Optional<T> largest(@Nullable TreeNode<T> tree) {
    if (tree == null) {
      return Optional.empty();
    }
    if (tree.right == null) {
      return Optional.of(tree.value);
    } else {
      return largest(tree.right);
    }
  }

  /**
   * return the second largest value from a BST.
   */
  public static  <T extends Comparable> Optional<T> secondLargest(@Nullable TreeNode<T> tree) {
    if (tree == null) {
      return Optional.empty();
    }
    if (tree.right == null) {
      return largest(tree.left);
    } else if (tree.right.left == null && tree.right.right == null) {
      return Optional.of(tree.value);
    } else {
      return secondLargest(tree);
    }
  }


  public static int trimLZeros(CharSequence a, int as, int ae) {
    int i = as;
    while (i < ae && a.charAt(i) == '0') {
      i++;
    }
    return i;
  }


  /**
   * Compare 2 positive numbers.
   */
  public static int comparePos(CharSequence a, int pas, int ae, CharSequence b, int pbs, int be) {
    int as = trimLZeros(a, pas, ae);
    int bs = trimLZeros(b, pbs, be);
    int na = ae - as;
    int nb = be - bs;
    if (na > nb) {
      return 1;
    } else if (na < nb) {
      return -1;
    } else {
      for (int i = as, j = bs; i < na; i++, j++) {
        char ca = a.charAt(i);
        char cb = b.charAt(j);
        if (ca > cb) {
          return 1;
        } else if (ca < cb) {
          return -1;
        }
      }
      return 0;
    }
  }

  /**
   * substract 2 positive integers where a > b
   */
  public static CharSequence subPosAB(CharSequence a, int as, int ae, CharSequence b, int bs, int be) {
    int carry = 0;
    StringBuilder result = new StringBuilder();
    result.append(a, as, ae);
    for (int i = result.length() - 1, j = be - 1; i >= 0; i--, j--) {
      char c1 = result.charAt(i);
      char c2 = j >= bs ? b.charAt(j) : '0';
      int val =  c1 - carry - c2;
      if (val < 0) {
        carry = 1;
        result.setCharAt(i, (char)('0' + 10 + val));
      } else {
        carry = 0;
        result.setCharAt(i, (char)('0' + val));
      }
    }

    int trimLZeros = trimLZeros(result, 0, result.length());
    return result.subSequence(trimLZeros, result.length());
  }

  /**
   * Substract 2 positive numbers.
   * @param a
   * @param b
   * @return
   */
  public static CharSequence sub(final CharSequence a, final CharSequence b) {
    int cmp = comparePos(a, 0, a.length(), b, 0, b.length());
    if (cmp > 0) {
      return subPosAB(a, 0, a.length(), b, 0, b.length());
    } else if (cmp < 0) {
      return "-" + subPosAB(b, 0, b.length(), a, 0, a.length());
    } else {
      return "0";
    }
  }


  /**
   * divide 2 decimals.
   * @param a
   * @param b
   * @return
   */
  public static CharSequence divideInts(CharSequence a, CharSequence b) {
    return null;
  }




}
