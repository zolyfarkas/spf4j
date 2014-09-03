
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

import java.io.IOException;
import java.io.Writer;
import static java.lang.Math.min;
import javax.annotation.Nonnull;

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
    
}
