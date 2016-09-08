
/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
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
 */
package org.spf4j.base;

import com.google.common.base.Charsets;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//CHECKSTYLE:OFF
import sun.nio.cs.ArrayDecoder;
import sun.nio.cs.ArrayEncoder;
//CHECKSTYLE:ON
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import static java.lang.Math.min;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("IICU_INCORRECT_INTERNAL_CLASS_USE")
public final class Strings {

    private Strings() {
    }

    public static final String EOL = System.getProperty("line.separator", "\n");

    /**
     * function that calculates the number of operations that are needed to transform s1 into s2. operations are: char
     * add, char delete, char modify
     *
     * @param s1
     * @param s2
     * @return the number of operations required to transfor s1 into s2
     */
    public static int distance(@Nonnull final String s1, @Nonnull final String s2) {
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
        if (c1 == c2) {
            return 0;
        } else {
            return 1;
        }
    }

    private static final String[][] JAVA_CTRL_CHARS_UNESCAPE = {
        {"\\b", "\b"},
        {"\\n", "\n"},
        {"\\t", "\t"},
        {"\\f", "\f"},
        {"\\r", "\r"}
    };

    private static final CharSequenceTranslator UNESCAPE_JAVA
            = new AggregateTranslator(
                    new OctalUnescaper(),
                    new UnicodeUnescaper(),
                    new LookupTranslator(JAVA_CTRL_CHARS_UNESCAPE),
                    new LookupTranslator(
                            new String[][]{
                                {"\\\\", "\\"},
                                {"\\\"", "\""},
                                {"\\'", "'"},
                                {"\\", ""}
                            })
            );

    public static String unescape(final String what) {
        return UNESCAPE_JAVA.translate(what);
    }

    public static boolean contains(final String string, final char... chars) {
        for (char c : chars) {
            if (string.indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(final CharSequence string, final char... chars) {
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (Arrays.search(chars, c) >= 0) {
                return true;
            }
        }
        return false;
    }

    public static String withFirstCharLower(final String str) {
        if (str.isEmpty()) {
            return str;
        }
        if (Character.isLowerCase(str.charAt(0))) {
            return str;
        }
        int l = str.length();
        StringBuilder result = new StringBuilder(l);
        result.append(Character.toLowerCase(str.charAt(0)));
        for (int i = 1; i < l; i++) {
            result.append(str.charAt(i));
        }
        return result.toString();
    }

    public static void writeReplaceWhitespaces(final String str, final char replacement, final Appendable writer)
            throws IOException {
        for (char c : steal(str)) {
            if (Character.isWhitespace(c)) {
                writer.append(replacement);
            } else {
                writer.append(c);
            }
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(Strings.class);

    private static final MethodHandle CHARS_FIELD_GET;

    //String(char[] value, boolean share) {
    private static final MethodHandle PROTECTED_STR_CONSTR_HANDLE;
    private static final Class<?>[] PROTECTED_STR_CONSTR_PARAM_TYPES;

  static {
    Field charsField = AccessController.doPrivileged(new PrivilegedAction<Field>() {
      @Override
      public Field run() {
        Field charsField;
        try {
          charsField = String.class.getDeclaredField("value");
          charsField.setAccessible(true);
        } catch (NoSuchFieldException ex) {
          LOG.info("char array stealing from String not supported", ex);
          charsField = null;
        } catch (SecurityException ex) {
          throw new RuntimeException(ex);
        }
        return charsField;
      }
    });
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    try {
      CHARS_FIELD_GET = lookup.unreflectGetter(charsField);
    } catch (IllegalAccessException ex) {
      throw new ExceptionInInitializerError(ex);
    }

    // up until u45 String(int offset, int count, char value[]) {
    // u45 reverted to: String(char[] value, boolean share) {
    Constructor<String> prConstr = AccessController.doPrivileged(
            new PrivilegedAction<Constructor<String>>() {
      @Override
      public Constructor<String> run() {
        Constructor<String> constr;
        try {
          constr = String.class.getDeclaredConstructor(char[].class, boolean.class);
          constr.setAccessible(true);
        } catch (NoSuchMethodException ex) {
          try {
            constr = String.class.getDeclaredConstructor(int.class, int.class, char[].class);
            constr.setAccessible(true);
          } catch (NoSuchMethodException ex2) {
            ex2.addSuppressed(ex);
            LOG.info("building String from char[] fast not supported", ex2);
            constr = null;
          } catch (SecurityException ex2) {
            ex2.addSuppressed(ex);
            throw new RuntimeException(ex2);
          }
        } catch (SecurityException ex) {
          throw new RuntimeException(ex);
        }
        return constr;
      }
    });

    if (prConstr == null) {
      PROTECTED_STR_CONSTR_PARAM_TYPES = null;
      PROTECTED_STR_CONSTR_HANDLE = null;
    } else {
      PROTECTED_STR_CONSTR_PARAM_TYPES = prConstr.getParameterTypes();
      try {
        PROTECTED_STR_CONSTR_HANDLE = lookup.unreflectConstructor(prConstr);
      } catch (IllegalAccessException ex) {
        throw new ExceptionInInitializerError(ex);
      }
    }
  }

    /**
     * Steal the underlying character array of a String.
     *
     * @param str
     * @return
     */
    public static char[] steal(final String str) {
        if (CHARS_FIELD_GET == null) {
            return str.toCharArray();
        } else {
            try {
                return (char[]) CHARS_FIELD_GET.invokeExact(str);
            } catch (RuntimeException | Error ex) {
              throw ex;
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Create a String based on the provided character array. No copy of the array is made.
     *
     * @param chars
     * @return
     */
    public static String wrap(final char[] chars) {
        if (PROTECTED_STR_CONSTR_PARAM_TYPES != null) {
            try {
                if (PROTECTED_STR_CONSTR_PARAM_TYPES.length == 3) {
                    return (String) PROTECTED_STR_CONSTR_HANDLE.invokeExact(0, chars.length, chars);
                } else {
                    return (String) PROTECTED_STR_CONSTR_HANDLE.invokeExact(chars, true);
                }
            } catch (Error | RuntimeException ex) {
              throw ex;
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        } else {
            return new String(chars);
        }
    }

    private static final boolean LENIENT_CODING  = Boolean.getBoolean("spf4j.encoding.lenient");

    private static final ThreadLocal<CharsetEncoder> UTF8_ENCODER = new ThreadLocal<CharsetEncoder>() {

        @Override
        protected CharsetEncoder initialValue() {
            return createUtf8Encoder();
        }

    };

  public static CharsetEncoder createUtf8Encoder() {
    if (LENIENT_CODING) {
      return Charsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPLACE)
              .onUnmappableCharacter(CodingErrorAction.REPLACE);
    } else {
      return Charsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT);
    }
  }

    private static final ThreadLocal<CharsetDecoder> UTF8_DECODER = new ThreadLocal<CharsetDecoder>() {

        @Override
        protected CharsetDecoder initialValue() {
          return createUtf8Decoder();
        }

    };

  public static CharsetDecoder createUtf8Decoder() {
    if (LENIENT_CODING) {
      return Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE)
              .onUnmappableCharacter(CodingErrorAction.REPLACE);
    } else {
      return Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT);
    }
  }

    public static CharsetEncoder getUTF8CharsetEncoder() {
        return UTF8_ENCODER.get();
    }

    public static CharsetDecoder getUTF8CharsetDecoder() {
        return UTF8_DECODER.get();
    }

    @SuppressFBWarnings("SUA_SUSPICIOUS_UNINITIALIZED_ARRAY")
    public static byte[] encode(final CharsetEncoder ce, final char[] ca, final int off, final int len) {
        if (len == 0) {
            return Arrays.EMPTY_BYTE_ARRAY;
        }
        byte[] ba = Arrays.getBytesTmp(getmaxNrBytes(ce, len));
        int nrBytes = encode(ce, ca, off, len, ba);
        return java.util.Arrays.copyOf(ba, nrBytes);
    }

    public static int getmaxNrBytes(final CharsetEncoder ce, final int nrChars) {
        return (int) (nrChars * (double) ce.maxBytesPerChar());
    }

    public static int encode(final CharsetEncoder ce, final char[] ca, final int off, final int len,
            final byte[] targetArray) {
        if (len == 0) {
            return 0;
        }
        if (ce instanceof ArrayEncoder) {
            return ((ArrayEncoder) ce).encode(ca, off, len, targetArray);
        } else {
            ce.reset();
            ByteBuffer bb = ByteBuffer.wrap(targetArray);
            CharBuffer cb = CharBuffer.wrap(ca, off, len);
            try {
                CoderResult cr = ce.encode(cb, bb, true);
                if (!cr.isUnderflow()) {
                    cr.throwException();
                }
                cr = ce.flush(bb);
                if (!cr.isUnderflow()) {
                    cr.throwException();
                }
            } catch (CharacterCodingException x) {
                throw new Error(x);
            }
            return bb.position();
        }
    }

    @SuppressFBWarnings("SUA_SUSPICIOUS_UNINITIALIZED_ARRAY")
    public static String decode(final CharsetDecoder cd, final byte[] ba, final int off, final int len) {
        if (len == 0) {
            return "";
        }
        int en = (int) (len * (double) cd.maxCharsPerByte());
        char[] ca = Arrays.getCharsTmp(en);
        if (cd instanceof ArrayDecoder) {
            int clen = ((ArrayDecoder) cd).decode(ba, off, len, ca);
            return new String(ca, 0, clen);
        }
        cd.reset();
        ByteBuffer bb = ByteBuffer.wrap(ba, off, len);
        CharBuffer cb = CharBuffer.wrap(ca);
        try {
            CoderResult cr = cd.decode(bb, cb, true);
            if (!cr.isUnderflow()) {
                cr.throwException();
            }
            cr = cd.flush(cb);
            if (!cr.isUnderflow()) {
                cr.throwException();
            }
        } catch (CharacterCodingException x) {
            throw new Error(x);
        }
        return new String(ca, 0, cb.position());
    }

    /**
     * Optimized UTF8 decoder.
     *
     * Here is a benchmark comparison with the JDK implementation
     * (see EncodingBenchmark.java in the benchmark project):
     *
     * EncodingBenchmark.stringDecode             thrpt   10  16759798.463 ± 343505.144  ops/s
     * EncodingBenchmark.fastStringDecode         thrpt   10  17413298.464 ± 301756.867  ops/s
     *
     *
     * @param bytes
     * @return
     */
    public static String fromUtf8(final byte[] bytes) {
        return decode(UTF8_DECODER.get(), bytes, 0, bytes.length);
    }

    public static String fromUtf8(final byte[] bytes, final int startIdx, final int length) {
        return decode(UTF8_DECODER.get(), bytes, startIdx, length);
    }

    /**
     * Optimized UTF8 string encoder.
     *
     * comparison with the stock JDK implementation
     * (see EncodingBenchmark.java in the benchmark project):
     *
     * EncodingBenchmark.stringEncode             thrpt   10   9481668.776 ± 252543.135  ops/s
     * EncodingBenchmark.fastStringEncode         thrpt   10  22469383.612 ± 898677.892  ops/s
     *
     * @param str
     * @return
     */
    public static byte[] toUtf8(final String str) {
        final char[] chars = steal(str);
        return encode(UTF8_ENCODER.get(), chars, 0, chars.length);
    }

  @Deprecated
  public static int compareTo(@Nonnull final CharSequence s, @Nonnull final CharSequence t) {
    return CharSequences.compareTo(s, t);
  }

  @Deprecated
  public static boolean equals(@Nonnull final CharSequence s, @Nonnull final CharSequence t) {
    return CharSequences.equals(s, t);
  }

  @Deprecated
  public static int hashcode(final CharSequence cs) {
    return CharSequences.hashcode(cs);
  }

  @Deprecated
  public static CharSequence subSequence(final CharSequence seq, final int startIdx, final int endIdx) {
    return CharSequences.subSequence(seq, startIdx, endIdx);
  }

  @Deprecated
  public static boolean endsWith(final CharSequence qc, final CharSequence with) {
    return CharSequences.endsWith(qc, with);
  }

  /**
   * Utility method to escape java strings to json strings.
   *
   * @param toEscape - the java string to escape.
   * @param jsonString - the destination json String builder.
   */
  public static void escapeJsonString(@Nonnull final String toEscape, final StringBuilder jsonString) {

    int len = toEscape.length();
    for (int i = 0; i < len; i++) {
      char c = toEscape.charAt(i);
      appendJsonStringEscapedChar(c, jsonString);
    }

  }

  public static void escapeJsonString(@Nonnull final String toEscape, final Appendable jsonString) throws IOException {
    int len = toEscape.length();
    for (int i = 0; i < len; i++) {
      char c = toEscape.charAt(i);
      appendJsonStringEscapedChar(c, jsonString);
    }
  }


  public static void appendJsonStringEscapedChar(final char c, final StringBuilder jsonString) {
    switch (c) {
      case '\\':
      case '"':
        jsonString.append('\\');
        jsonString.append(c);
        break;
      case '\b':
        jsonString.append("\\b");
        break;
      case '\t':
        jsonString.append("\\t");
        break;
      case '\n':
        jsonString.append("\\n");
        break;
      case '\f':
        jsonString.append("\\f");
        break;
      case '\r':
        jsonString.append("\\r");
        break;
      default:
        if (c < ' ') {
          jsonString.append("\\u");
          appendUnsignedStringPadded(jsonString, (int) c, 4, 4);
        } else {
          jsonString.append(c);
        }
    }
  }


  public static void appendJsonStringEscapedChar(final char c, final Appendable jsonString) throws IOException {
    switch (c) {
      case '\\':
      case '"':
        jsonString.append('\\');
        jsonString.append(c);
        break;
      case '\b':
        jsonString.append("\\b");
        break;
      case '\t':
        jsonString.append("\\t");
        break;
      case '\n':
        jsonString.append("\\n");
        break;
      case '\f':
        jsonString.append("\\f");
        break;
      case '\r':
        jsonString.append("\\r");
        break;
      default:
        if (c < ' ') {
          jsonString.append("\\u");
          appendUnsignedStringPadded(jsonString, (int) c, 4, 4);
        } else {
          jsonString.append(c);
        }
    }
  }

    private static final char[] DIGITS = {
        '0', '1', '2', '3', '4', '5',
        '6', '7', '8', '9', 'a', 'b',
        'c', 'd', 'e', 'f', 'g', 'h',
        'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't',
        'u', 'v', 'w', 'x', 'y', 'z'
    };

    private static final ThreadLocal<char[]> BUFF = new ThreadLocal<char[]>() {

        @Override
        @SuppressFBWarnings("SUA_SUSPICIOUS_UNINITIALIZED_ARRAY")
        protected char[] initialValue() {
            return new char[64];
        }

    };

    public static void appendUnsignedString(final StringBuilder sb, final long nr, final int shift) {
        long i = nr;
        char[] buf = BUFF.get();
        int charPos = 64;
        int radix = 1 << shift;
        long mask = radix - 1;
        do {
            buf[--charPos] = DIGITS[(int) (i & mask)];
            i >>>= shift;
        } while (i != 0);
        sb.append(buf, charPos, 64 - charPos);
    }

    public static void appendUnsignedString(final StringBuilder sb, final int nr, final int shift) {
        long i = nr;
        char[] buf = BUFF.get();
        int charPos = 32;
        int radix = 1 << shift;
        long mask = radix - 1;
        do {
            buf[--charPos] = DIGITS[(int) (i & mask)];
            i >>>= shift;
        } while (i != 0);
        sb.append(buf, charPos, 32 - charPos);
    }

    public static void appendUnsignedStringPadded(final StringBuilder sb, final int nr, final int shift,
            final int padTo) {
        long i = nr;
        char[] buf = BUFF.get();
        int charPos = 32;
        int radix = 1 << shift;
        long mask = radix - 1;
        do {
            buf[--charPos] = DIGITS[(int) (i & mask)];
            i >>>= shift;
        } while (i != 0);
        final int nrChars = 32 - charPos;
        if (nrChars > padTo) {
            throw new IllegalArgumentException("Your pad to value " + padTo
                    + " is to small, must be at least " + nrChars);
        } else {
            for (int j = 0, n = padTo - nrChars; j < n; j++) {
                sb.append('0');
            }
        }
        sb.append(buf, charPos, nrChars);
    }

    public static void appendUnsignedStringPadded(final Appendable sb, final int nr, final int shift,
            final int padTo) throws IOException {
        long i = nr;
        char[] buf = BUFF.get();
        int charPos = 32;
        int radix = 1 << shift;
        long mask = radix - 1;
        do {
            buf[--charPos] = DIGITS[(int) (i & mask)];
            i >>>= shift;
        } while (i != 0);
        final int nrChars = 32 - charPos;
        if (nrChars > padTo) {
            throw new IllegalArgumentException("Your pad to value " + padTo
                    + " is to small, must be at least " + nrChars);
        } else {
            for (int j = 0, n = padTo - nrChars; j < n; j++) {
                sb.append('0');
            }
        }
        sb.append(CharBuffer.wrap(buf), charPos, charPos + nrChars);
    }

    public static void appendSpaces(final Appendable to, final int nrSpaces) throws IOException {
      for (int i = 0; i < nrSpaces; i++) {
        to.append(' ');
      }
    }

    public static void appendSpaces(final StringBuilder to, final int nrSpaces) {
      for (int i = 0; i < nrSpaces; i++) {
        to.append(' ');
      }
    }

  /**
   * See String.regionMatches.
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

  public static String truncate(@Nonnull final String value, final int length) {
    if (value.length() > length) {
      return value.substring(0, length);
    } else {
      return value;
    }
  }


}
