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

import com.google.common.annotations.GwtCompatible;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.UncheckedIOException;
import static java.lang.Math.min;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Special methods to use for character sequences...
 *
 * @author zoly
 */
@GwtCompatible
public final class CharSequences {

  private CharSequences() {
  }

  /**
   * function that calculates the number of operations that are needed to transform s1 into s2. operations are: char
   * add, char delete, char modify
   * See https://en.wikipedia.org/wiki/Levenshtein_distance for more info.
   *
   * @param s1
   * @param s2
   * @return the number of operations required to transfor s1 into s2
   */
  public static int distance(@Nonnull final CharSequence s1, @Nonnull final CharSequence s2) {
    int l1 = s1.length();
    int l2 = s2.length();

    int[] prev = new int[l2];
    char c1 = s1.charAt(0);
    prev[0] = distance(c1, s2.charAt(0));
    for (int j = 1; j < l2; j++) {
      prev[j] = prev[j - 1] + distance(c1, s2.charAt(j));
    }

    for (int i = 1; i < l1; i++) {
      int[] dist = new int[l2];
      c1 = s1.charAt(i);
      dist[0] = prev[i - 1] + distance(c1, s2.charAt(0));
      for (int j = 1; j < l2; j++) {
        dist[j] = min(prev[j - 1] + distance(c1, s2.charAt(j)),
                min(prev[j] + 1, dist[j - 1] + 1));
      }
      prev = dist;
    }
    return prev[l2 - 1];
  }

  public static int distance(final char c1, final char c2) {
    return (c1 == c2) ? 0 : 1;
  }

  /**
   * compare s to t.
   *
   * @param s
   * @param t
   * @return
   * @deprecated use compare.
   */
  @Deprecated
  public static int compareTo(@Nonnull final CharSequence s, @Nonnull final CharSequence t) {
    return compare(s, t);
  }

  public static int compare(@Nonnull final CharSequence s, @Nonnull final CharSequence t) {
    return compare(s, 0, s.length(), t, 0, t.length());
  }

  public static int compare(@Nonnull final CharSequence s, final int sLength,
          @Nonnull final CharSequence t, final int tLength) {
    return compare(s, 0, sLength, t, 0, tLength);
  }

  /**
   * compare 2 CharSequence fragments.
   *
   * @param s the charsequence to compare
   * @param sFrom the index for the first chars to compare.
   * @param sLength the number of characters to compare.
   * @param t the charsequence to compare to
   * @param tFrom the index for the first character to compare to.
   * @param tLength the number of characters to compare to.
   * @return
   */
  public static int compare(@Nonnull final CharSequence s, final int sFrom, final int sLength,
          @Nonnull final CharSequence t, final int tFrom, final int tLength) {

    int lim = min(sLength, tLength);
    int i = sFrom;
    int j = tFrom;
    int sTo = sFrom + lim;
    while (i < sTo) {
      char c1 = s.charAt(i);
      char c2 = t.charAt(j);
      if (c1 != c2) {
        return c1 - c2;
      }
      i++;
      j++;
    }
    return sLength - tLength;
  }

  public static boolean equalsNullables(@Nullable final CharSequence s, @Nullable final CharSequence t) {
    if (s == null) {
      return null == t;
    } else if (t == null) {
      return true;
    } else {
      return equals(s, t);
    }
  }

  public static boolean equals(@Nonnull final CharSequence s, @Nonnull final CharSequence t) {
    final int sl = s.length();
    final int tl = t.length();
    if (sl != tl) {
      return false;
    } else {
      for (int i = 0; i < sl; i++) {
        if (s.charAt(i) != t.charAt(i)) {
          return false;
        }
      }
      return true;
    }
  }

  public static int hashcode(@Nonnull final CharSequence cs) {
    if (cs instanceof String) {
      return ((String) cs).hashCode();
    }
    int h = 0;
    int len = cs.length();
    if (len > 0) {
      int off = 0;
      for (int i = 0; i < len; i++) {
        h = 31 * h + cs.charAt(off++);
      }
    }
    return h;
  }

  public static CharSequence subSequence(@Nonnull final CharSequence seq, final int startIdx, final int endIdx) {
    if (startIdx == 0 && endIdx == seq.length()) {
      return seq;
    } else if (startIdx >= endIdx) {
      return "";
    } else {
      return new SubSequence(seq, endIdx - startIdx, startIdx);
    }
  }

  private static final class SubSequence implements CharSequence {

    private final CharSequence underlyingSequence;
    private final int length;
    private final int startIdx;

    SubSequence(final CharSequence underlyingSequence, final int length, final int startIdx) {
      this.underlyingSequence = underlyingSequence;
      this.length = length;
      this.startIdx = startIdx;
    }

    @Override
    public int length() {
      return length;
    }

    @Override
    public char charAt(final int index) {
      return underlyingSequence.charAt(startIdx + index);
    }

    @Override
    public CharSequence subSequence(final int start, final int end) {
      return CharSequences.subSequence(underlyingSequence, startIdx + start, startIdx + end);
    }

    @Override
    @SuppressFBWarnings("STT_STRING_PARSING_A_FIELD")
    public String toString() {
      if (underlyingSequence instanceof String) {
        return ((String) underlyingSequence).substring(startIdx, startIdx + length);
      } else if (underlyingSequence instanceof StringBuilder) {
        return ((StringBuilder) underlyingSequence).substring(startIdx, startIdx + length);
      } else {
        char[] chars = new char[length];
        int idx = startIdx;
        for (int i = 0; i < length; i++, idx++) {
          chars[i] = underlyingSequence.charAt(idx);
        }
        return new String(chars);
      }
    }

  }

  public static boolean endsWith(final CharSequence qc, final CharSequence with) {
    int l = qc.length();
    int start = l - with.length();
    if (start >= 0) {
      for (int i = start, j = 0; i < l; i++, j++) {
        if (qc.charAt(i) != with.charAt(j)) {
          return false;
        }
      }
      return true;
    } else {
      return false;
    }
  }

  public static Appendable lineNumbered(final int startLineNr, final Appendable appendable)
          throws IOException {
    return lineNumbered(startLineNr, appendable, IntAppender.CommentNumberAppender.INSTANCE);
  }

  public static Appendable lineNumbered(final int startLineNr, final Appendable appendable, final IntAppender ia)
          throws IOException {
    ia.append(startLineNr, appendable);
    return new Appendable() {
      private int lineNr = startLineNr + 1;

      @Override
      public Appendable append(final CharSequence csq) throws IOException {
        return append(csq, 0, csq.length());
      }

      @Override
      public Appendable append(final CharSequence csq, final int start, final int end) throws IOException {
        int lastIdx = start;
        for (int i = start; i < end; i++) {
          if (csq.charAt(i) == '\n') {
            int next = i + 1;
            appendable.append(csq, lastIdx, next);
            ia.append(lineNr++, appendable);
            lastIdx = next;
          }
        }
        if (lastIdx < end) {
          appendable.append(csq, lastIdx, end);
        }
        return this;
      }

      @Override
      public Appendable append(final char c) throws IOException {
        appendable.append(c);
        if (c == '\n') {
          ia.append(lineNr++, appendable);
        }
        return this;
      }
    };
  }

  public static CharSequence toLineNumbered(final int startLineNr, final CharSequence source) {
    return toLineNumbered(startLineNr, source, IntAppender.CommentNumberAppender.INSTANCE);
  }

  public static CharSequence toLineNumbered(final int startLineNr, final CharSequence source, final IntAppender ia) {
    int length = source.length();
    StringBuilder destination = new StringBuilder(length + 6 * length / 80);
    try {
      lineNumbered(startLineNr, destination, ia).append(source);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return destination;
  }

  /**
   * A more flexible version of Integer.parseInt.
   *
   * @see java.lang.Integer.parseInt
   */
  public static int parseInt(@Nonnull final CharSequence s) {
    return parseInt(s, 10);
  }

  /**
   * A more flexible version of Integer.parseInt.
   *
   * @see java.lang.Integer.parseInt
   */
  public static int parseInt(@Nonnull final CharSequence cs, final int radix) {

    if (radix < Character.MIN_RADIX) {
      throw new NumberFormatException("radix " + radix
              + " less than Character.MIN_RADIX");
    }

    if (radix > Character.MAX_RADIX) {
      throw new NumberFormatException("radix " + radix
              + " greater than Character.MAX_RADIX");
    }

    int result = 0;
    boolean negative = false;
    int len = cs.length();

    if (len > 0) {
      int i = 0;
      int limit = -Integer.MAX_VALUE;
      int multmin;
      int digit;
      char firstChar = cs.charAt(0);
      if (firstChar < '0') { // Possible leading "+" or "-"
        if (firstChar == '-') {
          negative = true;
          limit = Integer.MIN_VALUE;
        } else if (firstChar != '+') {
          throw new NumberFormatException("For input char sequence: \"" + cs + '\"');
        }

        if (len == 1) { // Cannot have lone "+" or "-"
          throw new NumberFormatException("For input char sequence: \"" + cs + '\"');
        }
        i++;
      }
      multmin = limit / radix;
      while (i < len) {
        // Accumulating negatively avoids surprises near MAX_VALUE
        digit = Character.digit(cs.charAt(i++), radix);
        if (digit < 0) {
          throw new NumberFormatException("For input char sequence: \"" + cs + '\"');
        }
        if (result < multmin) {
          throw new NumberFormatException("For input char sequence: \"" + cs + '\"');
        }
        result *= radix;
        if (result < limit + digit) {
          throw new NumberFormatException("For input char sequence: \"" + cs + '\"');
        }
        result -= digit;
      }
    } else {
      throw new NumberFormatException("For input char sequence: \"" + cs + '\"');
    }
    return negative ? result : -result;
  }

  /**
   * A more flexible version of Long.parseLong.
   *
   * @see java.lang.Long.parseLong
   */
  public static long parseLong(@Nonnull final CharSequence cs) {
    return parseLong(cs, 10);
  }

  /**
   * A more flexible version of Long.parseLong.
   *
   * @see java.lang.Long.parseLong
   */
  public static long parseLong(@Nonnull final CharSequence cs, final int radix) {

    if (radix < Character.MIN_RADIX) {
      throw new NumberFormatException("radix " + radix
              + " less than Character.MIN_RADIX");
    }
    if (radix > Character.MAX_RADIX) {
      throw new NumberFormatException("radix " + radix
              + " greater than Character.MAX_RADIX");
    }

    long result = 0;
    boolean negative = false;
    int len = cs.length();

    if (len > 0) {
      int i = 0;
      long limit = -Long.MAX_VALUE;
      long multmin;
      int digit;
      char firstChar = cs.charAt(0);
      if (firstChar < '0') { // Possible leading "+" or "-"
        if (firstChar == '-') {
          negative = true;
          limit = Long.MIN_VALUE;
        } else if (firstChar != '+') {
          throw new NumberFormatException("For input char sequence: \"" + cs + '\"');
        }

        if (len == 1) { // Cannot have lone "+" or "-"
          throw new NumberFormatException("For input char sequence: \"" + cs + '\"');
        }
        i++;
      }
      multmin = limit / radix;
      while (i < len) {
        // Accumulating negatively avoids surprises near MAX_VALUE
        digit = Character.digit(cs.charAt(i++), radix);
        if (digit < 0) {
          throw new NumberFormatException("For input char sequence: \"" + cs + '\"');
        }
        if (result < multmin) {
          throw new NumberFormatException("For input char sequence: \"" + cs + '\"');
        }
        result *= radix;
        if (result < limit + digit) {
          throw new NumberFormatException("For input char sequence: \"" + cs + '\"');
        }
        result -= digit;
      }
    } else {
      throw new NumberFormatException("For input char sequence: \"" + cs + '\"');
    }
    return negative ? result : -result;
  }

  public static boolean containsAnyChar(final CharSequence string, final char... chars) {
    for (int i = 0; i < string.length(); i++) {
      char c = string.charAt(i);
      if (Arrays.search(chars, c) >= 0) {
        return true;
      }
    }
    return false;
  }

  public static boolean isValidFileName(@Nonnull final CharSequence fileName) {
    return !containsAnyChar(fileName, '/', '\\');
  }

  public static <T extends CharSequence> T validatedFileName(@Nonnull final T fileName) {
    if (!isValidFileName(fileName)) {
      throw new IllegalArgumentException("Invalid file name: " + fileName);
    }
    return fileName;
  }

  /**
   * Equivalent to String.regionMatches.
   */
  public static boolean regionMatches(final CharSequence t, final int toffset,
          final CharSequence other, final int ooffset, final int plen) {
    int to = toffset;
    int po = ooffset;
    // Note: toffset, ooffset, or len might be near -1>>>1.
    if ((ooffset < 0) || (toffset < 0) || (toffset > (long) t.length() - plen)
            || (ooffset > (long) other.length() - plen)) {
      return false;
    }
    int len = plen;
    while (len-- > 0) {
      if (t.charAt(to++) != other.charAt(po++)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Equivalent/based on to String.regionMatches.
   */
  public static boolean regionMatchesIgnoreCase(final CharSequence ta, final int toffset,
          final CharSequence pa, final int ooffset, final int plen) {
    int to = toffset;
    int po = ooffset;
    // Note: toffset, ooffset, or len might be near -1>>>1.
    if ((ooffset < 0) || (toffset < 0)
            || (toffset > (long) ta.length() - plen)
            || (ooffset > (long) pa.length() - plen)) {
      return false;
    }
    int len = plen;
    while (len-- > 0) {
      char c1 = ta.charAt(to++);
      char c2 = pa.charAt(po++);
      if (c1 == c2) {
        continue;
      }
      // If characters don't match but case may be ignored,
      // try converting both characters to uppercase.
      // If the results match, then the comparison scan should
      // continue.
      char u1 = Character.toUpperCase(c1);
      char u2 = Character.toUpperCase(c2);
      if (u1 == u2) {
        continue;
      }
      // Unfortunately, conversion to uppercase does not work properly
      // for the Georgian alphabet, which has strange rules about case
      // conversion.  So we need to make one last check before
      // exiting.
      if (Character.toLowerCase(u1) != Character.toLowerCase(u2)) {
        return false;
      }
    }
    return true;
  }

  /**
   * regular wildcard matcher.
   * * matches any number of consecutive characters.
   * ? matches any single character.
   * @param wildcard
   * @param cs2Match
   * @return
   */
  public static boolean match(final CharSequence wildcard, final CharSequence cs2Match) {
    int i = 0;
    int j = 0;
    final int length = wildcard.length();
    for (; i < length; i++, j++) {
      final char some2 = wildcard.charAt(i);
      if (some2 != cs2Match.charAt(j)) {
        if (some2 == '*') {
          i++;
          if (i == length) {
            return true;
          }
          final char some = wildcard.charAt(i);
          while (some != cs2Match.charAt(j)) {
            ++j;
          }
          j--;
        } else if (some2 != '?') {
          return false;
        }
      }
    }
    return j == cs2Match.length();
  }


  /**
   * Transform a wildcard expression 2 a java regular expression.
   * * matches any number of consecutive characters.
   * ? matches any single character.
   * @param wildcard
   * @return
   */
  public CharSequence getJavaRegexpStr(final CharSequence wildcard) {
    final int length = wildcard.length();
    final StringBuilder buff = new StringBuilder(length + 4);
    for (int i = 0; i < length; i++) {
      final char c = wildcard.charAt(i);
      switch (c) {
        case '*':
          buff.append(".*");
          break;
        case '?':
          buff.append('.');
          break;
        case '[':
        case ']':
        case '(':
        case ')':
        case '{':
        case '}':
        case '.':
          buff.append('\\').append(c);
          break;
        default:
          buff.append(c);
      }
    }
    return buff;
  }


  public static int indexOf(final CharSequence cs, final int from, final int to, final char c) {
      for (int i = from; i < to; i++) {
        if (c == cs.charAt(i)) {
          return i;
        }
      }
      return -1;
  }

}
