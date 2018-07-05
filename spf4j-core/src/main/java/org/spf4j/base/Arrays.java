/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Additionally licensed with:
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
package org.spf4j.base;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ObjectArrays;

/**
 * Array utilities.
 *
 * @author zoly
 */
@GwtCompatible
public final class Arrays {

  public static final Object[] EMPTY_OBJ_ARRAY = new Object[]{};

  public static final String[] EMPTY_STRING_ARRAY = new String[]{};

  public static final byte[] EMPTY_BYTE_ARRAY = new byte[]{};

  public static final long[] EMPTY_LONG_ARRAY = new long[]{};

  public static final int[] EMPTY_INT_ARRAY = new int[]{};

  private static final int ARR_CPY_THR = 128;

  private Arrays() {
  }

  public static double[] getColumnAsDoubles(final long[][] data, final int columnNumber) {
    double[] result = new double[data.length];
    for (int i = 0; i < result.length; i++) {
      result[i] = data[i][columnNumber];
    }
    return result;
  }

  public static double[] getColumn(final double[][] data, final int columnNumber) {
    double[] result = new double[data.length];
    for (int i = 0; i < result.length; i++) {
      result[i] = data[i][columnNumber];
    }
    return result;
  }

  public static double[] toDoubleArray(final long... larr) {
    double[] result = new double[larr.length];
    for (int i = 0; i < larr.length; i++) {
      result[i] = larr[i];
    }
    return result;
  }

  public static Object[] toObjectArray(final long... larr) {
    Object[] result = new Object[larr.length];
    for (int i = 0; i < larr.length; i++) {
      result[i] = larr[i];
    }
    return result;
  }

  public static double[] divide(final double[] arr1, final double[] arr2) {
    double[] result = new double[arr1.length];
    for (int i = 0; i < result.length; i++) {
      result[i] = arr1[i] / arr2[i];
    }
    return result;
  }

  public static boolean deepEquals(final Object[] a1, final Object[] a2, final int starting) {
    return deepEquals(a1, a2, starting, a1.length);
  }

  public static boolean deepEquals(final Object[] a1, final Object[] a2, final int starting, final int ending) {
    if (a1 == a2) {
      return true;
    }
    if (a1 == null || a2 == null) {
      return false;
    }
    for (int i = starting; i < ending; i++) {
      if (!java.util.Objects.deepEquals(a1[i], a2[i])) {
        return false;
      }
    }
    return true;
  }

  public static boolean equals(final byte[] a, final byte[] b, final int a1,  final int b1, final int length) {
    for (int i = a1, j = b1, k = 0; k < length; i++, j++, k++) {
      if (a[i] != b[j]) {
        return false;
      }
    }
    return true;
  }


  public static int search(final char[] array, final char c) {
    for (int i = 0; i < array.length; i++) {
      if (array[i] == c) {
        return i;
      }
    }
    return -1;
  }

  public static <T> T[] moveOfRange(final T[] original, final int from, final int to) {
    int newLength = to - from;
    if (newLength < 0) {
      throw new IllegalArgumentException(from + " > " + to);
    }
    T[] copy = ObjectArrays.newArray(original, newLength);
    for (int i = from, j = 0; i < to; i++, j++) {
      copy[j] = original[i];
      original[i] = null;
    }
    return copy;
  }

  public static <T> T[] concat(final T[]... arrays) {
    if (arrays.length < 2) {
      throw new IllegalArgumentException("You should concatenate at least 2 arrays: "
              + java.util.Arrays.deepToString(arrays));
    }
    int newLength = 0;
    for (T[] arr : arrays) {
      newLength += arr.length;
    }
    T[] result = ObjectArrays.newArray(arrays[0], newLength);
    int destIdx = 0;
    for (T[] arr : arrays) {
      System.arraycopy(arr, 0, result, destIdx, arr.length);
      destIdx += arr.length;
    }
    return result;
  }

  public static <T> int indexOf(final T[] array, final T content) {
    int result = -1;
    for (int i = 0; i < array.length; i++) {
      if (array[i].equals(content)) {
        return i;
      }
    }
    return result;
  }

  /**
   * implementation which significantly faster for large arrays (> 500).
   */
  @Beta
  public static void fill(final byte[] array, final int startIdx, final int endIdx, final byte value) {
    int len = endIdx - startIdx;
    if (len > 0) {
      if (endIdx > array.length || startIdx < 0) {
        throw new IllegalArgumentException("Illegal range from " + startIdx + " to " + endIdx);
      }
      if (len <= ARR_CPY_THR) {
        for (int i = startIdx; i < endIdx; i++) {
          array[i] = value;
        }
      } else {
        int fillEnd = startIdx + ARR_CPY_THR;
        for (int i = startIdx; i < fillEnd; i++) {
          array[i] = value;
        }
        array[startIdx] = value;
        for (int i = ARR_CPY_THR; i < len; i += i) {
          int lmi = len - i;
          int from = startIdx + i;
          if (lmi < i) {
            if (lmi < ARR_CPY_THR) {
              int xe = from + lmi;
              for (int j = from; j < xe; j++) {
                array[j] = value;
              }
            } else {
              System.arraycopy(array, startIdx, array, from, lmi);
            }
          } else {
            System.arraycopy(array, startIdx, array, from, i);
          }
        }
      }
    } else if (len < 0) {
      throw new IllegalArgumentException("Illegal range from " + startIdx + " to " + endIdx);
    }
  }


  /**
   * implementation which is significantly faster for large arrays (> 500).
   * Bechmark results:
   * Benchmark                                      Mode Cnt Score            Error           Units
   * ArraysBenchmark.testSpf4jFillSmall(10)         thrpt 10 1048892782.375   ± 29976629.818  ops/s
   * ArraysBenchmark.testjdkFillSmall(10)           thrpt 10 1046330835.509   ± 47577260.717  ops/s
   * ArraysBenchmark.testSpf4jFillMedium(100)       thrpt 10 123724912.161    ± 4049077.779   ops/s
   * ArraysBenchmark.testjdkFillMedium(100)         thrpt 10 124143139.498    ± 2044760.427   ops/s
   * ArraysBenchmark.testSpf4jFillLarge(1000)       thrpt 10 20335282.192     ± 592359.181    ops/s
   * ArraysBenchmark.testjdkFillLarge(1000)         thrpt 10 12661975.522     ± 170457.046    ops/s
   * ArraysBenchmark.testSpf4jFillVeryLarge(10000)  thrpt 10 2767351.098      ± 74871.147     ops/s
   * ArraysBenchmark.testjdkFillVeryLarge(10000     thrpt 10 1045099.669      ± 30044.505     ops/s
   *
   */
  @Beta
  public static <T> void fill(final T[] array, final int startIdx, final int endIdx, final T value) {
    int len = endIdx - startIdx;
    if (len > 0) {
      if (endIdx > array.length || startIdx < 0) {
        throw new IllegalArgumentException("Illegal range from " + startIdx + " to " + endIdx);
      }
      if (len <= ARR_CPY_THR) {
        for (int i = startIdx; i < endIdx; i++) {
          array[i] = value;
        }
      } else {
        int fillEnd = startIdx + ARR_CPY_THR;
        for (int i = startIdx; i < fillEnd; i++) {
          array[i] = value;
        }
        array[startIdx] = value;
        for (int i = ARR_CPY_THR; i < len; i += i) {
          int lmi = len - i;
          int from = startIdx + i;
          if (lmi < i) {
            if (lmi < ARR_CPY_THR) {
              int xe = from + lmi;
              for (int j = from; j < xe; j++) {
                array[j] = value;
              }
            } else {
              System.arraycopy(array, startIdx, array, from, lmi);
            }
          } else {
            System.arraycopy(array, startIdx, array, from, i);
          }
        }
      }
    } else if (len < 0) {
      throw new IllegalArgumentException("Illegal range from " + startIdx + " to " + endIdx);
    }
  }

  @Beta
  public static void fill(final char[] array, final int startIdx, final int endIdx, final char value) {
    int len = endIdx - startIdx;
    if (len > 0) {
      if (endIdx > array.length || startIdx < 0) {
        throw new IllegalArgumentException("Illegal range from " + startIdx + " to " + endIdx);
      }
      if (len <= ARR_CPY_THR) {
        for (int i = startIdx; i < endIdx; i++) {
          array[i] = value;
        }
      } else {
        int fillEnd = startIdx + ARR_CPY_THR;
        for (int i = startIdx; i < fillEnd; i++) {
          array[i] = value;
        }
        array[startIdx] = value;
        for (int i = ARR_CPY_THR; i < len; i += i) {
          int lmi = len - i;
          int from = startIdx + i;
          if (lmi < i) {
            if (lmi < ARR_CPY_THR) {
              int xe = from + lmi;
              for (int j = from; j < xe; j++) {
                array[j] = value;
              }
            } else {
              System.arraycopy(array, startIdx, array, from, lmi);
            }
          } else {
            System.arraycopy(array, startIdx, array, from, i);
          }
        }
      }
    } else if (len < 0) {
      throw new IllegalArgumentException("Illegal range from " + startIdx + " to " + endIdx);
    }
  }

  @Beta
  public static void fill(final int[] array, final int startIdx, final int endIdx, final int value) {
    int len = endIdx - startIdx;
    if (len > 0) {
      if (endIdx > array.length || startIdx < 0) {
        throw new IllegalArgumentException("Illegal range from " + startIdx + " to " + endIdx);
      }
      if (len <= ARR_CPY_THR) {
        for (int i = startIdx; i < endIdx; i++) {
          array[i] = value;
        }
      } else {
        int fillEnd = startIdx + ARR_CPY_THR;
        for (int i = startIdx; i < fillEnd; i++) {
          array[i] = value;
        }
        array[startIdx] = value;
        for (int i = ARR_CPY_THR; i < len; i += i) {
          int lmi = len - i;
          int from = startIdx + i;
          if (lmi < i) {
            if (lmi < ARR_CPY_THR) {
              int xe = from + lmi;
              for (int j = from; j < xe; j++) {
                array[j] = value;
              }
            } else {
              System.arraycopy(array, startIdx, array, from, lmi);
            }
          } else {
            System.arraycopy(array, startIdx, array, from, i);
          }
        }
      }
    } else if (len < 0) {
      throw new IllegalArgumentException("Illegal range from " + startIdx + " to " + endIdx);
    }
  }

  @Beta
  public static void fill(final long[] array, final int startIdx, final int endIdx, final long value) {
    int len = endIdx - startIdx;
    if (len > 0) {
      if (endIdx > array.length || startIdx < 0) {
        throw new IllegalArgumentException("Illegal range from " + startIdx + " to " + endIdx);
      }
      if (len <= ARR_CPY_THR) {
        for (int i = startIdx; i < endIdx; i++) {
          array[i] = value;
        }
      } else {
        int fillEnd = startIdx + ARR_CPY_THR;
        for (int i = startIdx; i < fillEnd; i++) {
          array[i] = value;
        }
        array[startIdx] = value;
        for (int i = ARR_CPY_THR; i < len; i += i) {
          int lmi = len - i;
          int from = startIdx + i;
          if (lmi < i) {
            if (lmi < ARR_CPY_THR) {
              int xe = from + lmi;
              for (int j = from; j < xe; j++) {
                array[j] = value;
              }
            } else {
              System.arraycopy(array, startIdx, array, from, lmi);
            }
          } else {
            System.arraycopy(array, startIdx, array, from, i);
          }
        }
      }
    } else if (len < 0) {
      throw new IllegalArgumentException("Illegal range from " + startIdx + " to " + endIdx);
    }
  }

  @Beta
  public static void fill(final double[] array, final int startIdx, final int endIdx, final double value) {
    int len = endIdx - startIdx;
    if (len > 0) {
      if (endIdx > array.length || startIdx < 0) {
        throw new IllegalArgumentException("Illegal range from " + startIdx + " to " + endIdx);
      }
      if (len <= ARR_CPY_THR) {
        for (int i = startIdx; i < endIdx; i++) {
          array[i] = value;
        }
      } else {
        int fillEnd = startIdx + ARR_CPY_THR;
        for (int i = startIdx; i < fillEnd; i++) {
          array[i] = value;
        }
        array[startIdx] = value;
        for (int i = ARR_CPY_THR; i < len; i += i) {
          int lmi = len - i;
          int from = startIdx + i;
          if (lmi < i) {
            if (lmi < ARR_CPY_THR) {
              int xe = from + lmi;
              for (int j = from; j < xe; j++) {
                array[j] = value;
              }
            } else {
              System.arraycopy(array, startIdx, array, from, lmi);
            }
          } else {
            System.arraycopy(array, startIdx, array, from, i);
          }
        }
      }
    } else if (len < 0) {
      throw new IllegalArgumentException("Illegal range from " + startIdx + " to " + endIdx);
    }
  }

}
