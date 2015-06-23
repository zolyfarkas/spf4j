
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
import java.io.Writer;
import static java.lang.Math.min;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
/**
 *
 * @author zoly
 */
public final class Strings {

    private Strings() {
    }

    public static final String EOL = System.getProperty("line.separator", "\n");

    /**
     * function that calculates the number of operations that are needed to transform s1 into s2.
     * operations are: char
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

    public static void writeReplaceWhitespaces(final String str, final char replacement, final Writer writer)
            throws IOException {
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isWhitespace(c)) {
                writer.append(replacement);
            } else {
                writer.append(c);
            }
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(Strings.class);

    private static final Field CHARS_FIELD;

    //String(char[] value, boolean share) {
    private static final Constructor<String> PROTECTED_STR_CONSTR;
    private static final Class<?> [] PROTECTED_STR_CONSTR_PARAM_TYPES;

    static {
        CHARS_FIELD = AccessController.doPrivileged(new PrivilegedAction<Field>() {
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

        if (Runtime.JAVA_PLATFORM.ordinal() >= Runtime.Version.V1_8.ordinal()) {
            // up until u45 String(int offset, int count, char value[]) {
            // u45 reverted to: String(char[] value, boolean share) {
        PROTECTED_STR_CONSTR = AccessController.doPrivileged(new PrivilegedAction<Constructor<String>>() {
                @Override
                public Constructor<String> run() {
                    Constructor<String> constr;
                    try {
                        constr = String.class.getDeclaredConstructor(int.class, int.class, char[].class);
                        constr.setAccessible(true);
                    } catch (NoSuchMethodException ex) {
                        try {
                            constr = String.class.getDeclaredConstructor(char[].class, boolean.class);
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

        } else {
        PROTECTED_STR_CONSTR = AccessController.doPrivileged(new PrivilegedAction<Constructor<String>>() {
                @Override
                public Constructor<String> run() {
                    Constructor<String> constr;
                    try {
                        constr = String.class.getDeclaredConstructor(char[].class, boolean.class);
                        constr.setAccessible(true);
                    } catch (NoSuchMethodException ex) {
                        LOG.info("building String from char[] fast not supported", ex);
                        constr = null;
                    } catch (SecurityException ex) {
                        throw new RuntimeException(ex);
                    }
                    return constr;
                }
            });
        }
        PROTECTED_STR_CONSTR_PARAM_TYPES = PROTECTED_STR_CONSTR.getParameterTypes();

    }

    /**
     * Steal the underlying character array of a String.
     * @param str
     * @return
     */
    public static char [] steal(final String str) {
        if (CHARS_FIELD == null) {
            return str.toCharArray();
        } else {
            try {
                return (char []) CHARS_FIELD.get(str);
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Create a String based on the provided character array.
     * No copy of the array is made.
     * @param chars
     * @return
     */
    public static String wrap(final char [] chars) {
        if (PROTECTED_STR_CONSTR == null) {
            return new String(chars);
        } else {
            try {
                if (PROTECTED_STR_CONSTR_PARAM_TYPES.length == 3) {
                    return PROTECTED_STR_CONSTR.newInstance(0, chars.length, chars);
                } else {
                    return PROTECTED_STR_CONSTR.newInstance(chars, Boolean.TRUE);
                }
            } catch (InstantiationException | IllegalAccessException
                    | IllegalArgumentException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static final ThreadLocal<CharsetEncoder> UTF8_ENCODER = new ThreadLocal<CharsetEncoder>() {

        @Override
        protected CharsetEncoder initialValue() {
            return Charsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE);
        }

    };

    private static final ThreadLocal<CharsetDecoder> UTF8_DECODER = new ThreadLocal<CharsetDecoder>() {

        @Override
        protected CharsetDecoder initialValue() {
            return Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE);
        }

    };


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
            final byte [] targetArray) {
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


    public static String fromUtf8(final byte [] bytes) {
        return decode(UTF8_DECODER.get(), bytes, 0, bytes.length);
    }

    public static String fromUtf8(final byte [] bytes, final int startIdx, final int length) {
        return decode(UTF8_DECODER.get(), bytes, startIdx, length);
    }


    public static byte [] toUtf8(final String str) {
        final char[] chars = steal(str);
        return encode(UTF8_ENCODER.get(), chars, 0, chars.length);
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



    public static CharSequence subSequence(final CharSequence seq, final int startIdx, final int endIdx) {
        if (startIdx == 0  && endIdx == seq.length()) {
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

        public SubSequence(final CharSequence underlyingSequence, final int length, final int startIdx) {
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
           return Strings.subSequence(underlyingSequence, startIdx + start, startIdx + end);
        }

        @Override
        public String toString() {
            char [] chars = new char[length];
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
                if (qc.charAt(i) != qc.charAt(j)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }


}
