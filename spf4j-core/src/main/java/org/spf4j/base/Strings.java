
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
    
    static {
        CHARS_FIELD = AccessController.doPrivileged(new PrivilegedAction<Field>() {
                @Override
                public Field run() {
                    Field charsField;
                    try {
                        charsField = String.class.getDeclaredField("value");
                    } catch (NoSuchFieldException ex) {
                        LOG.info("char array stealing from String not supported", ex);
                        charsField = null;
                    } catch (SecurityException ex) {
                        throw new RuntimeException(ex);
                    }
                    if (charsField != null) {
                        charsField.setAccessible(true);
                    }
                    return charsField;
                }
            });
        
        PROTECTED_STR_CONSTR = AccessController.doPrivileged(new PrivilegedAction<Constructor<String>>() {
                @Override
                public Constructor<String> run() {
                    Constructor<String> constr;
                    try {
                        constr = String.class.getDeclaredConstructor(char[].class, boolean.class);
                    } catch (NoSuchMethodException ex) {
                        LOG.info("building String from char[] fast not supported", ex);
                        constr = null;
                    } catch (SecurityException ex) {
                        throw new RuntimeException(ex);
                    }
                    if (constr != null) {
                        constr.setAccessible(true);
                    }
                    return constr;
                }
            });
        
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
                return PROTECTED_STR_CONSTR.newInstance(chars, Boolean.TRUE);
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
        int en = (int) (len * (double) ce.maxBytesPerChar());
        byte[] ba = Arrays.getBytesTmp(en);
        if (len == 0) {
            return ba;
        }
        if (ce instanceof ArrayEncoder) {
            int blen = ((ArrayEncoder) ce).encode(ca, off, len, ba);
            return java.util.Arrays.copyOf(ba, blen);
        } else {
            ce.reset();
            ByteBuffer bb = ByteBuffer.wrap(ba);
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
            return java.util.Arrays.copyOf(ba, bb.position());
        }
    }


    @SuppressFBWarnings("SUA_SUSPICIOUS_UNINITIALIZED_ARRAY")
    public static String decode(final CharsetDecoder cd, final byte[] ba, final int off, final int len) {
        int en = (int) (len * (double) cd.maxCharsPerByte());
        char[] ca = Arrays.getCharsTmp(en);
        if (len == 0) {
            return "";
        }
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
    
    
    public static byte [] toUtf8(final String str) {
        final char[] chars = steal(str);
        return encode(UTF8_ENCODER.get(), chars, 0, chars.length);
    }
    
    
    
}
