package org.spf4j.base;

import java.io.IOException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Special methods to use for character sequences...
 * @author zoly
 */
public final class CharSequences {

  private CharSequences() {
  }

  public static int compareTo(@Nonnull final CharSequence s, @Nonnull final CharSequence t) {
    int i = 0;
    final int sl = s.length();
    final int tl = t.length();
    while (i < sl && i < tl) {
      char a = s.charAt(i);
      char b = t.charAt(i);
      int diff = a - b;
      if (diff != 0) {
        return diff;
      }
      i++;
    }
    return sl - tl;
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
    public String toString() {
      char[] chars = new char[length];
      int idx = startIdx;
      for (int i = 0; i < length; i++, idx++) {
        chars[i] = underlyingSequence.charAt(idx);
      }
      return Strings.wrap(chars);
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


  public static CharSequence toLineNumbered(final int startLineNr, final CharSequence source,  final IntAppender ia) {
    int length = source.length();
    StringBuilder destination = new StringBuilder(length + 6 * length / 80);
    try {
      lineNumbered(startLineNr, destination, ia).append(source);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    return destination;
  }


  /**
   * A more flexible version of Integer.parseInt.
   * @see java.lang.Integer.parseInt
   */
  public static int parseInt(final CharSequence s) {
    return parseInt(s, 10);
  }

  /**
   * A more flexible version of Integer.parseInt.
   * @see java.lang.Integer.parseInt
   */
  public static int parseInt(final CharSequence cs, final int radix) {
    /*
         * WARNING: This method may be invoked early during VM initialization
         * before IntegerCache is initialized. Care must be taken to not use
         * the valueOf method.
     */

    if (cs == null) {
      throw new NumberFormatException("cs is null for radix = " + radix);
    }

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
    int i = 0, len = cs.length();
    int limit = -Integer.MAX_VALUE;
    int multmin;
    int digit;

    if (len > 0) {
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
   * @see java.lang.Long.parseLong
   */
  public static long parseLong(final CharSequence cs) {
    return parseLong(cs, 10);
  }

  /**
   * A more flexible version of Long.parseLong.
   * @see java.lang.Long.parseLong
   */
  public static long parseLong(final CharSequence cs, final int radix) {
    if (cs == null) {
      throw new NumberFormatException("cs is null for radix = " + radix);
    }

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
    int i = 0, len = cs.length();
    long limit = -Long.MAX_VALUE;
    long multmin;
    int digit;

    if (len > 0) {
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


}
